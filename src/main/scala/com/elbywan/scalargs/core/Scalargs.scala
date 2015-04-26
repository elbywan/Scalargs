package com.elbywan.scalargs.core

import scala.annotation.tailrec
import scala.reflect.runtime.universe._

object Scalargs {
    //todo : multiple times same param (boundaries : min/max) + collect?

    /**
     * Creates a new argument configuration with the specified long argument name (--[XXX]).
     * @param name Long name
     * @return A newly instantiated Argument configuration object.
     */
    def argument(name: String) = {
        if(name.length < 1)
            throw new IllegalArgumentException("Argument length < 1")
        Argument[String](Some(name))
    }

    /**
     * Creates a new argument configuration with the specified short argument name (-[XXX]).
     * @param name Short name
     * @return A newly instantiated Argument configuration object.
     */
    def shortArgument(name: String) = {
        if(name.length < 1)
            throw new IllegalArgumentException("Argument length < 1")
        Argument[String](shortName = Some(name))
    }

    /**
     * Creates a new program configuration with given name.
     * @param name Program name
     * @return A newly instantiated Program configuration object.
     */
    def program(name: String) = {
        if(name.length < 1)
            throw new IllegalArgumentException("Empty program name")
        Program(name)
    }
}

/**
 * Program configuration.
 *
 * @define RETURN A newly cloned and set Program object
 * @define CREATE Creates a new cloned Program
 *
 * @param name Program name
 * @param titleDesc Short description, used in the help text
 * @param longDesc Long description, used in the help text
 * @param args Argument configurations
 */
/* Full syntax
  -------------
Program("Program name") [title "Title short text"] [usage "Usage text"] arguments (
  argument("argument name") [empty|as[TYPE]{String => TYPE}] [mandatory|optional] [short "short argument name"] [description "Description or usage] [perform { value => action }],
  shortArgument("short argument name") [empty|as[TYPE]] [mandatory|optional] [name "argument name"] [description "Description or usage] [perform { value => action }]
  (Repeat as desired ...............................................................................................................................................)
) from {ARGS_VARIABLE}
*/
case class Program(name: String, titleDesc: String = "", longDesc: String = "", args: Array[Argument[_]] = Array()){
    /**
     * Sets the help title text.
     * @param description Title text
     * @return $RETURN
     */
    def title(description: String) = {
        Program(name, description, longDesc, args)
    }

    /**
     * Sets the program usage or long description.
     * @param description Usage text
     * @return $RETURN
     */
    def usage(description: String) = {
        Program(name, titleDesc, description, args)
    }

    /**
     * Provides the Argument configurations.
     * @param args Argument configurations.
     * @return $RETURN
     */
    def arguments(args: Argument[_]*) = {
        Program(name, titleDesc, longDesc, args.toArray)
    }

    private def printHelp() = {
        def line(ascii: String, str: String, prefix: String = ""): String = {
            str + "\n" + prefix + ascii * str.length
        }

        def formatArgs(args: Array[Argument[_]]) = {
            args.foldLeft(""){
                (acc : String, arg : Argument[_]) => acc + "\n" + formatArg(arg)
            }
        }
        def formatArg[T](arg: Argument[T]) = {
            arg.name.map{name => "--"+name+" "}.getOrElse("") +
                arg.shortName.map{name => "-"+name+" "}.getOrElse("") +
                { if(!arg.isEmpty) "<value of type [" + arg.getType + "]>" else "" } +
                "\n\t" + arg.description
        }

        val mandatories = args filter { arg => arg.mandat }
        val optionals = args filterNot { arg => arg.mandat }

        //Title
        println(s"""${line("-", name + " " + titleDesc)}""")
        //Usage or long description
        if(!longDesc.isEmpty)
            println("\n"+longDesc)
        //Mandatory arguments
        if(mandatories.length > 0){
            println("\n = Mandatory =")
            println(formatArgs(mandatories))
        }
        //Optional arguments
        if(optionals.length > 0) {
            println("\n = Optional =")
            println(formatArgs(optionals))
        }
    }

    /**
     * Launches the parsing form the String argument array passed as a parameter.
     * @param strArgs Argument array.
     * @return An argument Map of the form [Name -> Value]
     */
    def from(strArgs: Array[String]) = {
        val mandatories = args filter { arg => arg.mandat }

        def matchArg(args: Array[Argument[_]], strArg: String) = {
            args.find { arg =>
                arg.name.exists {
                    strArg equals "--" + _
                } ||
                arg.shortName.exists {
                    strArg equals "-" + _
                }
            }
        }

        val validateMandatories = {
            @tailrec
            def loop(li: List[String], acc: Array[Argument[_]]): Array[Argument[_]] = {
                li match {
                    case item :: next :: tail => {
                        val arg = matchArg(args, item)
                        if(arg.isDefined){}
                        if (arg.exists(!_.isEmpty()))
                            loop(tail, acc filter {
                                !_.equals(arg.get)
                            })
                        else
                            loop(next :: tail, acc filter {
                                !_.equals(arg.getOrElse(null))
                            })
                    }
                    case item :: tail => {
                        val arg = matchArg(args, item)
                        if (arg.exists(!_.isEmpty()))
                            loop(tail, acc)
                        else
                            loop(tail, acc filter {
                                !_.equals(arg.getOrElse(null))
                            })
                    }
                    case Nil => acc
                }
            }
            loop(strArgs.toList, mandatories) isEmpty
        }
        val helpCheck = strArgs.contains("-h") || strArgs.contains("--help")

        def applyAction[T](arg: Argument[T], param: Any) = {
            arg.action(param.asInstanceOf[T])
        }

        @tailrec
        def parseArgs(arr: List[String], accumulator: Map[String, Any]): Map[String, Any] = {
            arr match {
                case item :: next :: tail => {
                    matchArg(args, item) match {
                        case None =>
                            parseArgs(next :: tail, accumulator)
                        case Some(argumentObj) =>
                            if (argumentObj.isEmpty()) {
                                applyAction(argumentObj, scala.runtime.BoxedUnit.UNIT)
                                parseArgs(next :: tail, accumulator + (argumentObj.getName -> ""))
                            } else {
                                applyAction(argumentObj, argumentObj.convert(next))
                                parseArgs(tail, accumulator + (argumentObj.getName -> argumentObj.convert(next)))
                            }
                    }
                }
                case item :: tail => {
                    matchArg(args, item) match {
                        case None =>
                            parseArgs(tail, accumulator)
                        case Some(argumentObj) =>
                            if (argumentObj.isEmpty()) {
                                applyAction(argumentObj, scala.runtime.BoxedUnit.UNIT)
                                parseArgs(tail, accumulator + (argumentObj.getName -> ""))
                            } else
                                parseArgs(tail, accumulator)
                    }
                }
                case Nil => accumulator
            }
        }

        if (helpCheck || !validateMandatories){
            printHelp()
            if(!helpCheck)
                throw new IllegalArgumentException("Argument configuration format violated.")
            Map[String, Any]()
        } else
            parseArgs(strArgs.toList, Map())
    }
}

