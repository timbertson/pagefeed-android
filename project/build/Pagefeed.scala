import sbt._

trait Defaults {
  def androidPlatformName = "android-7"
}
class Pagefeed(info: ProjectInfo) extends ParentProject(info) {
  override def shouldCheckOutputDirectories = false
  override def updateAction = task { None }

  lazy val main  = project(".", "Pagefeed", new MainProject(_))
  /*lazy val tests = project("tests",  "tests", new TestProject(_), main)*/

  class MainProject(info: ProjectInfo) extends AndroidProject(info) with Defaults with MarketPublish {
    val scalatest = "org.scalatest" % "scalatest" % "1.0" % "test"
    val keyalias = "android"
  }

  /*class TestProject(info: ProjectInfo) extends AndroidTestProject(info) with Defaults*/
}
