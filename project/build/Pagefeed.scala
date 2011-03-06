import sbt._

trait Defaults {
	def androidPlatformName = "android-7"
}
class Pagefeed(info: ProjectInfo) extends ParentProject(info) with de.tuxed.codefellow.plugin.CodeFellowPlugin {
	override def shouldCheckOutputDirectories = false
	override def updateAction = task { None }

	lazy val main  = project(".", "Pagefeed", new MainProject(_))
	/*lazy val tests = project("tests",  "tests", new TestProject(_), main)*/

	class MainProject(info: ProjectInfo) extends AndroidProject(info) with Defaults with MarketPublish {
		val scalatest = "org.scalatest" % "scalatest" % "1.1" % "test"
		val mockito = "org.mockito" % "mockito-core" % "1.8.5" % "test"
		val keyalias = "android"
		override def compileOrder = CompileOrder.JavaThenScala
	}

	/*class TestProject(info: ProjectInfo) extends AndroidTestProject(info) with Defaults*/
}
