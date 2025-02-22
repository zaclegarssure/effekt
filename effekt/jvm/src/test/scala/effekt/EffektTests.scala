package effekt

import java.io.File

import sbt.io._
import sbt.io.syntax._
import scala.sys.process.*

import scala.language.implicitConversions

trait EffektTests extends munit.FunSuite {

  // The name of the backend as it is passed to the --backend flag.
  def backendName: String

  def output: File = new File(".") / "out" / "tests" / getClass.getName.toLowerCase

  // The sources of all testfiles are stored here:
  def examplesDir = new File("examples")

  // Test files which are to be ignored (since features are missing or known bugs exist)
  def ignored: List[File] = List()

  // Folders to discover and run tests in
  def included: List[File] = List()

  def runTestFor(input: File, expected: String): Unit =
    test(input.getPath + s" (${backendName})") {
      assertNoDiff(run(input), expected)
    }

  def run(input: File): String =
    val compiler = new effekt.Driver {}
    val configs = compiler.createConfig(Seq(
      "--Koutput", "string",
      "--backend", backendName,
      "--out", output.getPath
    ))
    configs.verify()
    compiler.compileFile(input.getPath, configs)
    configs.stringEmitter.result()


  def runTests() =
    Backend.backend(backendName).runner.checkSetup() match {
      case Left(msg) => test(s"${this.getClass.getName}: ${msg}".ignore) { () }
      case Right(value) => included.foreach(runPositiveTestsIn)
    }

  def runPositiveTestsIn(dir: File): Unit = //describe(dir.getName) {
    dir.listFiles.foreach {
      case f if f.isDirectory && !ignored.contains(f) =>
        runPositiveTestsIn(f)
      case f if f.getName.endsWith(".effekt") || f.getName.endsWith(".effekt.md") =>
        val path = f.getParentFile
        val baseName = f.getName.stripSuffix(".md").stripSuffix(".effekt")

        val checkfile = path / (baseName + ".check")

        if (!checkfile.exists()) {
          sys error s"Missing checkfile for ${f.getPath}"
        }

        if (ignored.contains(f)) {
          test(f.getName.ignore) { () }
        } else {
          val contents = IO.read(checkfile)
          runTestFor(f, contents)
        }

      case _ => ()
    }

  runTests()
}
