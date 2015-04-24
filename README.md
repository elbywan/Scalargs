| Scalargs | [![Build Status](https://travis-ci.org/elbywan/Scalargs.svg?branch=master)](https://travis-ci.org/elbywan/xtended-xml) | [![Coverage Status](https://coveralls.io/repos/elbywan/Scalargs/badge.svg)](https://coveralls.io/r/elbywan/Scalargs)
===============

##Wtf ?

**Scalargs** is a scala command line interface arguments parser, with a very simple DSL.
It automatically validates or rejects arguments given a configuration you specify (you can specify values of a given type, mandatory arguments ...)

##Installation

Get it with sbt :

```scala
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies += "com.github.elbywan" %% "scalargs" % "0.1-SNAPSHOT"
```

To play around with the library, type `sbt console`.

##Usage

- Specify a configuration using the DSL
- Help is automatically generated and printed if the user pass -h or --help as an argument, or if the arguments are not validated
- Parse the arguments as a scala map

>todo : detailed usage

In the meantime, you can check the tests folder for some code samples.

###Import

```scala
import com.elbywan.scalargs.core.Scalargs._
```

###DSL

*Argument syntax*
- Short argument : -s
- Long argument : --long

*Full DSL syntax*
```scala
Program("Program name") [title "Title short text"] [usage "Usage text"] arguments (
  argument("argument name") [empty|as[TYPE]{String => TYPE}] [mandatory|optional] [short "short argument name"] [description "Description or usage"] [perform { value => action }],
  shortArgument("short argument name") [empty|as[TYPE]{String => TYPE}] [mandatory|optional] [name "long argument name"] [description "Description or usage"] [perform { value => action }]
  (... Repeat as desired ...)
) from {ARGUMENTS_ARRAY_VARIABLE}
```

*Example*
```scala
val prog = program("Program name") title "- Just a demo" usage "Pass some arguments and watch the results" arguments (
    argument("name") description "Your first or last name." mandatory() perform { x => println("Hello " + x+ "!") },
    argument("age").as[Int]{_.toInt} short "a" description "Your age." mandatory() perform {x: Int => println(x)},
    argument("greet") empty() description "Receive a special greeting." perform { _ => println("Hey dude !!") }
)

val args = prog from Array("--name", "Igor", "-a", "25")
val name = args("name").getOrElse("")
val age = args("age").getOrElse(0)
println(s"Hey $name, are you really $age years old ?")

prog from Array("--help")

/* Automatically generated help *

Program name - Just a demo
--------------------------

Pass some arguments and watch the results

 = Mandatory =

--name <value of type [String]>
	Your first or last name.
--age -a <value of type [Int]>
	Your age.

 = Optional =

--greet
	Receive a special greeting.
	
*/
```
