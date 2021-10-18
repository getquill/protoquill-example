package io.getquill.simple

object InlineUse {
  def main(args: Array[String]): Unit = {
    inline def splice1 = InlineMac.matchOne("foo")
    // inline def splice1 = InlineMac.matchOne("bar")
    val splice2 = InlineMac.matchTwo(splice1)
  }
}
