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
import scala.collection.mutable

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.simple
import eu.cdevreeze.yaidom.utils.NamespaceUtils

/**
 * Editor of indexed elements.
 *
 * @author Chris de Vreeze
 */
final class IndexedElemEditor(
    val getFallbackPrefixForNamespace: String => String) extends BackingElemEditor {

  type N = indexed.IndexedScopedNode.Node

  type E = indexed.Elem

  def withResolvedName(thisElem: E, newEName: EName): E = {
    val oldScope = thisElem.scope

    val newScope =
      newEName.namespaceUriOption.map(ns => oldScope.includingNamespace(ns, () => getFallbackPrefixForNamespace(ns))).
        getOrElse(oldScope)

    assert(newEName.namespaceUriOption.forall(ns => newScope.prefixNamespaceMap.values.toSet.contains(ns)))

    val prefixOption: Option[String] =
      newEName.namespaceUriOption.map(ns => newScope.prefixForNamespace(ns, () => sys.error(s"Missing prefix for namespace $ns")))

    val newQName = newEName.toQName(prefixOption)

    doUpdate(thisElem) { oldElem =>
      oldElem.copy(qname = newQName, scope = newScope).notUndeclaringPrefixes(Scope.Empty)
    }
  }

  def withResolvedAttributes(thisElem: E, newResolvedAttributes: immutable.Iterable[(EName, String)]): E = {
    val oldScope = thisElem.scope

    val newScope = newResolvedAttributes.foldLeft(oldScope) {
      case (accScope, (attrName, attrValue)) =>
        attrName.namespaceUriOption.map(ns => accScope.includingNamespace(ns, () => getFallbackPrefixForNamespace(ns))).
          getOrElse(accScope)
    }

    val attrs: immutable.IndexedSeq[(QName, String)] = newResolvedAttributes.toIndexedSeq map {
      case (attrName, attrValue) =>
        val prefixOption: Option[String] =
          attrName.namespaceUriOption.map(ns => newScope.prefixForNamespace(ns, () => sys.error(s"Missing prefix for namespace $ns")))

        val attrQName = attrName.toQName(prefixOption)

        (attrQName -> attrValue)
    }

    doUpdate(thisElem) { oldElem =>
      oldElem.copy(attributes = attrs, scope = newScope).notUndeclaringPrefixes(Scope.Empty)
    }
  }

  def withChildren(thisElem: E, newChildren: immutable.IndexedSeq[N]): E = {
    doUpdate(thisElem) { oldElem =>
      oldElem.copy(children = newChildren.map(n => getUnderlyingNode(n))).notUndeclaringPrefixes(Scope.Empty)
    }
  }

  def withChildSeqs(thisElem: E, newChildSeqs: immutable.IndexedSeq[immutable.IndexedSeq[N]]): E = {
    withChildren(thisElem, newChildSeqs.flatten)
  }

  def filteringResolvedAttributes(thisElem: E, p: (EName, String) => Boolean): E = {
    withResolvedAttributes(thisElem, thisElem.resolvedAttributes.filter(kv => p(kv._1, kv._2)))
  }

  def plusResolvedAttribute(thisElem: E, attrEName: EName, attrValue: String): E = {
    val newAttrs =
      (mutable.LinkedHashMap(thisElem.resolvedAttributes.toSeq: _*) + (attrEName -> attrValue)).toIndexedSeq

    withResolvedAttributes(thisElem, newAttrs)
  }

  def plusResolvedAttributeOption(thisElem: E, attrEName: EName, attrValueOption: Option[String]): E = {
    attrValueOption.map(attrValue => plusResolvedAttribute(thisElem, attrEName, attrValue)).getOrElse(thisElem)
  }

  def minusResolvedAttribute(thisElem: E, attrEName: EName): E = {
    withResolvedAttributes(thisElem, thisElem.resolvedAttributes.filterNot(_._1 == attrEName))
  }

  def filteringChildren(thisElem: E, p: N => Boolean): E = {
    withChildren(thisElem, getChildren(thisElem).filter(p))
  }

  def plusChild(thisElem: E, newChild: N): E = {
    withChildren(thisElem, getChildren(thisElem) :+ newChild)
  }

  def plusChildOption(thisElem: E, newChildOption: Option[N]): E = {
    newChildOption.map(ch => plusChild(thisElem, ch)).getOrElse(thisElem)
  }

  def plusChild(thisElem: E, index: Int, newChild: N): E = {
    val children = getChildren(thisElem)

    require(
      index <= children.size,
      s"Expected index $index to be at most the number of children: ${children.size}")

    if (index == children.size) plusChild(thisElem, newChild)
    else withChildren(thisElem, children.patch(index, immutable.IndexedSeq(newChild, children(index)), 1))
  }

  def plusChildOption(thisElem: E, index: Int, newChildOption: Option[N]): E = {
    newChildOption.map(ch => plusChild(thisElem, index, ch)).getOrElse(thisElem)
  }

  def plusChildren(thisElem: E, childSeq: immutable.IndexedSeq[N]): E = {
    withChildren(thisElem, getChildren(thisElem) ++ childSeq)
  }

  def minusChild(thisElem: E, index: Int): E = {
    val children = getChildren(thisElem)

    require(
      index < children.size,
      s"Expected index $index to be less than the number of children: ${children.size}")

    withChildren(thisElem, children.patch(index, immutable.IndexedSeq(), 1))
  }

  def includingNamespaceOption(thisElem: E, nsOption: Option[String]): E = {
    val oldScope = thisElem.scope

    val newScope =
      nsOption.map(ns => oldScope.includingNamespace(ns, () => getFallbackPrefixForNamespace(ns))).
        getOrElse(oldScope)

    assert(nsOption.forall(ns => newScope.prefixNamespaceMap.values.toSet.contains(ns)))

    doUpdate(thisElem) { oldElem =>
      oldElem.copy(scope = newScope).notUndeclaringPrefixes(Scope.Empty)
    }
  }

  def pushUpPrefixedNamespaces(thisElem: E): E = {
    doUpdate(thisElem)(e => NamespaceUtils.pushUpPrefixedNamespaces(e))
  }

  def prettify(thisElem: E, indent: Int): E = {
    doUpdate(thisElem)(_.prettify(indent))
  }

  def prettify(thisElem: E, indent: Int, useTab: Boolean, newLine: String): E = {
    doUpdate(thisElem)(_.prettify(indent, useTab, newLine))
  }

  private def doUpdate(thisElem: E)(f: simple.Elem => simple.Elem): E = {
    val newUnderlyingRootElem =
      thisElem.underlyingRootElem.updateElemOrSelf(thisElem.path)(f)

    indexed.Elem(thisElem.docUriOption, newUnderlyingRootElem, thisElem.path)
  }

  private def getChildren(thisElem: E): immutable.IndexedSeq[N] = {
    var childElems: List[E] = thisElem.findAllChildElems.toList

    thisElem.underlyingElem.children map {
      case e: simple.Elem =>
        val childElem = childElems.head
        childElems = childElems.tail
        childElem
      case t: simple.Text                   => indexed.IndexedScopedNode.Text(t.text, t.isCData)
      case pi: simple.ProcessingInstruction => indexed.IndexedScopedNode.ProcessingInstruction(pi.target, pi.data)
      case c: simple.Comment                => indexed.IndexedScopedNode.Comment(c.text)
      case er: simple.EntityRef             => indexed.IndexedScopedNode.EntityRef(er.entity)
    } ensuring (_ => childElems.isEmpty)
  }

  private def getUnderlyingNode(node: N): simple.Node = node match {
    case e: indexed.IndexedScopedNode.Elem[_]                => e.underlyingElem.asInstanceOf[simple.Elem]
    case t: indexed.IndexedScopedNode.Text                   => simple.Text(t.text, t.isCData)
    case pi: indexed.IndexedScopedNode.ProcessingInstruction => simple.ProcessingInstruction(pi.target, pi.data)
    case c: indexed.IndexedScopedNode.Comment                => simple.Comment(c.text)
    case er: indexed.IndexedScopedNode.EntityRef             => simple.EntityRef(er.entity)
  }
}
