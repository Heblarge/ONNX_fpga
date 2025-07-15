package playGround
import scala.sys.process._

object PATH_match extends App {
    val vpiInclude = scala.sys.env.get("VCS_HOME")
    println("VCS_HOME"+vpiInclude)
    // 执行 `which g++` 命令
    val gppPath = "which g++".!!
    // 输出结果
    println(s"The g++ path is: $gppPath")
}