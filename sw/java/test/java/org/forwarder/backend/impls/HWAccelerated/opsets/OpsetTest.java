package org.forwarder.backend.impls.HWAccelerated.opsets;

import org.forwarder.backend.impls.HWAccelerated.opsets.OpsetTest.MarkdownTable.BodyRow;
import org.forwarder.backend.impls.HWAccelerated.opsets.OpsetTest.MarkdownTable.HeaderItem;
import org.forwarder.backend.impls.HWAccelerated.opsets.OpsetTest.MarkdownTable.HeaderItem.ALigned;
import org.forwarder.backend.impls.HWAccelerated.opsets.OpsetTest.MarkdownTable.RowItem;
import org.junit.Test;
import org.onnx4j.opsets.domain.aiOnnx.AiOnnxOperator;
import org.reflections.Reflections;

import java.util.*;
import java.util.Map.Entry;

public class OpsetTest {

	@Test
	public void buildOpsetTbl() throws Throwable {
		// 创建一个映射，键为操作类型（String），值为该操作支持的版本列表（List<Integer>）
		Map<String, List<Integer>> supportedOps = new LinkedHashMap<String, List<Integer>>();
		// 使用 Reflections 库查找所有扩展自 AiOnnxOperator 的类
		Reflections reflections = new Reflections(
				"org.forwarder.backend.impls.HWAccelerated");
		Set<Class<? extends AiOnnxOperator>> opClasses = reflections
				.getSubTypesOf(AiOnnxOperator.class);

		long maxSupportedVer = 0L;// 初始化一个变量来存储最大支持的版本号

		// 遍历所有找到的操作类，查找每个操作的支持版本，获取最大支持的版本号
		for (Class<? extends AiOnnxOperator> opClz : opClasses) {
			if (opClz.isInterface())
				continue;// 如果是interface，说明并非一个Operater实现，而只是一个虚类，跳过

			
			AiOnnxOperator operator = createAiOnnxOperator(opClz);//说明不是接口，通过键创建一个操作实例
			long ver = operator.getVersion();//获取操作的支持版本号
			//maxSupportedVer遍历取到最高的支持版本号
			if (ver > maxSupportedVer)
				maxSupportedVer = ver;
		}
		// 再次遍历所有找到的操作类，构建操作的支持版本表
		for (Class<? extends AiOnnxOperator> opClz : opClasses) {
			if (opClz.isInterface())
				continue;// 如果是interface，说明并非一个Operater实现，而只是一个虚类，跳过
			AiOnnxOperator operator = createAiOnnxOperator(opClz);//说明不是接口，通过键创建一个操作实例

			String opType = operator.getOpType();//获取操作类型
			long ver = operator.getVersion();//获取操作的支持版本号
			List<Integer> supportedList = supportedOps.get(opType);//从supportedOps中获取对应操作类型的支持版本列表
			if (supportedList == null) {
				//supportedList为null说明是第一次访问到这种opType的operator
				supportedList = new LinkedList<Integer>();//构建一个空的列表，并赋值给supportedList
				supportedOps.put(opType, supportedList);//将当前的opType和对应的列表加入到supportedOps中
				for (int n = 0; n < maxSupportedVer; n++) {
					supportedList.add(0);
				}
			}
			supportedList.set((int) ver - 1, (int) ver);
		}
		Map<String, List<Integer>> sortedOps = new TreeMap<String, List<Integer>>(
				supportedOps);

		HeaderItem[] headers = new HeaderItem[(int) (maxSupportedVer + 1)];
		headers[0] = new HeaderItem("Operator", ALigned.LEFT);
		for (int n = 1; n < headers.length; n++) {
			headers[n] = new HeaderItem("Opset" + n, ALigned.CENTER);
		}

		MarkdownTable tbl = new MarkdownTable(headers);
		for (Entry<String, List<Integer>> entrySet : sortedOps.entrySet()) {
			BodyRow row = new BodyRow();
			row.add(new RowItem(entrySet.getKey()));

			long currSupportVer = 0l;
			for (Integer supportVer : entrySet.getValue()) {
				String data = "";
				if (supportVer > 0)
					currSupportVer = supportVer;

				if (currSupportVer > 0)
					data = String.valueOf(currSupportVer);
				else
					data = "-";
				row.add(new RowItem(data));
			}
			tbl.addRow(row);
		}
		System.out.println(tbl.toString());
	}

	private AiOnnxOperator createAiOnnxOperator(
			Class<? extends AiOnnxOperator> opClz)
			throws InstantiationException, IllegalAccessException {
		return opClz.newInstance();
	}

	static class MarkdownTable {

		static class HeaderItem {

			enum ALigned {
				LEFT, CENTER, RIGHT
			}

			private String name;
			private ALigned aligned;

			public HeaderItem(String name) {
				this(name, ALigned.LEFT);
			}

			public HeaderItem(String name, ALigned aligned) {
				super();
				this.name = name;
				this.aligned = aligned;
			}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public ALigned getAligned() {
				return aligned;
			}

			public void setAligned(ALigned aligned) {
				this.aligned = aligned;
			}
		}

		static class BodyRow {
			private List<RowItem> items = new LinkedList<RowItem>();

			public BodyRow add(RowItem item) {
				this.items.add(item);
				return this;
			}

			public List<RowItem> getItems() {
				return items;
			}
		}

		static class RowItem {
			private String data;

			public RowItem(String data) {
				super();
				this.data = data;
			}

			public String getData() {
				return data;
			}

			public void setData(String data) {
				this.data = data;
			}
		}

		private List<HeaderItem> header = new LinkedList<HeaderItem>();
		private List<BodyRow> body = new LinkedList<BodyRow>();

		public MarkdownTable(HeaderItem... headers) {
			for (HeaderItem header : headers) {
				this.header.add(header);
			}
		}

		public MarkdownTable(String... headerNames) {
			for (String headerName : headerNames) {
				this.header.add(new HeaderItem(headerName));
			}
		}

		public MarkdownTable addRow(BodyRow row) {
			this.body.add(row);
			return this;
		}

		@Override
		public String toString() {
			StringBuffer stringBuf = new StringBuffer();

			stringBuf.append("|");
			for (HeaderItem header : this.header) {
				stringBuf.append(header.getName());
				stringBuf.append("|");
			}

			stringBuf.append("\n|");
			for (HeaderItem header : this.header) {
				switch (header.getAligned()) {
				default:
				case LEFT:
					stringBuf.append(":---");
					break;
				case CENTER:
					stringBuf.append(":---:");
					break;
				case RIGHT:
					stringBuf.append("---:");
					break;
				}
				stringBuf.append("|");
			}

			for (BodyRow row : this.body) {
				stringBuf.append("\n|");
				for (RowItem item : row.getItems()) {
					stringBuf.append(item.getData());
					stringBuf.append("|");
				}
			}

			return stringBuf.toString();
		}
	}

}
