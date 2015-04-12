name := "Scalargs"
organization := "com.github.elbywan"
version := "0.1-SNAPSHOT"
scalaVersion := "2.11.6"

val SCALATEST_VERSION   = "2.2.1"

libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % "2.11.6",
    "org.scalatest" %% "scalatest" % SCALATEST_VERSION % Test
)

pomIncludeRepository := { _ => false }
pomExtra :=
    <url>https://github.com/elbywan/Scalargs</url>
    <licenses>
        <license>
            <name>GNU Gpl v3</name>
            <url>http://www.gnu.org/licenses/gpl.txt</url>
            <distribution>repo</distribution>
        </license>
    </licenses>
    <scm>
        <url>git@github.com:elbywan/scalargs.git</url>
        <connection>scm:git:git@github.com:elbywan/scalargs.git</connection>
    </scm>
    <developers>
        <developer>
            <id>elbywan</id>
            <name>Julien Elbaz</name>
            <url>https://github.com/elbywan</url>
        </developer>
    </developers>

publishArtifact in Test := false
publishMavenStyle := true
publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
    else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
