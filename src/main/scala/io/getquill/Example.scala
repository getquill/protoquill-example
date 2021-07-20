package io.getquill

object Example {

  case class Person(name: String, age: Int)

  def main(args: Array[String]): Unit = {
    val ctx = new SqlMirrorContext(PostgresDialect, Literal)
    import ctx._
    inline def q =
      query[Person]

    println(run(q).string)
  }
}
