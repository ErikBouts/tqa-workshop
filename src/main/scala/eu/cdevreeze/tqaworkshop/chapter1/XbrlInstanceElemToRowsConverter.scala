/*
 * Copyright 2016-2017 Chris de Vreeze
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cdevreeze.tqaworkshop.chapter1

import scala.collection.immutable

import eu.cdevreeze.yaidom.queryapi.BackingElemApi
import eu.cdevreeze.yaidom.core.EName

/**
 * This class converts an XBRL instance to a collection of rows. The input instance is passed as a
 * so-called yaidom `BackingElemApi`. This is a yaidom query API trait, combining many traits that each
 * offer a part of the yaidom query API. The yaidom query API traits abstract from the concrete
 * element implementation, and indeed multiple element implementations exist that offer the `BackingElemApi`
 * query API.
 *
 * This class shows multiple yaidom query API methods in action. The 3 most central query API methods
 * are:
 * <ul>
 * <li>`filterChildElems`</li>
 * <li>`filterElems`</li>
 * <li>`filterElemsOrSelf</li>
 * </ul>
 * These methods are higher-order functions, taking an element predicate as parameter. They correspond
 * to the child element, descendant element and descendant-or-self element "axes". This is analogous
 * to the child, descendant and descendant-or-self XPath axes, except that in these filtering yaidom methods
 * only elements are returned, and not arbitrary nodes.
 *
 * Equivalent XPath may be shorter, but the yaidom queries have familiar semantics for Scala developers.
 * For example, equality comparisons are Scala equality comparisons, which differ substantially from the
 * XPath comparison operators.
 *
 * For more on '''yaidom''' and its comparison to for example '''XPath''', see
 * [[http://dvreeze.github.io/yaidom-queries.html]] and [[http://dvreeze.github.io/yaidom-and-namespaces.html]].
 *
 * @author Chris de Vreeze
 */
final class XbrlInstanceElemToRowsConverter {

  import XbrlInstanceElemToRowsConverter.Row

  def convertXbrlInstance(instanceElem: BackingElemApi): immutable.IndexedSeq[Row] = {
    ???
  }
}

object XbrlInstanceElemToRowsConverter {

  final case class Row(val conceptName: EName)
}
