package jsy.student

import jsy.lab4.Lab4Like

object Lab4 extends jsy.util.JsyApplication with Lab4Like {
  import jsy.lab4.ast._
  import jsy.lab4.Parser
  
  /*
   * CSCI 3155: Lab 4
   * Kyle Gronberg
   * 
   * Partner: <Your Partner's Name>
   * Collaborators: <Any Collaborators>
   */

  /*
   * Fill in the appropriate portions above by replacing things delimited
   * by '<'... '>'.
   * 
   * Replace the '???' expression with your code in each function.
   *
   * Do not make other modifications to this template, such as
   * - adding "extends App" or "extends Application" to your Lab object,
   * - adding a "main" method, and
   * - leaving any failing asserts.
   *
   * Your lab will not be graded if it does not compile.
   *
   * This template compiles without error. Before you submit comment out any
   * code that does not compile or causes a failing assert. Simply put in a
   * '???' as needed to get something that compiles without error. The '???'
   * is a Scala expression that throws the exception scala.NotImplementedError.
   */
  
  /* Collections and Higher-Order Functions */
  
  /* Lists */
  
  def compressRec[A](l: List[A]): List[A] = l match {
    case Nil | _ :: Nil => l
    case h1 :: (t1 @ (h2 :: _)) => {
      val x = compressRec(t1)
      if (h1 == h2) x else  h1 :: x
    }
  }
  
  def compressFold[A](l: List[A]): List[A] = l.foldRight(Nil: List[A]){
    (h, acc) => acc match {
      case Nil => h :: acc
      case h1 :: _ => if (h == h1) acc else h :: acc
    }
  }
  
  def mapFirst[A](l: List[A])(f: A => Option[A]): List[A] = l match {
    case Nil => l
    case h :: t => f(h) match {
      case Some(thing) => thing :: t
      case None => h :: mapFirst(t)(f)
    }
  }
  
  /* Trees */

  def foldLeft[A](t: Tree)(z: A)(f: (A, Int) => A): A = {
    def loop(acc: A, t: Tree): A = t match {
      case Empty => acc
      case Node(l, d, r) => loop(f(loop(acc, l),d),r)
    }
    loop(z, t)
  }

  // An example use of foldLeft
  def sum(t: Tree): Int = foldLeft(t)(0){ (acc, d) => acc + d }

  // Create a tree from a list. An example use of the
  // List.foldLeft method.
  def treeFromList(l: List[Int]): Tree =
    l.foldLeft(Empty: Tree){ (acc, i) => acc insert i }

  def strictlyOrdered(t: Tree): Boolean = {
    val (b, _) = foldLeft(t)((true, None: Option[Int])){
      (acc, h) => acc match {
        case (_, None) => (true, Some(h))
        case (v, Some(x)) => if(h <= x) (false, Some(x)) else (v, Some(x))
      }
    }
    b
  }

  /* Type Inference */

  // While this helper function is completely given, this function is
  // worth studying to see how library methods are used.
  def hasFunctionTyp(t: Typ): Boolean = t match {
    case TFunction(_, _) => true
    case TObj(fields) if (fields exists { case (_, t) => hasFunctionTyp(t) }) => true
    case _ => false
  }
  
