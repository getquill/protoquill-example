//package io.getquill
//
//import org.apache.spark.sql.SparkSession
//import Makers._
//
//object WriteToParquetExample {
//
//  val session =
//    SparkSession.builder()
//      .master("local")
//      .appName("spark test")
//      .getOrCreate()
//
//  // TODO fix documentation this needs to be session.sqlContext, not session
//  implicit val sqlContext = session.sqlContext
//  import sqlContext.implicits._      // Also needed...
//
//  def main(args: Array[String]): Unit = {
//    val peopleLarge = (1 to 100).map(makePersonLarge(_)).toDS()
//    val addressesLarge = (1 to 100).map(makeAddressLarge(_)).toDS()
//
//    peopleLarge.write.parquet("peopleLarge")
//    addressesLarge.write.parquet("addressesLarge")
//  }
//}
//
//object SparkExample {
//
//  val session =
//    SparkSession.builder()
//      .master("local")
//      .appName("spark test")
//      .getOrCreate()
//
//  // TODO fix documentation this needs to be session.sqlContext, not session
//  implicit val sqlContext = session.sqlContext
//  import sqlContext.implicits._      // Also needed...
//
//  // Import the Quill Spark Context
//  import io.getquill.QuillSparkContext._
//
//  def main(args: Array[String]): Unit = {
//
//    val peopleLarge = session.read.parquet("peopleLarge").as[PersonLarge]
//    val addressesLarge = session.read.parquet("addressesLarge").as[Address]
//
////    val ds = peopleLarge.map(p => (p.id, p.name))
////    ds.explain()
//
////    val ds = run { liftQuery(peopleLarge).map(p => (p.id, p.name)) }
////    ds.explain()
//
//    val q = quote {
//      for {
//        p <- liftQuery(peopleLarge)
//        a <- liftQuery(addressesLarge) if (p.id == a.ownerFk)
//      } yield (p.name, a.street)
//    }
//    val peopleAndAddresses = run(q)
//
//    peopleAndAddresses.show() //
//
//
//    //val peopleLarge = List()
//
////    val people = List(Person(1, "Joe", 123), Person(2, "Jack", 456)).toDS()
////    val addresses = List(Address(1, "123 St"), Address(2, "456 St")).toDS()
////
////    val q = quote {
////      for {
////        p <- liftQuery(people)
////        a <- liftQuery(addresses) if (p.id == a.ownerFk)
////      } yield (p, a)
////    }
////    val peopleAndAddresses = run(q)
////    peopleAndAddresses.show()
//
//
//
//  }
//
//}
