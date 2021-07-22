lazy val `protoquill-example` = project
  .in(file("."))
  .settings(
    name := "protoquill-example",
    version := "0.1.0",
    resolvers ++= Seq(
      Resolver.mavenLocal,
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/service/local/repositories/snapshots/content",
      "Sonatype OSS Releases" at "https://oss.sonatype.org/service/local/repositories/releases/content"
    ),
    scalaVersion := "3.0.0",
    scalacOptions ++= Seq(
      "-language:implicitConversions",
    ),
    libraryDependencies ++= Seq(
      "io.getquill" %% "quill-sql" % "top_level-SNAPSHOT"
    )
  )
