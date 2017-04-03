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

import eu.cdevreeze.tqa.ENames.DimensionEName
import eu.cdevreeze.tqa.ENames.IdEName
import eu.cdevreeze.tqa.Namespaces.XbrliNamespace
import eu.cdevreeze.tqa.backingelem.nodeinfo.SaxonDocumentBuilder
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.queryapi.BackingElemApi
import net.sf.saxon.s9api.Processor
import org.scalatest.FlatSpec

/**
 * Test specification for yaidom querying.
 *
 * Exercise: fill in the needed implementations (replacing the "???"), and make this test spec run successfully.
 *
 * To do this exercise, make sure to have the API documentation of the yaidom library available.
 * Also see [[http://dvreeze.github.io/yaidom-queries.html]] and [[http://dvreeze.github.io/yaidom-and-namespaces.html]].
 *
 * Make sure to use a Java 8 JDK.
 *
 * @author Chris de Vreeze
 */
class QuerySpec extends FlatSpec {

  private val processor = new Processor(false)
  private val docBuilder = new SaxonDocumentBuilder(processor.newDocumentBuilder(), (uri => uri))

  private val rootElem: BackingElemApi =
    docBuilder.build(classOf[QuerySpec].getResource("/sample-Instance-Proof.xml").toURI)

  private val XbrldiNamespace = "http://xbrl.org/2006/xbrldi"

  private val GaapNamespace = "http://xasb.org/gaap"

  "The query API" should "allow filtering of child elements" in {
    // Filter all child elements of the root element named xbrli:context, having an ID attribute
    // starting with string "I-2007". Use the EName and not the (lexical) QName in the query.

    def hasIdStartingWithI2007(elem: BackingElemApi): Boolean = {
      elem.attributeOption(IdEName).exists(_.startsWith("I-2007"))
    }

    val filteredContexts: immutable.IndexedSeq[BackingElemApi] = ???

    assertResult(26) {
      filteredContexts.size
    }
  }

  it should "allow filtering of descendant elements" in {
    // Filter all descendant elements of the root element named xbrldi:explicitMember.
    // Use the EName and not the (lexical) QName in the query.

    val explicitMembers: immutable.IndexedSeq[BackingElemApi] = ???

    assertResult(Set("explicitMember")) {
      explicitMembers.map(_.localName).toSet
    }

    assertResult(true) {
      val dimensions: Set[EName] =
        explicitMembers.flatMap(_.attributeAsResolvedQNameOption(DimensionEName)).toSet

      val someDimensions: Set[EName] =
        List("EntityAxis", "BusinessSegmentAxis", "VerificationAxis", "PremiseAxis", "ReportDateAxis").
          map(localName => EName(GaapNamespace, localName)).toSet

      someDimensions.subsetOf(dimensions)
    }
  }

  it should "allow filtering of descendant-or-self elements" in {
    // Filter all descendant-or-self elements of the root element in the xbrli namespace.
    // Use the EName and not the (lexical) QName in the query.

    val xbrliElems: immutable.IndexedSeq[BackingElemApi] = ???

    assertResult(true) {
      val xbrliENames = xbrliElems.map(_.resolvedName).toSet

      val someXbrliENames: Set[EName] =
        List("xbrl", "context", "entity", "identifier", "segment", "period", "unit").
          map(localName => EName(XbrliNamespace, localName)).toSet

      someXbrliENames.subsetOf(xbrliENames)
    }

    assertResult(true) {
      xbrliElems.filter(_.resolvedName.namespaceUriOption.contains(GaapNamespace)).isEmpty
    }
  }
}
