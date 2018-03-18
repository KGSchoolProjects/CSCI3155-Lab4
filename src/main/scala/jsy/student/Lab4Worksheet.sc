/*
 * CSCI 3155: Lab 4 Worksheet
 *
 * This worksheet demonstrates how you could experiment
 * interactively with your implementations in Lab4.scala.
 */

// Imports the parse function from jsy.lab1.Parser
import jsy.lab4.Parser.parse

// Imports the ast nodes
import jsy.lab4.ast._

// Imports all of the functions form jsy.student.Lab2 (your implementations in Lab2.scala)
import jsy.student.Lab4._

// Try compressRec
//val cr1 = compressRec(List(1, 2, 2, 3, 3, 3))

// Parse functions with possibly multiple parameters and type annotations.
iterateStep(parse("function fst(x: number, y: number): number { return x }"))
iterateStep(parse("function (x: number) { return x }"))
iterateStep(parse("function (f: (y: number) => number, x: number) { return f(x) }"))
//parse("const factorial = function f(n : number) : number {if (n == 0) return 1}")
//iterateStep("Decl(MConst,factorial,Function(Some(f),List((n,MTyp(MConst,TNumber))),Some(TNumber),If(Binary(Eq,Var(n),N(0.0)),N(1.0),Binary(Times,Var(n),Call(Var(f),List(Binary(Minus,Var(n),N(1.0))))))))")
// Parse objects
parse("{ f: 0, g: true }")
parse("x.f")
