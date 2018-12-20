name := "AkkaNodeCommunication"
version := "1.0"
scalaVersion := "2.10.1"


libraryDependencies <++= (scalaVersion)(sv =>
  Seq(
    "org.scala-lang" % "scala-reflect" % "2.10.1",
    "org.scala-lang" % "scala-compiler" % "2.10.1"
  )
)