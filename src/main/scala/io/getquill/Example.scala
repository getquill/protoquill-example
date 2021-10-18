package io.getquill

object Example {

  case class Person(id: Int, name: String, age: Int)
  case class Address(fk: Int, street: String)

  def main(args: Array[String]): Unit = {
    val ctx = new SqlMirrorContext(PostgresDialect, Literal)
    import ctx._
    inline def addresses = quote {
      query[Address].filter(a => a.street == "foo")
      // query[Address].filter(a => a.street == System.getProperty("foo"))
    }
    inline def q = quote {
      for {
        p <- query[Person]
        a <- addresses.join(a => a.fk == p.id)
      } yield (p, a)
    }

    println(run(q).string)
  }
}
