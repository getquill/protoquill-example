package io.getquill

object Example {
  case class Person(id: Int, name: String, age: Int)
  case class Address(owner:Int, street: String)
  val ctx = new SqlMirrorContext(PostgresDialect, Literal)
  import ctx._

  val people = run(query[Person])
  val addresses = run(query[Person])
  val peopleAndAddresses = run(query[Person].join(query[Address]).on((p, a) => p.id == a.owner))
}

//
//import java.util
//
//
//object Example {
//  val ctx = new SqlMirrorContext(PostgresDialect, Literal)
//  import ctx._
//
//  /*
//def myUDF(t: CustomerTable[id, name, age]) =
//  t join JOIN Address a on a.id == t.ownerId
//
//myUDF(SELECT p.id, p.name, p.age, a.street FROM Person p)
//UNION
//myUDF(SELECT p.id, p.name, p.age, a.street FROM VIP p)
//
//
//SELECT p.id, p.name, p.age, a.street FROM Person
//JOIN Address a on a.id == p.ownerId
//UNION
//SELECT p.id, p.name, p.age, a.street FROM VIP
//JOIN Address a on a.id == p.ownerId
//
//SELECT p.id, p.name, p.age, a.street FROM (
//  SELECT p.id, p.name, p.age FROM Person
//  UNION
//  SELECT p.id, p.name, p.age FROM VIP
//) AS p
//JOIN Address a on a.id == p.ownerId
// */
//
//  case class Person(id: Int, name: String, age: Int)
//  case class VIP(id: Int, name: String, age: Int, otherStuff: String)
//  case class Address(ownerId: Int, street: String)
//
//  case class CustomerTable(id: Int, name: String, age: Int)
//
//
//
//  def main(args: Array[String]): Unit = {
//
//    /*
//    def myUDF(customerTable: CustomerTable[id, name, age]) =
//       t join JOIN Address a on a.id == customerTable.ownerId
//     */
//
//    // Query[CustomerTable] => Query[(CustomerTable, Address)]
//    val myUDF = quote {
//      (customerTable: Query[CustomerTable]) =>
//        for {
//          customer <- customerTable
//          address <- query[Address].join(a => a.ownerId == customer.id)
//        } yield (customer, address)
//    }
//
//    val result = quote {
//      /*
//      myUDF(SELECT p.id, p.name, p.age, a.street FROM Person p)
//        UNION
//      myUDF(SELECT p.id, p.name, p.age, a.street FROM VIP p)
//       */
//      myUDF(query[Person].map(p => CustomerTable(p.id, p.name, p.age))) union
//        myUDF(query[VIP].map(p => CustomerTable(p.id, p.name, p.age)))
//    }
//
//    println(run(result)) //
//
//
//    // run(q) // List[(Person, Option[Address])]
//
//    /*
//    SELECT
//      p.*, a.*
//     FROM
//      Person p JOIN Address a ON p.id == a.ownerId
//
//     */
//  }
//}
