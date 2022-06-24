lazy val `quill-example` = project
  .in(file("."))
  .settings(
    name := "quill-example",
    version := "0.1.0",
    resolvers := Seq(
      Resolver.mavenLocal//,
      //"Sonatype OSS Snapshots" at "https://oss.sonatype.org/service/local/repositories/snapshots/content",
      //"Sonatype OSS Releases" at "https://oss.sonatype.org/service/local/repositories/releases/content"
    ),
    scalaVersion := "2.12.5",
//    scalacOptions ++= Seq(
//      "-language:implicitConversions",
//    ),
    libraryDependencies ++= Seq(
      "io.getquill" %% "quill-sql" % "3.16.3",
      "io.getquill" %% "quill-spark" % "3.16.3",
      "io.getquill" %% "quill-jdbc-zio" % "3.16.3",
      "org.scalatest" %% "scalatest" % "3.2.9" % "test",
      "org.scalatest" %% "scalatest-mustmatchers" % "3.2.9" % "test",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.2",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.13.2"

    )
  )
