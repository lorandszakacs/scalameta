import org.scalatest._
import scala.meta._
import scala.meta.dialects.Scala211

class QuasiquoteSuite extends FunSuite {
  test("q\"foo($term, ..$terms, $term)\"") {
    val term = q"x"
    val terms = List(q"y", q"z")
    assert(q"foo($term, ..$terms, $term)".show[Code] === "foo(x, y, z, x)")
  }

  test("q\"foo[..$types]\"") {
    val types = List(t"T", t"U")
    assert(q"foo[..$types]".show[Code] === "foo[T, U]")
  }

  test("rank-0 liftables") {
    assert(q"foo[${42}]".show[Code] === "foo[42]")
    assert(q"${42}".show[Code] === "42")
  }

  test("rank-1 liftables") {
    implicit def custom[U >: List[Term]]: Liftable[List[Int], U] = Liftable(_.map(x => q"$x"))
    assert(q"foo(..${List(1, 2, 3)})".show[Code] === "foo(1, 2, 3)")
  }

  test("q\"$foo(${x: Int})\"") {
    q"foo(42)" match {
      case q"$foo(${x: Int})" =>
        assert(foo.show[Code] === "foo")
        assert(x == 42)
    }
  }
}