  def typeof(env: TEnv, e: Expr): Typ = {
    def err[T](tgot: Typ, e1: Expr): T = throw StaticTypeError(tgot, e1, e)

    e match {
      case Print(e1) => typeof(env, e1); TUndefined
      case N(_) => TNumber
      case B(_) => TBool
      case Undefined => TUndefined
      case S(_) => TString
      case Var(x) => lookup(env, x)
      case Decl(mode, x, e1, e2) => typeof(extend(env, x, typeof(env, e1)), e2)
      case Unary(Neg, e1) => typeof(env, e1) match {
        case TNumber => TNumber
        case tgot => err(tgot, e1)
      }
      case Unary(Not, e1) => typeof(env, e1) match {
        case TBool => TBool
        case tgot => err(tgot, e1)
      }
      case Binary(Plus, e1, e2) => (typeof(env, e1), typeof(env, e2)) match {
        case (TNumber, TNumber) => TNumber
        case (TString, TString) => TString
        case (TNumber, tgot) => err(tgot, e2)
        case (TString, tgot) => err(tgot, e2)
        case (tgot, TNumber) => err(tgot, e1)
        case (tgot, TString) => err(tgot, e1)
      }
      case Binary(Minus | Times | Div, e1, e2) => (typeof(env, e1), typeof(env, e2)) match {
        case (TNumber, TNumber) => TNumber
        case (TNumber, tgot) => err(tgot, e2)
        case (tgot, _) => err(tgot, e1)
      }
      case Binary(Eq | Ne, e1, e2) => (typeof(env, e1), typeof(env, e2)) match {
        case (TFunction(_, _), _) => err(typeof(env, e1), e1)
        case (_, TFunction(_, _)) => err(typeof(env, e2), e2)
        case (t1, t2) if t1 == t2 => t1
        case (tgot1, _) => err(tgot1, e1)
      }
      case Binary(Lt | Le | Gt | Ge, e1, e2) => (typeof(env, e1), typeof(env, e2)) match {
        case (TNumber, TNumber) => TBool
        case (TString, TString) => TBool
        case (TNumber, tgot) => err(tgot, e2)
        case (TString, tgot) => err(tgot, e2)
        case (tgot, TNumber) => err(tgot, e1)
        case (tgot, TString) => err(tgot, e1)
      }
      case Binary(And | Or, e1, e2) => (typeof(env, e1), typeof(env, e2)) match {
        case (TBool, TBool) => TBool
        case (TBool, tgot) => err(tgot, e2)
        case (tgot, TBool) => err(tgot, e1)
      }
      case Binary(Seq, e1, e2) => typeof(env, e1); typeof(env, e2)
      case If(e1, e2, e3) => typeof(env, e1) match {
        case TBool => {
          if (typeof(env, e2) == typeof(env, e3)) typeof(env, e2) else err(typeof(env, e3), e3)
        }
        case tgot => err(tgot, e1)
      }
      case Function(p, params, tann, e1) => {
        // Bind to env1 an environment that extends env with an appropriate binding if
        // the function is potentially recursive.
        val env1 = (p, tann) match {
          case (Some(func), Some(retTyp)) => {
            val tFunc = TFunction(params, retTyp)
            extend(env, func, tFunc)
          }
          case (None, _) => env
          case _ => err(TUndefined, e1)
        }
        // Bind to env2 an environment that extends env1 with bindings for params.
        val env2 = params.foldLeft(env1) {
          case (acc, (x, mt)) => acc + (x -> mt.t)
        }

        // Infer the type of the function body
        val t1 = tann match {
          case Some(retTyp) => {
            val ret = typeof(env2, e1)
            if (ret == retTyp) ret else err(ret, e1)
          }
          case None => typeof(env2, e1)
        }
        TFunction(params,t1)
      }
      case Call(e1, args) => typeof(env, e1) match {
        case TFunction(params, tret) if (params.length == args.length) =>
          (params zip args).foreach {
            case((_,MTyp(_,ti)),arg) => {
              val argt = typeof(env,arg)
              if (ti != argt) err(argt,e1)
            }
          };
          tret
        case tgot => err(tgot, e1)
      }
      case Obj(fields) => {
        val tfields = fields.mapValues {
          case(exp) => typeof(env,exp)
        }
        TObj(tfields)
      }

      case GetField(e1, f) => (e1.getClass.getDeclaredField(f)) match {
        case (field) => typeof(env,field.get(e1).asInstanceOf[Expr])
        }

    }
  }
  
  
  /* Small-Step Interpreter */
  /*
   * Helper function that implements the semantics of inequality
   * operators Lt, Le, Gt, and Ge on values.
   *
   * We suggest a refactoring of code from Lab 2 to be able to
   * use this helper function in eval and step.
   *
   * This should the same code as from Lab 3.
   */
  def inequalityVal(bop: Bop, v1: Expr, v2: Expr): Boolean = {
    require(isValue(v1), s"inequalityVal: v1 ${v1} is not a value")
    require(isValue(v2), s"inequalityVal: v2 ${v2} is not a value")
    require(bop == Lt || bop == Le || bop == Gt || bop == Ge)
    require(bop == Lt || bop == Le || bop == Gt || bop == Ge)
    (v1, v2) match {
      case (S(s1), S(s2)) => bop match {
        case Lt => s1 < s2
        case Le => s1 <= s2
        case Gt => s1 > s2
        case Ge => s1 >= s2
      }
      case (N(n1), N(n2)) => bop match {
        case Lt => n1 < n2
        case Le => n1 <= n2
        case Gt => n1 > n2
        case Ge => n1 >= n2
      }
    }
  }

  /* This should be the same code as from Lab 3 */
  def iterate(e0: Expr)(next: (Expr, Int) => Option[Expr]): Expr = {
    def loop(e: Expr, n: Int): Expr = next(e, n) match {
      case None => e
      case Some(e1) => loop(e1, n+1)
    }
    loop(e0, 0)
  }

  /* Capture-avoiding substitution in e replacing variables x with esub. */
  def substitute(e: Expr, esub: Expr, x: String): Expr = {
    def subst(e: Expr): Expr = e match {
      case N(_) | B(_) | Undefined | S(_) => e
      case Print(e1) => Print(substitute(e1, esub, x))
        /***** Cases from Lab 3 */
      case Unary(uop, e1) => Unary(uop,substitute(e1,esub,x))
      case Binary(bop, e1, e2) => Binary(bop,substitute(e1,esub,x),substitute(e2,esub,x))
      case If(e1, e2, e3) => If(substitute(e1,esub,x),substitute(e2,esub,x),substitute(e3,esub,x))
      case Var(y) => if (x == y) esub else Var(y)
      case Decl(mode, y, e1, e2) => {
        if (x == y) Decl(mode, y, substitute(e1, esub, x), e2)
        else Decl(mode, y, substitute(e1, esub, x), substitute(e2, esub, x))
      }
        /***** Cases needing adapting from Lab 3 */
      case Function(p, params, tann, e1) =>
        ???
      case Call(e1, args) => Call(subst(e1), args.map{arg => subst(arg)})
        /***** New cases for Lab 4 */
      case Obj(fields) => Obj(fields.map{case (field,exp) => (field, subst(exp))})
      case GetField(e1, f) => GetField(subst(e1), f)
    }

    val fvs = freeVars(e)
    def fresh(x: String): String = if (fvs.contains(x)) fresh(x + "$") else x
    subst(???)
  }

