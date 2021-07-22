package io.getquill

object Example {

  case class Person(id: Int, name: String, age: Int)
  case class Address(street: String, zip: Int, personId: Int)

  def main(args: Array[String]): Unit = {
    val ctx = new SqlMirrorContext(PostgresDialect, Literal)
    import ctx._
    inline def q =
      for {
        p <- query[Person]
        a <- query[Address].join(a => a.personId == p.id)
      } yield (p, a)

    println(run(q).string)
  }
}
