package org.deusaquilus

import io.getquill._

object Example {

  case class Person(name: String, age: Int)

  def main(args: Array[String]): Unit = {
    val ctx = new SqlMirrorContext(PostgresDialect, Literal)
    import ctx.*
    inline def q = quote {
      query[Person].filter(p => p.name == "Joe")
    }
    inline def v = query[Person]
    println(run(q).string)
  }
}
