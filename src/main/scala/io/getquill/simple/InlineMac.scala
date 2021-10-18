package io.getquill.simple

import scala.quoted._

object InlineMac {
  inline def matchOne(inline value: String): String = ${ matchOneImpl('value) }
  def matchOneImpl(value: Expr[String])(using Quotes): Expr[String] = {
    import quotes.reflect._
    value match
      case '{ "foo" } => '{ "foo2" }
      case _ => report.throwError("Invalid value", value)
  }

  inline def matchTwo(inline value: String): String = ${ matchTwoImpl('value) }
  def matchTwoImpl(value: Expr[String])(using Quotes): Expr[String] = {
    import quotes.reflect._
    value.asTerm.underlyingArgument.asExpr match
      case '{ "foo2" } => '{ "foo3" }
  }
}
