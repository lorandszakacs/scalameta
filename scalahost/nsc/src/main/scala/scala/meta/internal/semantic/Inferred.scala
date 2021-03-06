package scala.meta
package internal.semantic

case class SugarRange(start: Int, end: Int, symbol: Symbol) {
  def addOffset(offset: Int) = SugarRange(start + offset, end + offset, symbol)
  def toMeta(input: Input): (Position, Symbol) =
    (Position.Range(input, start, end), symbol)
}
case class AttributedSugar(syntax: String, names: List[SugarRange]) {
  def +(other: String) = AttributedSugar(syntax + other, names)
  def +(other: AttributedSugar) =
    AttributedSugar(syntax + other.syntax, names ++ other.names.map(_.addOffset(syntax.length)))
}

object AttributedSugar {
  val empty = AttributedSugar("", Nil)
  val star = AttributedSugar("*", List(SugarRange(0, 1, Symbol("_star_."))))
  def apply(syntax: String): AttributedSugar = AttributedSugar(syntax, Nil)
  def mkString(sugars: List[AttributedSugar], sep: String): AttributedSugar = sugars match {
    case Nil => empty
    case head :: Nil => head
    case head :: lst =>
      lst.foldLeft(head) {
        case (accum, sugar) =>
          accum + sep + sugar
      }
  }
}

// data structure to manage multiple inferred sugars at the same position.
case class Inferred(
    select: Option[AttributedSugar] = None,
    targs: Option[AttributedSugar] = None,
    conversion: Option[AttributedSugar] = None,
    args: Option[AttributedSugar] = None
) {
  assert(
    args.isEmpty || conversion.isEmpty,
    s"Not possible to define conversion + args! $args $conversion"
  )

  private def all: List[AttributedSugar] = (select :: targs :: conversion :: args :: Nil).flatten

  def toSugar(input: Input, pos: Position): Sugar = {
    def onlyConversionIsDefined =
      conversion.isDefined &&
        select.isEmpty &&
        targs.isEmpty &&
        args.isEmpty

    def needsPrefix: Boolean =
      !onlyConversionIsDefined

    val sugar: AttributedSugar = {
      val start =
        if (needsPrefix) AttributedSugar.star
        else AttributedSugar.empty
      all.foldLeft(start)(_ + _)
    }
    val sugarInput = Input.Sugar(sugar.syntax, input, pos.start, pos.end)
    val names = sugar.names.toIterator.map(_.toMeta(sugarInput)).toMap
    new Sugar(sugarInput, names)
  }

}
