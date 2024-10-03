package galilei

import contingency.*
import prepositional.*
import rudiments.*
import nomenclature.*
import serpentine.*
import denominative.*
import gossamer.*
import vacuous.*
import anticipation.*

object Posix:
  abstract class Root() extends serpentine.Root(t"/", t"/", Case.Sensitive):
    type Platform = Posix
  
  object RootSingleton extends Root()

  type Rules = MustNotContain["/"] & MustNotEqual["."] & MustNotEqual[".."] & MustNotEqual[""]

  given (using Tactic[PathError], Tactic[NameError]) => Posix is Navigable by Name[Posix] from
      (Root on Posix) under Rules as navigable =
    new Navigable:
      type Self = Posix
      type Operand = Name[Posix]
      type Source = Root on Posix
      type Constraint = Rules

      val separator: Text = t"/"
      val parentElement: Text = t".."
      val selfText: Text = t"."

      def element(element: Text): Name[Posix] = Name(element)
      def rootLength(path: Text): Int = 1
      def elementText(element: Name[Posix]): Text = element.text
      def rootText(root: Source): Text = t"/"
    
      def root(path: Text): Source =
        if path.at(Prim) == '/' then Posix.RootSingleton
        else raise(PathError(PathError.Reason.InvalidRoot, path)) yet Posix.RootSingleton
      
      def caseSensitivity: Case = Case.Sensitive

erased trait Posix extends Filesystem