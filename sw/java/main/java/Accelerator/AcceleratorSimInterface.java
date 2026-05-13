package Accelerator;

/**
 * 空的仿真接口类 - 用于上板环境
 * 实际仿真功能在 SpinalHDL 生成的类中
 */
public class AcceleratorSimInterface {
    // 空实现，仅用于编译通过

    public static AcceleratorCfg acceleratorCfg = new AcceleratorCfg();

    public static long[][] runRefOneInst(long[][] input0, long[][] input1, InstJavaTODO inst) {
        // 上板环境不需要仿真，返回空数组
        return new long[0][0];
    }

    public static AcceleratorCfg acceleratorCfg() {
        return acceleratorCfg;
    }

    public static class AcceleratorCfg {
        // 空实现 - 上板环境不需要仿真配置
        public int fracWidth() {
            return 19;  // 默认值
        }

        public int intWidth() {
            return 32;  // 默认值
        }
    }
}
