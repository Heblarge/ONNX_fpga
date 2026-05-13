import org.junit.Test;
import static org.junit.Assert.*;
import projectname.MyTopLevelSim;
import scala.collection.JavaConverters;
import scala.collection.Seq;
import scala.Tuple2;

import java.util.Arrays;
import java.util.List;

public class MyTopLevelSimJavaTest {
    @Test
    public void testRunSim() {
        // 构造输入序列
        List<Boolean> cond0List = Arrays.asList(true, false, true, false, true);
        List<Boolean> cond1List = Arrays.asList(false, true, false, true, false);

        // Java List 转 Scala Seq
        Seq<java.lang.Boolean> cond0Seq = JavaConverters.asScalaBuffer(cond0List).toSeq();
        Seq<java.lang.Boolean> cond1Seq = JavaConverters.asScalaBuffer(cond1List).toSeq();

        // 调用 Scala 仿真方法
        scala.collection.Seq<Tuple2<Object, Object>> resultSeq = MyTopLevelSim.runSim(cond0Seq, cond1Seq);

        // 断言结果数量
        assertEquals(cond0List.size(), resultSeq.size());

        // 断言部分结果内容（可根据实际模型逻辑调整）
        Tuple2<Object, Object> first = resultSeq.apply(0);
        assertTrue(first._1() instanceof Integer);
        assertTrue(first._2() instanceof Boolean);
        // 输出第一个结果
        System.out.println("state: " + first._1() + ", flag: " + first._2());
        // 输出所有结果
        for (Tuple2<Object, Object> tup : scala.collection.JavaConverters.seqAsJavaList(resultSeq)) {
            System.out.println("state: " + tup._1() + ", flag: " + tup._2());
        }
        // 可添加更多断言
    }
}