/**
 * @define RETURN A newly cloned and set Argument object
 * @define CREATE Creates a new cloned argument
 *
 * This class is used to initiate an argument configuration before parsing the raw list.
 *
 * @param name Long name [--name]
 * @param shortName Short name [--name]
 * @param description Description
 * @param mandat True if mandatory
 * @param action Action to perform when the argument is parsed
 * @tparam T Type of the value associated with the argument
 */
sealed case class Argument[T: TypeTag](name: Option[String] = None, shortName: Option[String] = None,
                                       description: String = "", mandat: Boolean = false,
                                       action: (T => Unit) = { _ : T => () },
                                       convert: (String => T) = { (_:String).asInstanceOf[T] }){

    /**
     * $CREATE with a long argument name set ( format: --[XXX] )
     * @param longName Long name
     * @return $RETURN
     */
    def long(longName: String) = {
        Argument[T](Some(longName), shortName, description, mandat, action, convert)
    }

    /**
     * $CREATE with a short argument name set ( format: -[XXX] )
     * @param shortName Short name
     * @return $RETURN
     */
    def short(shortName: String) = {
        Argument[T](name, Some(shortName), description, mandat, action, convert)
    }

    /**
     * $CREATE with a description, used in the printed help.
     * @param description Description or usage
     * @return $RETURN
     */
    def description(description: String) = {
        Argument[T](name, shortName, description, mandat, action, convert)
    }

    /**
     * $CREATE which will not expect a value.
     * @return $RETURN
     */
    def empty() = {
        Argument[Unit](name, shortName, description, mandat)
    }

    /**
     * Checks whether the argument will expect a value.
     * @return True if a value is expected
     */
    def isEmpty() = {
        getType.=:=(typeOf[Unit])
    }

    /**
     * $CREATE  which will expect a value of a certain type as the next command line argument.
     * @param convert Conversion function
     * @tparam J Type of the value expected
     * @return $RETURN
     */
    def as[J: TypeTag](convert: String => J) = {
        Argument[J](name, shortName, description, mandat, {_ : J => ()}, convert)
    }

    /**
     * Returns the expected value type.
     * @return Type of the value expected
     */
    def getType() = {
        typeOf[T]
    }

    /**
     * $CREATE which is mandatory (the program will exit if the argument is not found)
     * @return $RETURN
     */
    def mandatory() = {
        Argument[T](name, shortName, description, true, action, convert)
    }

    /**
     * $CREATE which is optional (should be the default)
     * @return $RETURN
     */
    def optional() = {
        Argument[T](name, shortName, description, false, action, convert)
    }

    /**
     * Perform an action when the argument is found.
     * @param action Function taking the value associated to the argument as a parameter.
     * @return Nothing special
     */
    def perform(action: T => Unit) = {
        Argument[T](name, shortName, description, mandat, action, convert)
    }
    def apply(action: T => Unit) = this.perform(action: T => Unit)

    /**
     * Returns the long argument name if configured, otherwise returns the short name.
     */
    def getName() = {
        name.getOrElse(shortName.get)
    }

}
