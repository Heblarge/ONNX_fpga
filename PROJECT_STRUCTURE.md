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
├── 📂 project/
│   ├── 📂 project/
│   ├── 📄 build.properties  
│   │      // *(添加描述)*
│   └── 📄 plugins.sbt  
│          // *(添加描述)*
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
│   ├── 📄 .diagnose.oneSearch  
│   │      // *(添加描述)*
│   ├── 📄 .verdi_onesearch_elabdir  
│   │      // *(添加描述)*
│   ├── 📄 novas.rc  
│   │      // *(添加描述)*
│   ├── 📄 novas_autosave.ses  
│   │      // *(添加描述)*
│   ├── 📄 novas_autosave.ses.config  
│   │      // *(添加描述)*
│   ├── 📄 novas_autosave.ses.png  
│   │      // *(添加描述)*
│   ├── 📄 novas_autosave.ses.wave.0  
│   │      // *(添加描述)*
│   ├── 📄 pes.bat  
│   │      // *(添加描述)*
│   └── 📄 verdi.cmd  
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
└── 📄 squeezenet1_1_Opset18.onnx  
       // *(添加描述)*

> 文档自动生成于 .