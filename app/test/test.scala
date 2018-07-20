package test

object test {

  def main(args: Array[String]): Unit = {
    val a = 694.toDouble
    val b = 19.toDouble
    val t = a/b/1024*100
    println(t.formatted("%.2f"))
  }
}
