# 项目结构文档

├── 📂 MNIST/
│   ├── 📂 data/
│   │   ├── 📄 mnist  
│   │   │      // *(添加描述)*
│   │   └── 📄 mnist.t.bz2  
│   │          // *(添加描述)*
│   ├── 📂 models/
│   │   ├── 📄 cnn_mnist_pytorch.onnx  
│   │   │      // *(添加描述)*
│   │   ├── 📄 generic_sparse_to_dense_matmul.onnx  
│   │   │      // *(添加描述)*
│   │   ├── 📄 lr_mnist_scikit.onnx  
│   │   │      // *(添加描述)*
│   │   ├── 📄 partial-inputs-test-2.onnx  
│   │   │      // *(添加描述)*
│   │   └── 📄 partial-inputs-test.onnx  
│   │          // *(添加描述)*
│   └── 📂 raw/
│       ├── 📄 t10k-images-idx3-ubyte  
│       │      // *(添加描述)*
│       ├── 📄 t10k-images-idx3-ubyte.gz  
│       │      // *(添加描述)*
│       ├── 📄 t10k-labels-idx1-ubyte  
│       │      // *(添加描述)*
│       ├── 📄 t10k-labels-idx1-ubyte.gz  
│       │      // *(添加描述)*
│       ├── 📄 train-images-idx3-ubyte  
│       │      // *(添加描述)*
│       ├── 📄 train-images-idx3-ubyte.gz  
│       │      // *(添加描述)*
│       ├── 📄 train-labels-idx1-ubyte  
│       │      // *(添加描述)*
│       └── 📄 train-labels-idx1-ubyte.gz  
│              // *(添加描述)*
├── 📂 hw/
│   ├── 📂 gen/
│   │   └── 📄 .gitignore  
│   │          // *(添加描述)*
│   ├── 📂 spinal/
│   │   ├── 📂 Accelerator/
│   │   │   ├── 🆂 Accelerator.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 AcceleratorSimInterface.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── ☕ InstJavaTODO.java  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 InstSim.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 performanceTest.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 tb_Accelerator.scala  
│   │   │   │      // *(添加描述)*
│   │   │   └── ☕ test.java  
│   │   │          // *(添加描述)*
│   │   ├── 📂 Activation/
│   │   │   ├── 🆂 Activation.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 tb_2.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 tb_3.scala  
│   │   │   │      // *(添加描述)*
│   │   │   └── 🆂 tb_Activation.scala  
│   │   │          // *(添加描述)*
│   │   ├── 📂 Tiling/
│   │   │   ├── 🆂 Collector.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 Conv_sim.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 Sdpram.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 Slicer.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 tb_Collector.scala  
│   │   │   │      // *(添加描述)*
│   │   │   └── 🆂 tb_Slicer.scala  
│   │   │          // *(添加描述)*
│   │   ├── 📂 Cordic/
│   │   │   ├── 🆂 cordic.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 cordicDoubleRate.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 cordicUtil.ipynb  
│   │   │   │      // *(添加描述)*
│   │   │   └── 🆂 tb_cordic.scala  
│   │   │          // *(添加描述)*
│   │   ├── 📂 DataPump/
│   │   │   ├── 🆂 DataPump_mm2mm.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 DataPump_mm2s.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 DataPump_s2mm.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 MemoryPort.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 tb_DataPump_mm2mm.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 tb_DataPump_mm2s.scala  
│   │   │   │      // *(添加描述)*
│   │   │   └── 🆂 tb_DataPump_s2mm.scala  
│   │   │          // *(添加描述)*
│   │   ├── 📂 ExponentialFunction/
│   │   │   ├── 🆂 EXP_function.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📝 ReadMe.md  
│   │   │   │      // Exponential Function
│   │   │   ├── 📄 VREXP_Bipartite_LUT.m  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 dataWidthAnalysis.mlx  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 doubleExpLUT.txt  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 genLUTtest.mlx  
│   │   │   │      // *(添加描述)*
│   │   │   └── 🆂 tb_EXP_function.scala  
│   │   │          // *(添加描述)*
│   │   ├── 📂 FloatingPoint/
│   │   │   ├── 📄 .$floating.drawio.png.bkp  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📝 Readme.md  
│   │   │   │      // FloatingPoint
│   │   │   ├── 📄 floating.drawio.png  
│   │   │   │      // *(添加描述)*
│   │   │   └── 🆂 fp_adder_multiplier_one_period.scala  
│   │   │          // *(添加描述)*
│   │   ├── 📂 MatrixComputeUnit/
│   │   │   ├── 📂 AdderTree/
│   │   │   │   └── 🆂 adderTreeOri.scala  
│   │   │   │          // *(添加描述)*
│   │   │   ├── 📂 ElementWise/
│   │   │   │   ├── 🆂 ElementWise.scala  
│   │   │   │   │      // *(添加描述)*
│   │   │   │   └── 🆂 tb_ElementWise.scala  
│   │   │   │          // *(添加描述)*
│   │   │   └── 📂 SystolicArray2D/
│   │   │       ├── 🆂 ElementTranspose.scala  
│   │   │       │      // *(添加描述)*
│   │   │       ├── 📝 ReadMe.md  
│   │   │       │      // SystolicArray2D
│   │   │       ├── 🆂 SIntShifter.scala  
│   │   │       │      // *(添加描述)*
│   │   │       ├── 📄 SystolicArray2D.drawio.png  
│   │   │       │      // *(添加描述)*
│   │   │       ├── 🆂 SystolicArray2D.scala  
│   │   │       │      // *(添加描述)*
│   │   │       ├── 📄 SystolicArray2DUnit.drawio.png  
│   │   │       │      // *(添加描述)*
│   │   │       ├── 🆂 SystolicArray2DUnit.scala  
│   │   │       │      // *(添加描述)*
│   │   │       ├── 📄 SystolicArray2DUnitSpecial.drawio.png  
│   │   │       │      // *(添加描述)*
│   │   │       ├── 🆂 SystolicArray2DUnitSpecial.scala  
│   │   │       │      // *(添加描述)*
│   │   │       ├── 📄 SystolicArray2D_CC.drawio.png  
│   │   │       │      // *(添加描述)*
│   │   │       ├── 🆂 SystolicArray2D_CC.scala  
│   │   │       │      // *(添加描述)*
│   │   │       ├── 🆂 SystolicArray2d_Wrapper.scala  
│   │   │       │      // *(添加描述)*
│   │   │       ├── 🆂 sim_Multifunction_SystolicArray2D.scala  
│   │   │       │      // *(添加描述)*
│   │   │       ├── 🆂 tb_SystolicArray2D.scala  
│   │   │       │      // *(添加描述)*
│   │   │       ├── 🆂 tb_SystolicArray2D_CC.scala  
│   │   │       │      // *(添加描述)*
│   │   │       └── 🆂 tb_SystolicArray2D_Wrapper.scala  
│   │   │              // *(添加描述)*
│   │   ├── 📂 Interface/
│   │   │   ├── 🆂 Interface.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 StreamDispatcher.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 Util.scala  
│   │   │   │      // *(添加描述)*
│   │   │   └── 🆂 tb_StreamDispatcher.scala  
│   │   │          // *(添加描述)*
│   │   ├── 📂 LogCumsumExp/
│   │   │   └── 🐍 logcumsumexp.py  
│   │   │          // *(添加描述)*
│   │   ├── 📂 LogarithmFunction/
│   │   │   ├── 🆂 LN_function.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🐍 cordic_ln_hw.py  
│   │   │   │      // *(添加描述)*
│   │   │   └── 🆂 tb_LN_function.scala  
│   │   │          // *(添加描述)*
│   │   ├── 📂 MemBlackBoxer/
│   │   │   ├── 📂 MemManager/
│   │   │   │   ├── 🆂 MemConfig.scala  
│   │   │   │   │      // *(添加描述)*
│   │   │   │   ├── 🆂 MemPorts.scala  
│   │   │   │   │      // *(添加描述)*
│   │   │   │   ├── 🆂 MemVendor.scala  
│   │   │   │   │      // *(添加描述)*
│   │   │   │   └── 🆂 MemWrapper.scala  
│   │   │   │          // *(添加描述)*
│   │   │   ├── 📂 PhaseMemBlackBoxer/
│   │   │   │   ├── 🆂 PhaseSramConverter.scala  
│   │   │   │   │      // *(添加描述)*
│   │   │   │   └── 🆂 Utils.scala  
│   │   │   │          // *(添加描述)*
│   │   │   └── 📂 Vendor/
│   │   │       ├── 🆂 Huali.scala  
│   │   │       │      // *(添加描述)*
│   │   │       └── 🆂 UMC40.scala  
│   │   │              // *(添加描述)*
│   │   ├── 📂 ReLUFunction/
│   │   │   ├── 🆂 ReLUFunctionTest.scala  
│   │   │   │      // *(添加描述)*
│   │   │   └── 🆂 ReLU_function.scala  
│   │   │          // *(添加描述)*
│   │   ├── 📂 Sigmoid/
│   │   │   ├── 🆂 SigmoidFunction.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🐍 sigmoid_hp_hw.py  
│   │   │   │      // !/usr/bin/env python3
│   │   │   ├── 🐍 sigmoid_hw.py  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 sim result.png  
│   │   │   │      // *(添加描述)*
│   │   │   └── 🆂 tb_sigmoidFunction.scala  
│   │   │          // *(添加描述)*
│   │   ├── 📂 SoftplusFunction/
│   │   │   ├── 🆂 SoftplusFunction.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🐍 Softplus_hw.py  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 softplus_Bipartite_LUT.m  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 softplus_LUT.txt  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 softplus_ReLU_diff_Bipartite_LUT.m  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 softplus_ReLU_diff_LUT.txt  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🐍 softplus_comp_hw.py  
│   │   │   │      // 环境重置，需重新导入库
│   │   │   └── 🆂 tb_Softplus_function.scala  
│   │   │          // *(添加描述)*
│   │   ├── 📂 XPM_BlackBox/
│   │   │   ├── 📄 .gitignore  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📝 ReadMe.md  
│   │   │   │      // XPM BlackBox
│   │   │   ├── 🆂 tb_xpm_memory_sdpram.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 tb_xpm_memory_spram.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 tb_xpm_memory_tdpram.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 xpm_memor_sdpram.v  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 xpm_memory_dprom.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 xpm_memory_dprom.v  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 xpm_memory_sdpram.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 xpm_memory_spram.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 xpm_memory_spram.v  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 xpm_memory_tdpram.scala  
│   │   │   │      // *(添加描述)*
│   │   │   └── 📄 xpm_memory_tdpram.v  
│   │   │          // *(添加描述)*
│   │   ├── 📂 playGround/
│   │   │   ├── 🆂 Config.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 DuelClockDomain_sim_Demo.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 MemDemo.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 MyTopLevel.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 MyTopLevelFormal.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 MyTopLevelSim.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 PATH_Demo.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 PipelineAPI_Demo.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 StreamDemo copy.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 StreamWidthAdapter_Test.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 breeze_quickStart.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 clockDomain_Demo.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 new case class demo.scala  
│   │   │   │      // *(添加描述)*
│   │   │   └── 🆂 new class demo.scala  
│   │   │          // *(添加描述)*
│   │   ├── 📂 projectname/
│   │   │   ├── 🆂 Config.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 EXP_function.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 ExpSim.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 LNSim.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 LogarithmFunction.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 MyTopLevel.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 MyTopLevelFormal.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 MyTopLevelSim.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 SoftplusSim.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 Softplus_function.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 StreamDemoTop.scala  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 🆂 StreamDemoTopSim.scala  
│   │   │   │      // *(添加描述)*
│   │   │   └── 🆂 onnx_demo.scala  
│   │   │          // *(添加描述)*
│   │   └── 📂 simulation_tool/
│   │       ├── 🆂 ScoreboardInOrder_Bigint.scala  
│   │       │      // *(添加描述)*
│   │       └── 🆂 ScoreboardInOrder_matrix.scala  
│   │              // *(添加描述)*
│   ├── 📂 verilog/
│   │   └── 📄 .gitignore  
│   │          // *(添加描述)*
│   └── 📂 vhdl/
│       └── 📄 .gitignore  
│              // *(添加描述)*
├── 📂 java_dl4j_each_layer_outputs/
├── 📂 java_hw_each_layer_outputs/
├── 📂 java_outputs/
├── 📂 onnx_debug_py/
│   ├── 🐍 compare_hw_dl4j.py  
│   │      // *(添加描述)*
│   ├── 🐍 compare_tensors.py  
│   │      // *(添加描述)*
│   ├── 📄 execution_order.txt  
│   │      // *(添加描述)*
│   ├── 🐍 export_golden_tensors.py  
│   │      // *(添加描述)*
│   ├── 🐍 view_bin.py  
│   │      // -*- coding: utf-8 -*-
│   └── 🐍 view_py.py  
│          // -*- coding: utf-8 -*-
├── 📂 performance_results_analyze/
│   ├── 📄 SA_InFifoDepth_lines.png  
│   │      // *(添加描述)*
│   ├── 📄 SA_InstFifoDepth_lines.png  
│   │      // *(添加描述)*
│   ├── 📄 SA_OutFifoDepth_lines.png  
│   │      // *(添加描述)*
│   ├── 📄 SA_SideNum_lines.png  
│   │      // *(添加描述)*
│   ├── 📄 activationOutFifoDepth_lines.png  
│   │      // *(添加描述)*
│   ├── 📄 numCores_lines.png  
│   │      // *(添加描述)*
│   └── 📄 slicedInstFifoDepth_lines.png  
│          // *(添加描述)*
├── 📂 project/
│   ├── 📄 build.properties  
│   │      // *(添加描述)*
│   └── 📄 plugins.sbt  
│          // *(添加描述)*
├── 📂 rtl/
│   ├── 📂 Accelerator/
│   │   └── 📂 verilog/
│   │       ├── 📄 Accelerator.lst  
│   │       │      // *(添加描述)*
│   │       ├── 📄 Accelerator.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 Accelerator_removePruned.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 Activation.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 BufferCC.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 BufferCC_1.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 BufferCC_2.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 BufferCC_3.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 BufferCC_4.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 BufferCC_5.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 BufferCC_6.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 BufferCC_7.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 BufferCC_8.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 Collector.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 DataPump_mm2s.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 DataPump_s2mm.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 EXP_function.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 LN_function.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 Normalizer.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 ReLU_function.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SIntShifter.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SIntShifter_64.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 Sdpram.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 Slicer.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 Softplus_function.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamArbiter.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamDemux.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamDispatcher.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFifo.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFifoCC.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFifoCC_1.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFifoCC_2.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_1.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_10.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_11.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_12.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_13.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_14.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_15.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_16.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_17.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_18.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_19.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_2.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_20.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_21.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_22.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_23.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_24.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_25.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_26.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_27.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_28.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_29.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_3.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_30.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_31.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_32.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_33.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_34.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_35.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_36.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_37.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_38.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_39.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_4.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_40.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_41.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_42.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_43.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_44.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_45.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_46.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_47.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_48.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_49.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_5.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_50.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_51.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_52.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_53.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_54.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_55.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_56.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_57.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_58.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_59.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_6.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_60.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_61.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_62.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_7.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_8.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 StreamFork_9.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SystolicArray2D.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SystolicArray2DUnit.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SystolicArray2DUnitSpecial.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SystolicArray2DUnitSpecial_1.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SystolicArray2DUnitSpecial_31.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SystolicArray2DUnitSpecial_7.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SystolicArray2DUnit_13.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SystolicArray2DUnit_31.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SystolicArray2DUnit_49.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SystolicArray2DUnit_55.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SystolicArray2DUnit_61.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SystolicArray2DUnit_7.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SystolicArray2DUnit_961.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SystolicArray2DUnit_991.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SystolicArray2D_CC.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SystolicArray2D_Wrapper.v  
│   │       │      // *(添加描述)*
│   │       └── 📄 enumdefine.v  
│   │              // *(添加描述)*
│   ├── 📂 Activation/
│   │   └── 📂 sim_Activation_test_report/
│   │       ├── 📄 Activation.lst  
│   │       │      // *(添加描述)*
│   │       ├── 📄 Activation.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 EXP_function.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 LN_function.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 Normalizer.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 ReLU_function.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SIntShifter.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 Softplus_function.v  
│   │       │      // *(添加描述)*
│   │       └── 📄 enumdefine.v  
│   │              // *(添加描述)*
│   ├── 📂 LogFunction/
│   │   └── 📂 sim_LN_function_test_report/
│   │       ├── 📄 LN_function.lst  
│   │       │      // *(添加描述)*
│   │       ├── 📄 LN_function.v  
│   │       │      // *(添加描述)*
│   │       └── 📄 Normalizer.v  
│   │              // *(添加描述)*
│   ├── 📂 SystolicArray2DUnit/
│   │   └── 📂 verilog/
│   │       ├── 📄 SIntShifter.v  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SystolicArray2DUnit.lst  
│   │       │      // *(添加描述)*
│   │       ├── 📄 SystolicArray2DUnit.v  
│   │       │      // *(添加描述)*
│   │       └── 📄 enumdefine.v  
│   │              // *(添加描述)*
│   ├── 📂 SystolicArray2D_CC/
│   │   └── 📂 verilog/
│   │       └── 📄 SystolicArray2D_CC_depress_for_Sim.v  
│   │              // *(添加描述)*
│   └── 📂 SystolicArray2D_Wrapper/
│       └── 📂 verilog/
│           ├── 📄 BufferCC.v  
│           │      // *(添加描述)*
│           ├── 📄 BufferCC_1.v  
│           │      // *(添加描述)*
│           ├── 📄 BufferCC_2.v  
│           │      // *(添加描述)*
│           ├── 📄 BufferCC_3.v  
│           │      // *(添加描述)*
│           ├── 📄 BufferCC_4.v  
│           │      // *(添加描述)*
│           ├── 📄 BufferCC_5.v  
│           │      // *(添加描述)*
│           ├── 📄 BufferCC_6.v  
│           │      // *(添加描述)*
│           ├── 📄 BufferCC_7.v  
│           │      // *(添加描述)*
│           ├── 📄 BufferCC_8.v  
│           │      // *(添加描述)*
│           ├── 📄 SIntShifter.v  
│           │      // *(添加描述)*
│           ├── 📄 StreamFifoCC.v  
│           │      // *(添加描述)*
│           ├── 📄 StreamFifoCC_1.v  
│           │      // *(添加描述)*
│           ├── 📄 StreamFifoCC_2.v  
│           │      // *(添加描述)*
│           ├── 📄 StreamFork.v  
│           │      // *(添加描述)*
│           ├── 📄 StreamFork_1.v  
│           │      // *(添加描述)*
│           ├── 📄 StreamFork_2.v  
│           │      // *(添加描述)*
│           ├── 📄 StreamFork_3.v  
│           │      // *(添加描述)*
│           ├── 📄 StreamFork_4.v  
│           │      // *(添加描述)*
│           ├── 📄 StreamFork_5.v  
│           │      // *(添加描述)*
│           ├── 📄 StreamFork_6.v  
│           │      // *(添加描述)*
│           ├── 📄 SystolicArray2D.v  
│           │      // *(添加描述)*
│           ├── 📄 SystolicArray2DUnit.v  
│           │      // *(添加描述)*
│           ├── 📄 SystolicArray2DUnitSpecial.v  
│           │      // *(添加描述)*
│           ├── 📄 SystolicArray2DUnitSpecial_1.v  
│           │      // *(添加描述)*
│           ├── 📄 SystolicArray2DUnitSpecial_3.v  
│           │      // *(添加描述)*
│           ├── 📄 SystolicArray2DUnit_11.v  
│           │      // *(添加描述)*
│           ├── 📄 SystolicArray2DUnit_3.v  
│           │      // *(添加描述)*
│           ├── 📄 SystolicArray2DUnit_5.v  
│           │      // *(添加描述)*
│           ├── 📄 SystolicArray2DUnit_9.v  
│           │      // *(添加描述)*
│           ├── 📄 SystolicArray2D_CC.v  
│           │      // *(添加描述)*
│           ├── 📄 SystolicArray2D_Wrapper.lst  
│           │      // *(添加描述)*
│           ├── 📄 SystolicArray2D_Wrapper.v  
│           │      // *(添加描述)*
│           └── 📄 enumdefine.v  
│                  // *(添加描述)*
├── 📂 sw/
│   └── 📂 java/
│       ├── 📂 main/
│       │   ├── 📂 java/
│       │   │   └── 📂 org/
│       │   │       ├── 📂 forwarder/
│       │   │       ├── 📂 onnx4j/
│       │   │       └── ☕ OnnxRuntime_run_MNIST.java  
│       │   │              // *(添加描述)*
│       │   └── 📂 resources/
│       │       ├── 📂 META-INF/
│       │       │   └── 📂 services/
│       │       ├── 📂 models/
│       │       │   └── 📂 mnist/
│       │       ├── 📂 static/
│       │       │   └── 📂 resources/
│       │       ├── 📄 application-dev.properties  
│       │       │      // Embedding Tomcat Port
│       │       ├── 📄 application-production.properties  
│       │       │      // *(添加描述)*
│       │       ├── 📄 application-staging.properties  
│       │       │      // *(添加描述)*
│       │       ├── 📄 application-test.properties  
│       │       │      // *(添加描述)*
│       │       ├── 📄 application-uat.properties  
│       │       │      // *(添加描述)*
│       │       ├── 📄 application.properties  
│       │       │      // Default profile setting
│       │       └── 📡 logback.xml  
│       │              // *(添加描述)*
│       └── 📂 test/
│           ├── 📂 java/
│           │   ├── 📂 org/
│           │   │   ├── 📂 forwarder/
│           │   │   └── 📂 onnx4j/
│           │   ├── ☕ ExpSimJavaTest.java  
│           │   │      // *(添加描述)*
│           │   ├── ☕ LNSimJavaTest.java  
│           │   │      // *(添加描述)*
│           │   ├── ☕ MyTopLevelSimJavaTest.java  
│           │   │      // *(添加描述)*
│           │   ├── ☕ SoftplusSimJavaTest.java  
│           │   │      // *(添加描述)*
│           │   └── ☕ onnx4j_run.java  
│           │          // *(添加描述)*
│           └── 📂 resources/
│               ├── 📂 mnist/
│               ├── 📂 simple/
│               │   └── 📄 model.onnx  
│               │          // *(添加描述)*
│               ├── 📂 squeezenet/
│               │   └── 📂 opset_v7/
│               ├── 📂 tiny_yolov2/
│               │   ├── 📂 opset_v1/
│               │   ├── 📂 opset_v7/
│               │   └── 📂 opset_v8/
│               ├── 📡 logback-test.xml  
│               │      // *(添加描述)*
│               ├── 📄 onnx4j.properties  
│               │      // *(添加描述)*
│               └── 📄 simple_tf.onnx  
│                      // *(添加描述)*
├── 📂 vfastLog/
│   ├── 📄 pes.bat  
│   │      // *(添加描述)*
├── 📄 .gitignore  
│      // *(添加描述)*
├── 📄 .mill-version  
│      // *(添加描述)*
├── ⚙️ .scalafmt.conf  
│      // *(添加描述)*
├── 📄 .vlogansetup.args  
│      // *(添加描述)*
├── 📝 DeepResearch.md  
│      // ONNX到RTL硬件自动生成研究综述
├── 📝 PROJECT_STRUCTURE.md  
│      // 项目结构文档
├── 📝 PROJECT_STRUCTURE_DETAILED.md  
│      // Onnx_SpinalHDL_interface 项目详细结构
├── 📝 README.md  
│      // Onnx_SpinalHDL_interface
├── 📄 Relu_comparison.png  
│      // *(添加描述)*
├── 📄 Slicer.svg  
│      // *(添加描述)*
├── 📄 absnet.onnx  
│      // *(添加描述)*
├── 🐍 autotreedoc.py  
│      // *(添加描述)*
├── 📄 build.sbt  
│      // *(添加描述)*
├── 📄 build.sc  
│      // *(添加描述)*
├── 📄 performance_results.csv  
│      // *(添加描述)*
├── 🐍 performance_results_analyze.py  
│      // *(添加描述)*
├── 📄 relu_absolute_error.png  
│      // *(添加描述)*
├── 📄 relu_comparison.png  
│      // *(添加描述)*
├── 📄 relu_error_distribution.png  
│      // *(添加描述)*
├── 📄 relu_relative_error.png  
│      // *(添加描述)*
├── 📄 tb_EXP_function_absolute_error.png  
│      // *(添加描述)*
├── 📄 tb_EXP_function_comparison.png  
│      // *(添加描述)*
├── 📄 tb_EXP_function_relative_error.png  
│      // *(添加描述)*
├── 📄 tb_LN_function_absolute_error.png  
│      // *(添加描述)*
├── 📄 tb_LN_function_comparison.png  
│      // *(添加描述)*
├── 📄 tb_LN_function_relative_error.png  
│      // *(添加描述)*
├── 📄 tb_Softplus_absolute_error.png  
│      // *(添加描述)*
├── 📄 tb_Softplus_comparison.png  
│      // *(添加描述)*
├── 📄 tb_Softplus_relative_error_log.png  
│      // *(添加描述)*
└── 📄 worksheet.sc  
       // *(添加描述)*

> 文档自动生成于 .