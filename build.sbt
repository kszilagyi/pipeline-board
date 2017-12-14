import sbt.Project.projectToRef
import sbt.Keys._

//Skeleton copied and modified from: https://github.com/ochrons/scalajs-spa-tutorial

/** Options for the scala compiler */
val customScalacOptionsWoUnusedParams = Seq(
  //"-Xlint",
  "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
  "-encoding", "utf-8",                // Specify character encoding used by source files.
  "-explaintypes",                     // Explain type errors in more detail.
  "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
  "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
  "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
  "-Xlint:by-name-right-associative",  // By-name parameter of right associative operator.
  "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
  "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
  "-Xlint:option-implicit",            // Option.apply used implicit view.
  "-Xlint:package-object-classes",     // Class or object defined in package object.
  "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
  "-Xlint:unsound-match",              // Pattern match may not be typesafe.
  "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
  "-Ywarn-dead-code",                  // Warn when dead code is identified.
  "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
  "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
  "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
  "-Ywarn-numeric-widen",              // Warn when numerics are widened.
  "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
  //"-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals",              // Warn if a local definition is unused.
  //"-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates",            // Warn if a private member is unused.
  "-Ywarn-value-discard"               // Warn when non-Unit expression results are unused.

)
val unusedImports = "-Ywarn-unused:imports"


lazy val customScalacOptions = customScalacOptionsWoUnusedParams ++ unusedParams
lazy val unusedParams = Seq("-Ywarn-unused:params")

lazy val scalahostSettings = Seq(
  addCompilerPlugin("org.scalameta" % "scalahost" % "1.8.0" cross CrossVersion.full),
  scalacOptions ++= Seq(
    "-Yrangepos",
    "-Xplugin-require:scalahost"
  )
)

lazy val macroAnnotationSettings = Seq(
  resolvers += Resolver.sonatypeRepo("releases"),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
)

def commonSettings(customScalac: Seq[String]) = Seq(
  wartremoverErrors ++= Warts.allBut(Wart.Any, Wart.Nothing, Wart.ImplicitParameter, Wart.Product, Wart.Serializable,
                                     Wart.PublicInference),
  // use uTest framework for tests
  testFrameworks += new TestFramework("utest.runner.Framework"),
  addCompilerPlugin(
    "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
  ),
  scalacOptions ++= customScalac
) ++ macroAnnotationSettings

// a special crossProject for configuring a JS/JVM/shared structure
lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(
    scalaVersion := Settings.versions.scala,
    scalahostSettings,
    libraryDependencies ++= Settings.sharedDependencies.value,
    cancelable in Global := true,
    commonSettings(customScalacOptions :+ unusedImports)

  )
  // set up settings specific to the JS project
  .jsConfigure(_ enablePlugins ScalaJSWeb)
  .dependsOn(macros)

lazy val macros = (crossProject.crossType(CrossType.Pure) in file("macros"))
  .settings(
    scalaVersion := Settings.versions.scala,
    libraryDependencies ++= Settings.sharedDependencies.value,
    cancelable in Global := true,
    commonSettings(customScalacOptionsWoUnusedParams),
    scalacOptions --= unusedParams
  )
  // set up settings specific to the JS project
  .jsConfigure(_ enablePlugins ScalaJSWeb)

lazy val macrosJVM = macros.jvm.settings(name := "macrosJVM")

lazy val macrosJS = macros.js.settings(name := "macrosJS")

lazy val sharedJVM = shared.jvm.settings(name := "sharedJVM")

lazy val sharedJS = shared.js.settings(name := "sharedJS")

// use eliding to drop some debug code in the production build
lazy val elideOptions = settingKey[Seq[String]]("Set limit for elidable functions")

// instantiate the JS project for SBT with some additional settings
lazy val client: Project = (project in file("client"))
  .settings(
    name := "client",
    version := Settings.version,
    scalaVersion := Settings.versions.scala,
    libraryDependencies ++= Settings.scalajsDependencies.value,
    // by default we do development build, no eliding
    elideOptions := Seq(),
    scalacOptions ++= elideOptions.value,
    jsDependencies ++= Settings.jsDependencies.value,
    // RuntimeDOM is needed for tests
    jsDependencies += RuntimeDOM % "test",
    // yes, we want to package JS dependencies
    skip in packageJSDependencies := false,
    // use Scala.js provided launcher code to start the client app
    scalaJSUseMainModuleInitializer := true,
    scalaJSUseMainModuleInitializer in Test := false,
    commonSettings(customScalacOptions :+ unusedImports),
    wartremoverErrors -= Wart.ToString //this is usually translated to js tostring which the scala code don't know about
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .dependsOn(sharedJS % "test->test;compile->compile")

// Client projects (just one in this case)
lazy val clients = Seq(client)

// instantiate the JVM project for SBT with some additional settings
lazy val server = (project in file("server"))
  .settings(
    name := "server",
    version := Settings.version,
    scalaVersion := Settings.versions.scala,
    libraryDependencies ++= Settings.jvmDependencies.value,
    commands += ReleaseCmd,
    // triggers scalaJSPipeline when using compile or continuous compilation
    compile in Compile := {
      ((compile in Compile) dependsOn scalaJSPipeline).value
    },
    // connect to the client project
    scalaJSProjects := clients,
    pipelineStages in Assets := Seq(scalaJSPipeline),
    pipelineStages := Seq(digest, gzip),
    // compress CSS
    LessKeys.compress in Assets := true,
    wartremoverExcluded += crossTarget.value / "routes" / "main" / "router" / "Routes.scala",
    wartremoverExcluded += crossTarget.value / "routes" / "main" / "router" / "RoutesPrefix.scala",
    wartremoverExcluded += crossTarget.value / "routes" / "main" / "controllers" / "ReverseRoutes.scala",
    wartremoverExcluded += crossTarget.value / "routes" / "main" / "controllers" / "javascript" / "JavaScriptReverseRoutes.scala",
    wartremoverExcluded += crossTarget.value / "twirl" / "main"/ "views" / "html" / "index.template.scala",
    wartremoverExcluded += crossTarget.value / "twirl" / "main"/ "views" / "html" / "tags" / "_asset.template.scala",
    commonSettings(customScalacOptions)
  )
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin) // use the standard directory layout instead of Play's custom
  .aggregate(clients.map(projectToRef): _*)
  .dependsOn(sharedJVM % "test->test;compile->compile")


// Command for building a release
lazy val ReleaseCmd = Command.command("release") {
  state =>
    "set elideOptions in client := Seq(\"-Xelide-below\", \"WARNING\")" ::
      "client/clean" ::
      "client/test" ::
      "server/clean" ::
      "server/test" ::
      "server/dist" ::
      "set elideOptions in client := Seq()" ::
      state
}

lazy val root = (project in file(".")).aggregate(client, server, sharedJVM, sharedJS)

// loads the root project at sbt startup
onLoad in Global := (Command.process("project root", _: State)) compose (onLoad in Global).value