  /* Rename bound variables in e */
  def rename(e: Expr)(fresh: String => String): Expr = {
    def ren(env: Map[String,String], e: Expr): Expr = {
      e match {
        case N(_) | B(_) | Undefined | S(_) => e
        case Print(e1) => Print(ren(env, e1))

        case Unary(uop, e1) => ???
        case Binary(bop, e1, e2) => ???
        case If(e1, e2, e3) => ???

        case Var(y) =>
          ???
        case Decl(mode, y, e1, e2) =>
          val yp = fresh(y)
          ???

        case Function(p, params, retty, e1) => {
          val (pp, envp): (Option[String], Map[String,String]) = p match {
            case None => ???
            case Some(x) => ???
          }
          val (paramsp, envpp) = params.foldRight( (Nil: List[(String,MTyp)], envp) ) {
            ???
          }
          ???
        }

        case Call(e1, args) => ???

        case Obj(fields) => ???
        case GetField(e1, f) => ???
      }
    }
    ren(empty, e)
  }

  /* Check whether or not an expression is reduced enough to be applied given a mode. */
  def isRedex(mode: Mode, e: Expr): Boolean = mode match {
    case MConst => isValue(e)
    case MName => true
  }

  def step(e: Expr): Expr = {
    require(!isValue(e), s"step: e ${e} to step is a value")
    e match {
      /* Base Cases: Do Rules */
      case Print(v1) if isValue(v1) => println(pretty(v1)); Undefined
        /***** Cases needing adapting from Lab 3. */
      case Unary(Neg, N(n1)) => N(-n1)
      case Unary(Not, B(b1)) => B(!b1)

      case Binary(Seq, v1, e2) if isValue(v1) => e2

      case Binary(And, B(b1), e2) => if (b1) e2 else B(true)
      case Binary(Or, B(b1), e2) => if (b1) B(true) else e2

      case Binary(bop @ (Plus|Minus|Times|Div), N(n1), N(n2)) => bop match {
        case Plus => N(n1 + n2)
        case Minus => N(n1 - n2)
        case Times => N(n1 * n2)
        case Div => N(n1 / n2)
      }
      case Binary(Plus, S(s1), S(s2)) => S(s1+s2)

      case Binary(bop @ (Lt|Le|Gt|Ge), N(n1), N(n2)) => B(inequalityVal(bop, N(n1), N(n2)))
      case Binary(bop @ (Lt|Le|Gt|Ge), S(s1), S(s2)) => B(inequalityVal(bop, S(s1), S(s2)))

      case Binary(bop @ (Eq|Ne), v1, v2) if isValue(v1) && isValue(v2) => bop match {
        case Eq => B(v1 == v2)
        case Ne => B(v1 != v2)
      }
      case If(B(b1), e2, e3) => if (b1) e2 else e3

      case Decl(mode, x, e1, e2) if isRedex(mode, e1) => substitute(e2, e1, x)
      /***** More cases here */
      case Call(v1, args) if isValue(v1) =>
        v1 match {
          case Function(p, params, _, e1) => {
            val pazip = params zip args
            if (???) {
              val e1p = pazip.foldRight(e1) {
                ???
              }
              p match {
                case None => ???
                case Some(x1) => ???
              }
            }
            else {
              val pazipp = mapFirst(pazip) {
                ???
              }
              ???
            }
          }
          case _ => throw StuckError(e)
        }
        /***** New cases for Lab 4. */

      /* Inductive Cases: Search Rules */
      case Print(e1) => Print(step(e1))
        /***** Cases from Lab 3. */
      case Unary(uop, e1) => Unary(uop, step(e1))

      case Binary(bop, v1, e2) if isValue(v1) => Binary(bop, v1, step(e2))
      case Binary(bop, e1, e2) => Binary(bop, step(e1), e2)

      case If(e1, e2, e3) => If(step(e1), e2, e3)

      case Decl(mode, x, e1, e2) => Decl(mode, x, step(e1), e2)

      case Call(e1, args) => Call(step(e1), args)
        /***** Cases needing adapting from Lab 3 */
      //case Call(v1 @ Function(_, _, _, _), args) => Call(v1, step(args))
        /***** New cases for Lab 4. */

      /* Everything else is a stuck error. Should not happen if e is well-typed.
       *
       * Tip: you might want to first develop by comment out the following line to see which
       * cases you have missing. You then uncomment this line when you are sure all the cases
       * that you have left the ones that should be stuck.
       */
      case _ => throw StuckError(e)
    }
  }
  
  
  /* External Interfaces */
  
  //this.debug = true // uncomment this if you want to print debugging information
  this.keepGoing = true // comment this out if you want to stop at first exception when processing a file
}

