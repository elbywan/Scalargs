package com.elbywan.scalargs.tests

import com.elbywan.scalargs.core.Scalargs._
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class ArgsTest extends FlatSpec with Matchers {

    //Extensive program configuration
    val programConfiguration = program("Extensive conf") title "- Title" usage "Usage text" arguments (
        argument("name") description "Your first or last name." mandatory() perform { x => println("Hello " + x+ "!") },
        argument("age").as[Int]{_.toInt} short "a" description "Your age." mandatory() perform {x: Int => println(x)},
        argument("greet") empty() description "Receive a special greeting !" perform { _ => println("Greetings !!") },
        shortArgument("x") empty() optional
    )

    //OK args
    val validLongArgs = Array("--greet", "--name", "TOTO", "--age", "20")
    val validShortArgs = Array("-x", "-a", "20", "--name", "TOTO")

    //Invalid args
    val invalidShortArgs = Array("--x", "-name", "TOTO", "-a", "10", "-greet")
    val invalidMissingValue = Array("--name", "-a", "10")
    val invalidValueType = Array("--name", "TOTO", "-a", "bla")
    val invalidMandatory = Array("-n", "TOTO", "--greet")
    val invalidEmpty = Array("")

    //Help arg
    val shortHelpArg = Array("-h")
    val longHelpArg = Array("--help")

    it should "Reject empty configurations" in {
        an [IllegalArgumentException] should be thrownBy {
            program("")
        }
        an [IllegalArgumentException] should be thrownBy {
            argument("")
        }
        an [IllegalArgumentException] should be thrownBy {
            shortArgument("")
        }
    }

    it should "[Extensive conf.] Validate and parse long arguments properly" in {
        val outputStream = new java.io.ByteArrayOutputStream()

        var arguments: Map[String, Any] = Map()
        Console.withOut(outputStream) {
            arguments = programConfiguration from validLongArgs
        }

        arguments("name") should equal ("TOTO")
        arguments("age") should equal (20)
        arguments("greet") should equal ("")
        arguments should not contain ("x")

        outputStream.toString should equal(
            """Greetings !!
              |Hello TOTO!
              |20
              |""".stripMargin)
    }

    it should "[Extensive conf.] Validate and parse short arguments properly" in {
        val outputStream = new java.io.ByteArrayOutputStream()

        var arguments: Map[String, Any] = Map()
        Console.withOut(outputStream) {
            arguments = programConfiguration from validShortArgs
        }

        arguments("name") should equal ("TOTO")
        arguments("age")  should equal (20)
        arguments("x") should equal ("")
        arguments should not contain ("greet")

        outputStream.toString should equal(
            """20
              |Hello TOTO!
              |""".stripMargin)
    }

    it should "[Extensive conf.] Invalidate bad argument entries" in {
        val nullStream = new java.io.ByteArrayOutputStream()

        Console.withOut(nullStream) {
            var arguments: Map[String, Any] = Map()
            an[IllegalArgumentException] should be thrownBy {
                arguments = programConfiguration from invalidEmpty
            }
            an[IllegalArgumentException] should be thrownBy {
                arguments = programConfiguration from invalidMandatory
            }
            an[IllegalArgumentException] should be thrownBy {
                arguments = programConfiguration from invalidShortArgs
            }
            an[IllegalArgumentException] should be thrownBy {
                arguments = programConfiguration from invalidMissingValue
            }
            an[IllegalArgumentException] should be thrownBy {
                arguments = programConfiguration from invalidValueType
            }
        }
    }

    it should "automatically generate and print help text" in {
        val outputStreamLong = new java.io.ByteArrayOutputStream()
        val outputStreamShort = new java.io.ByteArrayOutputStream()
        Console.withOut(outputStreamLong) {
            programConfiguration from longHelpArg
        }
        Console.withOut(outputStreamShort) {
            programConfiguration from shortHelpArg
        }

        outputStreamLong.toString should equal (outputStreamShort.toString)
        outputStreamLong.toString should equal (
            s"""Extensive conf - Title
              |----------------------
              |
              |Usage text
              |
              | = Mandatory =
              |
              |--name <value of type [String]>
              |${"\t"}Your first or last name.
              |--age -a <value of type [Int]>
              |${"\t"}Your age.
              |
              | = Optional =
              |
              |--greet${" "}
              |${"\t"}Receive a special greeting !
              |-x${" "}
              |${"\t"}
              |""".stripMargin)
    }

    val minimalConfiguration = program("Minimal conf") arguments argument("argument")

    it should "[Minimal conf.] Validate minimal config." in {
        (minimalConfiguration from Array("--argument", "this is the argument's value"))("argument") should equal {
            "this is the argument's value"
        }
    }

    it should "[Minimal conf.] Print minimal help" in {
        val outputStream = new java.io.ByteArrayOutputStream()
        Console.withOut(outputStream) {
            minimalConfiguration from Array("-h")
        }
        outputStream.toString should equal {
            s"""Minimal conf${" "}
              |-------------
              |
              | = Optional =
              |
              |--argument <value of type [String]>
              |${"\t"}
              |""".stripMargin
        }
    }
}
