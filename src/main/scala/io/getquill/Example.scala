package io.getquill

object Example {

  case class Person(name: String, age: Int)

  def main(args: Array[String]): Unit = {
    val ctx = new SqlMirrorContext(PostgresDialect, Literal)
    import ctx._
    inline def q = quote {
      query[Person].filter(p => p.name == "Joe")
    }
    println(run(q).string)
  }
}
