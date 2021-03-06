import sbt._
import Settings.versions._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import play.sbt.PlayImport.guice
import play.sbt.PlayImport.ws
import sbt.Keys.libraryDependencies

object Settings {
  val name = "pipeline-board"

  /** Declare global dependency versions here to avoid mismatches in multi part dependencies */
  object versions {
    val scala = "2.12.2"
    val scalaDom = "0.9.2"
    val scalajsReact = "1.0.1"
    val scalaCSS = "0.5.3"
    val log4js = "1.4.10"
    val autowire = "0.2.6"
    val uTest = "0.4.7"

    val react = "15.6.1"
    val bootstrap = "3.3.7"
    val chartjs = "2.4.0"

    val scalajsScripts = "1.1.0"
    val circeVersion = "0.8.0"
    val enumeratumVersion = "1.5.12"
  }

  /**
    * These dependencies are shared between JS and JVM projects
    * the special %%% function selects the correct version for each project
    */
  val sharedDependencies = Def.setting(
    Seq(
      "com.lihaoyi" %%% "autowire" % autowire,
      "com.beachape" %%% "enumeratum" % enumeratumVersion,
      "com.beachape" %%% "enumeratum-circe" % enumeratumVersion,
      "com.lihaoyi" %%% "utest" % uTest % Test,
      "io.lemonlabs" %%% "scala-uri" % "0.5.1",
      "biz.enef" %%% "slogging" % "0.5.2",
      "com.github.japgolly.scalacss" %%% "core" % scalaCSS
    ) ++ Seq(
      "io.circe" %%% "circe-core",
      "io.circe" %%% "circe-generic",
      "io.circe" %%% "circe-parser",
      "io.circe" %%% "circe-java8"
    ).map(_ % circeVersion)
  )

  /** Dependencies only used by the JVM project */
  val jvmDependencies = Def.setting(Seq(
    "com.vmunier" %% "scalajs-scripts" % scalajsScripts,
    "org.webjars" % "font-awesome" % "4.7.0" % Provided,
    "org.webjars" % "bootstrap" % bootstrap % Provided,
    "com.typesafe.akka" %% "akka-typed" % "2.5.6",
    "biz.enef" %% "slogging-slf4j" % "0.5.2",
    "org.slf4j" % "slf4j-simple" % "1.7.+",
    "net.jcazevedo" %% "moultingyaml" % "0.4.0",
    guice, ws,
    "com.typesafe.slick" %% "slick" % "3.2.1",
    "org.slf4j" % "slf4j-nop" % "1.6.4",
    "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1",
    "org.xerial" % "sqlite-jdbc" % "3.21.0.1"
  ))

  /** Dependencies only used by the JS project (note the use of %%% instead of %%) */
  val scalajsDependencies = Def.setting(Seq(
    "com.github.japgolly.scalajs-react" %%% "core" % scalajsReact,
    "com.github.japgolly.scalajs-react" %%% "extra" % scalajsReact,
    "com.github.japgolly.scalacss" %%% "ext-react" % scalaCSS,
    "org.scala-js" %%% "scalajs-dom" % scalaDom,
    "com.github.japgolly.scalajs-react" %%% "test" % scalajsReact % "test"
    //"org.scala-js" %%% "scalajs-java-time" % "0.2.2" //todo why don't we need this?

  ))

  /** Dependencies for external JS libs that are bundled into a single .js file according to dependency order */
  val jsDependencies = Def.setting(Seq(
    "org.webjars.bower" % "react" % react / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
    "org.webjars.bower" % "react" % react / "react-dom.js" minified "react-dom.min.js" dependsOn "react-with-addons.js" commonJSName "ReactDOM",
    "org.webjars" % "chartjs" % chartjs / "Chart.js" minified "Chart.min.js",
    "org.webjars.bower" % "react" % react % "test" / "react-dom-server.js" minified  "react-dom-server.min.js" dependsOn "react-dom.js" commonJSName "ReactDOMServer")
  )
}