package org.onnx4j.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.onnx4j.Model;
import org.onnx4j.NamedOnnxObject;
import org.onnx4j.model.graph.Constant;
import org.onnx4j.model.graph.Node;
import org.onnx4j.model.graph.exchanges.GraphInput;
import org.onnx4j.model.graph.exchanges.GraphOutput;
import org.onnx4j.prototypes.OnnxProto3.GraphProto;
import org.onnx4j.prototypes.OnnxProto3.NodeProto;
import org.onnx4j.prototypes.OnnxProto3.TensorProto;
import org.onnx4j.prototypes.OnnxProto3.ValueInfoProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onnx4j.prototypes.OnnxOperatorsProto3.OperatorProto;
import org.onnx4j.prototypes.OnnxOperatorsProto3.OperatorSetProto;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph.Builder;

public class Graph extends NamedOnnxObject {

	private static Logger logger = LoggerFactory.getLogger(Graph.class);

	private Model model;
	private com.google.common.graph.Graph<Node> dag;
	private Constant[] constants;
	private GraphInput[] inputs;
	private GraphOutput[] outputs;

	private Map<String, OperatorProto> operatorProtos = new HashMap<>();
	private Map<String, OperatorSetProto> operatorSets = new HashMap<>();

	public Graph(Model model, GraphProto graphProto) {
		super(graphProto.getName(), graphProto.getDocString());
		this.model = model;

		this.registerOperatorSet(org.onnx4j.prototypes.Newopsets.getNewOpset());

		Map<String, Node> nodeMapByOutName = new HashMap<>();
		Map<String, Collection<Node>> nodesMapByInName = new HashMap<>();

		// 核心改进：使用全局唯一的 Key 确保 Node 对象在内存中唯一
		Map<String, Node> nodeCache = new HashMap<>();

		for (NodeProto nodeProto : graphProto.getNodeList()) {
			// 创建临时节点
			Node node = new Node(this.model, nodeProto, this.model.getTensorOptions());

			// 生成唯一识别 Key
			// 规则：有 Name 用 Name，没 Name 用其所有输出变量名拼接（确保多输出也唯一）
			String nodeUniqueKey = (nodeProto.getName() != null && !nodeProto.getName().isEmpty())
					? nodeProto.getName()
					: String.join(",", nodeProto.getOutputList());

			// 确保全图范围内，同一个逻辑节点只对应一个 Node 实例对象
			Node finalNode = nodeCache.computeIfAbsent(nodeUniqueKey, k -> node);

			for (String outputName : nodeProto.getOutputList()) {
				nodeMapByOutName.put(outputName, finalNode);
			}

			for (String inputName : nodeProto.getInputList()) {
				Collection<Node> nodes = nodesMapByInName.computeIfAbsent(inputName, k -> new ArrayList<>());
				nodes.add(finalNode);
			}
		}

		this.constants = this.initConstants(graphProto);
		this.inputs = this.initInputs(graphProto);
		this.dag = this.buildDAG(graphProto, nodeMapByOutName, nodesMapByInName);
		this.outputs = this.initOutputs(graphProto, nodeMapByOutName);
	}

	// =========================================================
	// 修复 NoSuchMethodError：HWAcceleratedBackend 调用的必须接口
	// =========================================================
	public Constant[] getConstants() {
		return this.constants;
	}

	public Model getModel() {
		return this.model;
	}

	// =========================================================
	// 修复 NoSuchMethodError：Session.feed() 调用的必须接口
	// =========================================================
	public GraphInput[] getInputs() {
		return this.inputs;
	}

	/**
	 * 根据名称获取输入 (GemmReluHWTest 报错就在这里)
	 */
	public GraphInput getInputs(String inputName) {
		if (this.inputs != null) {
			for (GraphInput graphInput : this.inputs) {
				if (graphInput.getName().equalsIgnoreCase(inputName))
					return graphInput;
			}
		}
		return null;
	}

	public GraphOutput[] getOutputs() {
		return this.outputs;
	}

	/**
	 * 根据名称获取输出
	 */
	public GraphOutput getOutput(String outputName) {
		if (this.outputs != null) {
			for (GraphOutput graphOutput : this.outputs) {
				if (graphOutput.getName().equalsIgnoreCase(outputName))
					return graphOutput;
			}
		}
		return null;
	}

	// =========================================================
	// 图引擎/执行器相关接口
	// =========================================================
	public Set<Node> getNodes() {
		return this.dag.nodes();
	}

	public Set<Node> predecessors(Node node) {
		return this.dag.predecessors(node);
	}

	public Set<Node> successors(Node node) {
		return this.dag.successors(node);
	}

	public void registerOperatorSet(OperatorSetProto customOpset) {
		if (customOpset == null) return;
		for (OperatorProto op : customOpset.getOperatorList()) {
			operatorProtos.put(op.getOpType(), op);
		}
	}

	public OperatorProto getCustomOp(String type) {
		return operatorProtos.get(type);
	}

	private Constant[] initConstants(GraphProto graph) {
		List<TensorProto> initializerList = graph.getInitializerList();
		Constant[] res = new Constant[initializerList.size()];
		for (int n = 0; n < initializerList.size(); n++) {
			res[n] = new Constant(this.model, initializerList.get(n));
		}
		return res;
	}

	private GraphInput[] initInputs(GraphProto graph) {
		List<ValueInfoProto> inputList = graph.getInputList();
		GraphInput[] res = new GraphInput[inputList.size()];
		for (int n = 0; n < inputList.size(); n++) {
			res[n] = new GraphInput(inputList.get(n));
		}
		return res;
	}

	private GraphOutput[] initOutputs(GraphProto graph, Map<String, Node> nodeMapByOutName) {
		List<ValueInfoProto> outputList = graph.getOutputList();
		GraphOutput[] res = new GraphOutput[outputList.size()];
		for (int n = 0; n < outputList.size(); n++) {
			ValueInfoProto vip = outputList.get(n);
			Node node = nodeMapByOutName.get(vip.getName());
			res[n] = new GraphOutput(node, vip);
		}
		return res;
	}

	private com.google.common.graph.Graph<Node> buildDAG(GraphProto graphProto, Map<String, Node> nodeMapByOutName,
														 Map<String, Collection<Node>> nodesMapByInName) {
		Builder<Node> builder = GraphBuilder.directed().allowsSelfLoops(false).<Node>immutable();

		// 强制把 nodeCache 里的所有物理对象先塞进图里，确保它们“在图中”
		// 这里的 nodeCache 就是你在构造函数里建立的那个 Map
		// 如果 buildDAG 拿不到，可以把它传进来

		for (Entry<String, Node> entrySet : nodeMapByOutName.entrySet()) {
			String outputName = entrySet.getKey();
			Node outputNode = entrySet.getValue();

			// 确保输出节点被加入图
			builder.addNode(outputNode);

			Collection<Node> inNodes = nodesMapByInName.get(outputName);
			if (inNodes != null) {
				for (Node inNode : inNodes) {
					builder.addNode(inNode); // 确保输入节点被加入图
					builder.putEdge(outputNode, inNode);
				}
			}
		}
		return builder.build();
	}
}