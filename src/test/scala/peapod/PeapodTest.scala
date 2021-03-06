package peapod

import generic.PeapodGenerator
import org.scalatest.FunSuite
import peapod.StorableTask._

class PeapodTest  extends FunSuite {
  class TaskA(implicit val p: Peapod) extends EphemeralTask[Double]  {
    def generate = 1
  }

  class TaskB(implicit val p: Peapod) extends EphemeralTask[Double]  {
    pea(new TaskA())
    def generate = 1
  }

  class TaskC(implicit val p: Peapod) extends StorableTask[Double]  {
    pea(new TaskB())
    def generate = 1
  }

  class TaskA1(implicit val p: Peapod) extends StorableTask[Double]  {
    override lazy val baseName = "TaskA"
    override val version = "1"
    override val description = "Return 1 Always"
    def generate = 1
  }
  class TaskA2(implicit val p: Peapod) extends StorableTask[Double]  {
    override lazy val baseName = "TaskA"
    override val version = "2"
    def generate = 1
  }
  class TaskB1(implicit val p: Peapod) extends StorableTask[Double]  {
    override lazy val baseName = "TaskB"
    override val description = "Return 1 Always"
    pea(new TaskA1())
    def generate = 1
  }
  class TaskB2(implicit val p: Peapod) extends StorableTask[Double]  {
    override lazy val baseName = "TaskB"
    pea(new TaskA2())
    def generate = 1
  }

  test("Delete Other Versions") {
    implicit val p = PeapodGenerator.peapod()
    p(new TaskB1()).get()
    assert(new TaskA1().exists())
    assert(new TaskB1().exists())
    p.clear()
    p(new TaskB2()).get()
    assert(new TaskA1().exists())
    assert(new TaskA2().exists())
    assert(new TaskB1().exists())
    assert(new TaskB2().exists())
    p.deleteOtherVersions()
    assert(! new TaskA1().exists())
    assert(new TaskA2().exists())
    assert(! new TaskB1().exists())
    assert(new TaskB2().exists())
  }


  test("Dependencies") {
    implicit val p = PeapodGenerator.peapod()
    val peaA = p(new TaskA())
    val peaB = p(new TaskB())
    assert(peaA.children == Set(peaB))
    assert(peaB.parents == Set(peaA))
  }

  test("Size") {
    implicit val p = PeapodGenerator.peapod()
    val peaA = p(new TaskA())
    assert(p.size() == 1)
    val peaB = p(new TaskB())
    assert(p.size() == 2)
  }

  test("SizeStored") {
    implicit val p = PeapodGenerator.peapod()
    p(new TaskC()).get()
    assert(p.size() == 3)
    p.clear()
    assert(p.size() == 0)
    val peaC = p(new TaskC())
    assert(peaC.task.exists())
    assert(p.size() == 3)
  }

  test("Clear") {
    implicit val p = PeapodGenerator.peapod()
    val peaA = p(new TaskA())
    assert(p.size() == 1)
    val peaB = p(new TaskB())
    assert(p.size() == 2)
    p.clear()
    assert(p.size() == 0)
  }

  test("DotFormatterStored") {
    implicit val p = PeapodGenerator.peapod()
    p(new TaskC()).get()
    val dot = p.dotFormatDiagram()
    p.clear()
    p(new TaskC())
    assert(p.dotFormatDiagram() == dot)
  }

  test("DotFormatter") {
    implicit val p = PeapodGenerator.peapod()
    p(new TaskB())
    val dot = p.dotFormatDiagram()
    assert(dot ==
      "digraph G {node [shape=box]\"peapod.PeapodTest$TaskA\" [style=dotted];\n\"peapod.PeapodTest$TaskB\" " +
        "[style=dotted];\"peapod.PeapodTest$TaskA\"->\"peapod.PeapodTest$TaskB\";" +
        "{ rank=same;\"peapod.PeapodTest$TaskA\"}{ rank=same;\"peapod.PeapodTest$TaskB\"}}"
    )

    assert(Util.gravizoDotLink(dot) == "http://g.gravizo.com/g?digraph%20G%20%7Bnode%20%5Bshape%3Dbox%5D%22peapod." +
      "PeapodTest%24TaskA%22%20%5Bstyle%3Ddotted%5D%3B%0A%22peapod.PeapodTest%24TaskB%22%20%5Bstyle%3Ddotted%5D%3B" +
      "%22peapod.PeapodTest%24TaskA%22-%3E%22peapod.PeapodTest%24TaskB%22%3B%7B%20rank%3Dsame%3B%22peapod.PeapodTest" +
      "%24TaskA%22%7D%7B%20rank%3Dsame%3B%22peapod.PeapodTest%24TaskB%22%7D%7D")

    assert(Util.mindfulmachinesDotLink(dot) == "http://graphvizserver-env.elasticbeanstalk.com/?H4sIAAAAAAAAAEvJTC9KLMh" +
      "QcFeozstPSVWILs5ILEi1TcqviFUqSE0syE_RCwBTIanFJSohicXZjkpARSWVOam2KfklJakpsdZcOFQ6YajEaaSuHU4zrKsVihLzsm2LE3NTcRt" +
      "QS4QqJ6XaWgAcspo28AAAAA")

  }
}
