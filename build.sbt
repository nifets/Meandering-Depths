
ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"
scalacOptions ++= Seq("-deprecation", "-feature")
lazy val root = (project in file("."))
  .settings(
    name := "Meandering Depths",
	libraryDependencies  ++= Seq(
		// Last stable release
		"org.scalanlp" %% "breeze" % "1.0",

		// Native libraries are not included by default. add this if you want them
		// Native libraries greatly improve performance, but increase jar sizes.
		// It also packages various blas implementations, which have licenses that may or may not
		// be compatible with the Apache License. No GPL code, as best I know.
		"org.scalanlp" %% "breeze-natives" % "1.0",

		// The visualization library is distributed separately as well.
		// It depends on LGPL code
		"org.scalanlp" %% "breeze-viz" % "1.0",

		"org.lwjgl" % "lwjgl" % "3.2.3",

		"org.lwjgl" % "lwjgl" % "3.2.3" classifier s"natives-windows",

		"org.lwjgl" % "lwjgl-opengl" % "3.2.3",

		"org.lwjgl" % "lwjgl-opengl" % "3.2.3" classifier s"natives-windows",

		"org.lwjgl" % "lwjgl-glfw" % "3.2.3",

		"org.lwjgl" % "lwjgl-glfw" % "3.2.3" classifier s"natives-windows",

        "org.scalactic" %% "scalactic" % "3.1.2",

        "org.scalatest" %% "scalatest" % "3.1.2" % "test",

        "org.scalacheck" %% "scalacheck" % "1.14.1" % "test"
	),
    mainClass in assembly := Some("core.MeanderingDepths"),
    assemblyJarName in assembly := "MeanderingDepths.jar",
    test in assembly := {},
    assemblyMergeStrategy in assembly := {
        case PathList("META-INF", xs @ _*) => MergeStrategy.discard
        case x => MergeStrategy.first
    }


  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
