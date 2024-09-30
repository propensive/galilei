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

object Linux:
  object Root

  abstract class Root() extends serpentine.Root(t"/", t"/", Case.Sensitive):
    type Platform = Linux
  
  object RootSingleton extends Root()

  type Rules = MustNotContain["/"] & MustNotEqual["."] & MustNotEqual[".."] & MustNotEqual[""]

  given (using Tactic[PathError], Tactic[NameError]) => Linux is Navigable by Name[Linux] from
      Root under Rules as navigable =
    new Navigable:
      type Self = Linux
      type Operand = Name[Linux]
      type Source = Root
      type Constraint = Rules

      val separator: Text = t"/"
      val parentElement: Text = t".."
      val selfText: Text = t"."

      def element(element: Text): Name[Linux] = Name(element)
      def rootLength(path: Text): Int = 1
      def elementText(element: Name[Linux]): Text = element.text
      def rootText(root: Source): Text = t"/"
    
      def root(path: Text): Source =
        if path.at(Prim) == '/' then %
        else raise(PathError(PathError.Reason.InvalidRoot, path)) yet %
      
      def caseSensitivity: Case = Case.Sensitive

erased trait Linux extends Filesystem