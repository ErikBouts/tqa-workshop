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

package eu.cdevreeze.tqaworkshop.util

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.queryapi.BackingElemApi

/**
 * Editor of BackingElemApi objects. It takes care of namespace prefix generation, if needed, and
 * it makes sure not to introduce any prefixed namespace undeclarations when adding child elements.
 * This enables the editor to think in terms of ENames rather than QNames.
 *
 * @author Chris de Vreeze
 */
trait BackingElemEditor {

  type N

  type E <: N with BackingElemApi.Aux[E]

  /**
   * Returns function that returns a prefix for any namespace. It should never return the empty
   * string (for the default namespace). Ideally, for any namespace the same prefix is always returned,
   * but it is also possible that a fresh prefix is generated.
   *
   * This function is only used for namespaces that are not in the "current" scope.
   */
  def getFallbackPrefixForNamespace: String => String

  /**
   * Functionally updates the resolved name. This sets the QName, and may affect the Scope.
   */
  def withResolvedName(thisElem: E, newEName: EName): E

  /**
   * Functionally updates the resolved attributes. This sets the attribute QNames, and may affect the Scope.
   */
  def withResolvedAttributes(thisElem: E, newResolvedAttributes: immutable.Iterable[(EName, String)]): E

  /**
   * Functionally updates the children. It is ensured by the implementation that no prefixed namespace undeclarations are
   * introduced. After all, they are illegal in XML 1.0.
   */
  def withChildren(thisElem: E, newChildren: immutable.IndexedSeq[N]): E

  /** Shorthand for `withChildren(thisElem, newChildSeqs.flatten)` */
  def withChildSeqs(thisElem: E, newChildSeqs: immutable.IndexedSeq[immutable.IndexedSeq[N]]): E

  /**
   * Functionally updates the resolved attributes by filtering. This filters the (QName-keyed) attributes.
   */
  def filteringResolvedAttributes(thisElem: E, p: (EName, String) => Boolean): E

  /**
   * Functionally adds a resolved attribute. This sets the attribute QName, and may affect the Scope.
   */
  def plusResolvedAttribute(thisElem: E, attrEName: EName, attrValue: String): E

  /**
   * Optionally functionally adds a resolved attribute. This sets the attribute QName, if applicable, and may affect the Scope.
   */
  def plusResolvedAttributeOption(thisElem: E, attrEName: EName, attrValueOption: Option[String]): E

  /**
   * Functionally removes a resolved attribute.
   */
  def minusResolvedAttribute(thisElem: E, attrEName: EName): E

  /**
   * Functionally updates the children by filtering.
   */
  def filteringChildren(thisElem: E, p: N => Boolean): E

  /**
   * Functionally adds a child. It is ensured by the implementation that no prefixed namespace undeclarations are
   * introduced. After all, they are illegal in XML 1.0.
   */
  def plusChild(thisElem: E, newChild: N): E

  /**
   * Optionally functionally adds a child. It is ensured by the implementation that no prefixed namespace undeclarations are
   * introduced. After all, they are illegal in XML 1.0.
   */
  def plusChildOption(thisElem: E, newChildOption: Option[N]): E

  /**
   * Functionally inserts a child. It is ensured by the implementation that no prefixed namespace undeclarations are
   * introduced. After all, they are illegal in XML 1.0.
   */
  def plusChild(thisElem: E, index: Int, newChild: N): E

  /**
   * Optionally functionally inserts a child. It is ensured by the implementation that no prefixed namespace undeclarations are
   * introduced. After all, they are illegal in XML 1.0.
   */
  def plusChildOption(thisElem: E, index: Int, newChildOption: Option[N]): E

  /**
   * Functionally adds some children. It is ensured by the implementation that no prefixed namespace undeclarations are
   * introduced. After all, they are illegal in XML 1.0.
   */
  def plusChildren(thisElem: E, childSeq: immutable.IndexedSeq[N]): E

  /**
   * Functionally removes a child.
   */
  def minusChild(thisElem: E, index: Int): E

  // TODO Functional update at relative path from the given element

  /**
   * Functionally updates the given element, by optionally including the given (optional) namespace in the Scope.
   */
  def includingNamespaceOption(thisElem: E, nsOption: Option[String]): E

  /**
   * See yaidom method `NamespaceUtils.pushUpPrefixedNamespaces`
   */
  def pushUpPrefixedNamespaces(thisElem: E): E

  /**
   * See yaidom method `simple.Elem.prettify`
   */
  def prettify(thisElem: E, indent: Int): E

  /**
   * See yaidom method `simple.Elem.prettify`
   */
  def prettify(thisElem: E, indent: Int, useTab: Boolean, newLine: String): E
}

object BackingElemEditor {

  type Aux[A, B] = BackingElemEditor {
    type N = A
    type E = B
  }
}
