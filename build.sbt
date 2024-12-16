ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.15"

lazy val root = (project in file("."))
  .settings(
    name := "bedrock-lambda",
    libraryDependencies ++= Seq(
      // AWS SDK for Bedrock
      "software.amazon.awssdk" % "bedrockruntime" % "2.25.52", // Replace with the latest version

// AWS Lambda Core for the Lambda handler
      "com.amazonaws" % "aws-lambda-java-core" % "1.2.3",

      // AWS Lambda Events (Optional, if handling AWS-specific event types)
      "com.amazonaws" % "aws-lambda-java-events" % "3.12.0",

      // JSON library for handling JSON responses
      "org.json" % "json" % "20230227",

      // Scala Java Converters
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.12.0", // For Java-Scala collection conversion,

      // Jackson Databind (for ObjectMapper)
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.17.0",

      // Jackson Module for Scala (for Scala type support in Jackson)
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.17.2",

      "org.slf4j" % "slf4j-api" % "2.0.12",               // SLF4J 2.x for logging
      "ch.qos.logback" % "logback-classic" % "1.5.6",
      "com.typesafe" % "config" % "1.4.3",

      "org.scalatest" %% "scalatest" % "3.2.19" % Test,        // ScalaTest for AnyFlatSpec
      "org.scalatestplus" %% "mockito-4-6" % "3.2.15.0" % Test, // ScalaTest Mockito integration for MockitoSugar

    ),

    fork := true,
    javaOptions += "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED",

    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) =>
        xs match {
          case "MANIFEST.MF" :: Nil     => MergeStrategy.discard
          case "services" :: _          => MergeStrategy.concat
          case _                        => MergeStrategy.discard
        }
      case "reference.conf"            => MergeStrategy.concat
      case x if x.endsWith(".proto")   => MergeStrategy.rename
      case _                           => MergeStrategy.first
    }

  )
