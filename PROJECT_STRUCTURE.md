# 项目结构文档

├── 📂 AN.DB/
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
│   │   ├── 📄 .gitignore  
│   │   │      // *(添加描述)*
│   │   └── 📄 MyTopLevel.v  
│   │          // *(添加描述)*
│   ├── 📂 spinal/
│   │   └── 📂 projectname/
│   │       ├── 🆂 Config.scala  
│   │       │      // *(添加描述)*
│   │       ├── 🆂 MyTopLevel.scala  
│   │       │      // *(添加描述)*
│   │       ├── 🆂 MyTopLevelFormal.scala  
│   │       │      // *(添加描述)*
│   │       ├── 🆂 MyTopLevelSim.scala  
│   │       │      // *(添加描述)*
│   │       └── 🆂 onnx_demo.scala  
│   │              // *(添加描述)*
│   ├── 📂 verilog/
│   │   └── 📄 .gitignore  
│   │          // *(添加描述)*
│   └── 📂 vhdl/
│       └── 📄 .gitignore  
│              // *(添加描述)*
├── 📂 lib/
├── 📂 project/
│   ├── 📂 project/
│   │   ├── 📂 project/
│   │   │   ├── 📂 project/
│   │   │   └── 📄 metals.sbt  
│   │   │          // *(添加描述)*
│   │   └── 📄 metals.sbt  
│   │          // *(添加描述)*
│   ├── 📄 build.properties  
│   │      // *(添加描述)*
│   ├── 📄 metals.sbt  
│   │      // *(添加描述)*
│   └── 📄 plugins.sbt  
│          // *(添加描述)*
├── 📂 simWorkspace/
│   ├── 📂 .pluginsCachePath/
│   │   ├── 🅒🅟🅟 SharedMemIface.cpp  
│   │   │      // include"SharedMemIface.hpp"
│   │   ├── 📄 SharedMemIface.hpp  
│   │   │      // pragma once
│   │   ├── 📄 SharedMemIface.o  
│   │   │      // *(添加描述)*
│   │   ├── 📄 SharedMemIface_wrap.cxx  
│   │   │      // *(添加描述)*
│   │   ├── 📄 SharedMemIface_wrap.o  
│   │   │      // *(添加描述)*
│   │   ├── 📄 SharedStruct.hpp  
│   │   │      // pragma once
│   │   ├── 🅒🅟🅟 VpiPlugin.cpp  
│   │   │      // ifndef SHMEM_FILENAME
│   │   ├── 📄 shared_mem_iface.so  
│   │   │      // *(添加描述)*
│   │   └── 📄 vpi_vcs.so  
│   │          // *(添加描述)*
│   ├── 📂 MyTopLevel/
│   │   ├── 📂 64/
│   │   ├── 📂 AN.DB/
│   │   │   ├── 📂 debug_dump/
│   │   │   ├── 📄 .vcs_lib_lock  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 AllModulesSkeletons.sdb  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 compat.db  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 dumpcheck.db  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 dve.sdb  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 make.vlogan  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 modfilename.db  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 str.db  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 str.index.db  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 str.info.db  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 vir.sdb  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 vir_global.sdb  
│   │   │   │      // *(添加描述)*
│   │   │   └── 📄 vloganopts.db  
│   │   │          // *(添加描述)*
│   │   ├── 📂 MyTopLevel.daidir/
│   │   │   ├── 📂 scsim.db.dir/
│   │   │   │   ├── 📂 cfg/
│   │   │   │   ├── 📄 scsim.db.file  
│   │   │   │   │      // *(添加描述)*
│   │   │   │   └── 📄 snps_setup.map  
│   │   │   │          // *(添加描述)*
│   │   │   ├── 📄 mxmap.db  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 mxopt.db  
│   │   │   │      // *(添加描述)*
│   │   │   └── 📄 mxsetup.db  
│   │   │          // *(添加描述)*
│   │   ├── 📂 csrc/
│   │   │   └── 📂 vh/
│   │   │       └── 📄 scsFilelist.t  
│   │   │              // include "stdio.h"
│   │   ├── 📂 rtl/
│   │   │   ├── 📄 MyTopLevel.v  
│   │   │   │      // *(添加描述)*
│   │   │   └── 📄 __simulation_def.v  
│   │   │          // *(添加描述)*
│   │   ├── 📂 work.lib++/
│   │   │   ├── 📂 oh.etc/
│   │   │   │   ├── 📄 _oharch  
│   │   │   │   │      // *(添加描述)*
│   │   │   │   └── 📄 oh.rc  
│   │   │   │          // *(添加描述)*
│   │   │   ├── 📄 AbsDocNameTbl  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 MapTbl  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 lib.dep  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 libfile  
│   │   │   │      // *(添加描述)*
│   │   │   ├── 📄 libfile.1-  
│   │   │   │      // *(添加描述)*
│   │   │   └── 📄 tdc.sdb  
│   │   │          // *(添加描述)*
│   │   ├── 📄 .vlogansetup.args  
│   │   │      // *(添加描述)*
│   │   ├── 📄 filelist.f  
│   │   │      // *(添加描述)*
│   │   ├── 📄 vcs.log  
│   │   │      // *(添加描述)*
│   │   └── 📄 vlogan.log  
│   │          // *(添加描述)*
│   └── 📂 unamed/
│       ├── 📂 rtl/
│       │   └── 📄 unamed.sv  
│       │          // *(添加描述)*
│       └── 📄 unamed.sby  
│              // *(添加描述)*
├── 📂 sw/
│   └── 📂 java/
│       ├── 📂 main/
│       │   ├── 📂 java/
│       │   │   └── 📂 org/
│       │   │       ├── 📂 forwarder/
│       │   │       ├── 📂 onnx4j/
│       │   │       └── ☕ ScoreMNIST.java  
│       │   │              // *(添加描述)*
│       │   └── 📂 resources/
│       │       ├── 📂 META-INF/
│       │       │   └── 📂 services/
│       │       ├── 📂 models/
│       │       │   └── 📂 mnist/
│       │       ├── 📂 static/
│       │       │   ├── 📂 resources/
│       │       │   └── 🌐 index.html  
│       │       │          // *(添加描述)*
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
│           │   └── ☕ onnx4j_run.java  
│           │          // *(添加描述)*
│           └── 📂 resources/
│               ├── 📂 mnist/
│               │   ├── 📂 opset_v1/
│               │   ├── 📂 opset_v7/
│               │   └── 📂 opset_v8/
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
├── 📂 verdiLog/
│   ├── 📄 exe.log  
│   │      // *(添加描述)*
│   ├── 📄 novas.log  
│   │      // *(添加描述)*
│   ├── 📄 turbo.log  
│   │      // *(添加描述)*
│   └── 📄 verdi_perf_err.log  
│          // *(添加描述)*
├── 📄 .gitignore  
│      // *(添加描述)*
├── 📄 .mill-version  
│      // *(添加描述)*
├── ⚙️ .scalafmt.conf  
│      // *(添加描述)*
├── 📄 .vlogansetup.args  
│      // *(添加描述)*
├── 📝 PROJECT_STRUCTURE.md  
│      // 项目结构文档
├── 📝 README.md  
│      // Onnx_SpinalHDL_interface
├── 📄 absnet.onnx  
│      // *(添加描述)*
├── 🐍 autotreedoc.py  
│      // *(添加描述)*
├── 📄 build.sbt  
│      // *(添加描述)*
├── 📄 build.sc  
│      // *(添加描述)*
├── ⚙️ novas.conf  
│      // *(添加描述)*
├── 📄 novas.rc  
│      // *(添加描述)*
├── 📄 squeezenet1_1_Opset18.onnx  
│      // *(添加描述)*
└── 📄 vlogan.log  
       // *(添加描述)*

> 文档自动生成于 .