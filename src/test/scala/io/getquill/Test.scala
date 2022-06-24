package io.getquill

import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import io.getquill.Quoted
import io.getquill.ast._
import io.getquill.quat.Quat
import io.getquill.NamingStrategy
import io.getquill.idiom.Idiom
import io.getquill.Query



class Test extends AnyFreeSpec with Matchers with BeforeAndAfterAll {
  case class Person(name: String, age: Int)

  val ctx = new SqlMirrorContext(PostgresDialect, Literal) //new PostgresJdbcContext(Literal, "testPostgresDB")
  import ctx._

  "foo" in {
    val q = quote { query[Person] } //hellooooooooooooooooooooo
    println(ctx.run(q))
  }
}
