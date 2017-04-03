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

import java.time.LocalDate
import java.time.LocalDateTime

import scala.collection.immutable

import eu.cdevreeze.tqa.ENames.DimensionEName
import eu.cdevreeze.tqa.ENames.IdEName
import eu.cdevreeze.tqa.ENames.SchemeEName
import eu.cdevreeze.tqa.Namespaces.LinkNamespace
import eu.cdevreeze.tqa.Namespaces.XbrliNamespace
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi.BackingElemApi

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
 * This code requires Java 8 or later!
 *
 * @author Chris de Vreeze
 */
final class XbrlInstanceElemToRowsConverter {

  type Elem = BackingElemApi

  import XbrlInstanceElemToRowsConverter._

  def convertXbrlInstance(instanceElem: Elem): immutable.IndexedSeq[Row] = {
    require(
      instanceElem.resolvedName == XbrliXbrlEName,
      s"Found name ${instanceElem.resolvedName} instead of $XbrliXbrlEName")

    // Contexts are always child elements of the xbrli:xbrl root element
    val contexts: immutable.IndexedSeq[Elem] =
      instanceElem.filterChildElems(_.resolvedName == XbrliContextEName)

    // Contexts must have an ID attribute, or else an exception is thrown
    val contextsById: Map[String, Elem] =
      contexts.groupBy(_.attribute(IdEName)).mapValues(_.head)

    // Units are always child elements of the xbrli:xbrl root element
    val units: immutable.IndexedSeq[Elem] =
      instanceElem.filterChildElems(_.resolvedName == XbrliUnitEName)

    // Units must have an ID attribute, or else an exception is thrown
    val unitsById: Map[String, Elem] =
      units.groupBy(_.attribute(IdEName)).mapValues(_.head)

    // The best we can do to recognize top-level facts, in the absence of a taxonomy.
    // Assumption: no tuples used, so all top level facts are item facts.
    val topLevelItemFacts: immutable.IndexedSeq[Elem] =
      instanceElem filterChildElems { e =>
        val namespaceOption = e.resolvedName.namespaceUriOption

        namespaceOption != Some(XbrliNamespace) && namespaceOption != Some(LinkNamespace)
      }

    val itemFactRows =
      topLevelItemFacts map { fact =>
        // An item fact must have a contextRef attribute, or else an exception is thrown
        val contextRef = fact.attribute(ContextRefEName)
        val unitRefOption = fact.attributeOption(UnitRefEName)

        // Non-resolvable contextRefs not allowed
        val xbrlContext =
          contextsById.getOrElse(contextRef, sys.error(s"Missing context with ID $contextRef"))

        // Non-resolvable unitRefs not allowed
        val xbrlUnitOption =
          unitRefOption.map(unitRef => unitsById.getOrElse(unitRef, sys.error(s"Missing unit with ID $unitRef")))

        convertItemFact(fact, xbrlContext, xbrlUnitOption)
      }

    itemFactRows
  }

  private def convertItemFact(fact: Elem, xbrlContext: Elem, xbrlUnitOption: Option[Elem]): Row = {
    val (identifierScheme, identifierValue) = getEntityIdentifierSchemeAndValue(xbrlContext)

    // The handling of periods is not entirely correct!

    Row(
      fact.path,
      fact.resolvedName,
      identifierScheme,
      identifierValue,
      findPeriodInstant(xbrlContext),
      findPeriodStartDate(xbrlContext),
      findPeriodEndDate(xbrlContext),
      findAllExplicitDimensionMembers(xbrlContext),
      xbrlUnitOption.toIndexedSeq.flatMap(u => findAllMeasures(u)))
  }

  private def getEntityIdentifierSchemeAndValue(xbrlContext: Elem): (String, String) = {
    require(
      xbrlContext.resolvedName == XbrliContextEName,
      s"Found name ${xbrlContext.resolvedName} instead of $XbrliContextEName")

    val identifierElem =
      xbrlContext.findElem(_.resolvedName == XbrliIdentifierEName).
        getOrElse(sys.error(s"Missing $XbrliIdentifierEName in context"))

    val scheme = identifierElem.attribute(SchemeEName)
    val identifierValue = identifierElem.text

    (scheme, identifierValue)
  }

  private def findPeriodInstant(xbrlContext: Elem): Option[LocalDateTime] = {
    require(
      xbrlContext.resolvedName == XbrliContextEName,
      s"Found name ${xbrlContext.resolvedName} instead of $XbrliContextEName")

    // Checking the ancestry, which must contain an xbrli:period element
    val instantElemOption =
      xbrlContext.findElem(e => e.resolvedName == XbrliInstantEName && e.path.containsName(XbrliPeriodEName))

    instantElemOption.map(e => parseInstantOrEndDate(e.text))
  }

  private def findPeriodStartDate(xbrlContext: Elem): Option[LocalDateTime] = {
    require(
      xbrlContext.resolvedName == XbrliContextEName,
      s"Found name ${xbrlContext.resolvedName} instead of $XbrliContextEName")

    // Checking the ancestry, which must contain an xbrli:period element
    val startDateElemOption =
      xbrlContext.findElem(e => e.resolvedName == XbrliStartDateEName && e.path.containsName(XbrliPeriodEName))

    startDateElemOption.map(e => parseStartDate(e.text))
  }

  private def findPeriodEndDate(xbrlContext: Elem): Option[LocalDateTime] = {
    require(
      xbrlContext.resolvedName == XbrliContextEName,
      s"Found name ${xbrlContext.resolvedName} instead of $XbrliContextEName")

    // Checking the ancestry, which must contain an xbrli:period element
    val endDateElemOption =
      xbrlContext.findElem(e => e.resolvedName == XbrliEndDateEName && e.path.containsName(XbrliPeriodEName))

    endDateElemOption.map(e => parseInstantOrEndDate(e.text))
  }

  private def findAllExplicitDimensionMembers(xbrlContext: Elem): Map[EName, EName] = {
    require(
      xbrlContext.resolvedName == XbrliContextEName,
      s"Found name ${xbrlContext.resolvedName} instead of $XbrliContextEName")

    val explicitMemberElems = xbrlContext.filterElems(_.resolvedName == XbrldiExplicitMemberEName)

    // The dimension attribute and element text must be interpreted as (dimension and member) ENames.
    // This requires the Scope of the element to be used for resolving lexical QNames.
    val result =
      explicitMemberElems map { explicitMemberElem =>
        val dimension = explicitMemberElem.attributeAsResolvedQName(DimensionEName)
        val member = explicitMemberElem.textAsResolvedQName

        (dimension -> member)
      }

    result.toMap
  }

  private def findAllMeasures(xbrlUnit: Elem): immutable.IndexedSeq[EName] = {
    require(
      xbrlUnit.resolvedName == XbrliUnitEName,
      s"Found name ${xbrlUnit.resolvedName} instead of $XbrliUnitEName")

    val measureElems = xbrlUnit.filterElems(_.resolvedName == XbrliMeasureEName)

    // The measure element texts must be interpreted as ENames.
    // This requires the Scope of the element to be used for resolving lexical QNames.
    val result =
      measureElems.map(measureElem => measureElem.textAsResolvedQName)

    result
  }

  private def parseStartDate(s: String): LocalDateTime = {
    if (s.contains('T')) {
      LocalDateTime.parse(s)
    } else {
      LocalDate.parse(s).atStartOfDay
    }
  }

  private def parseInstantOrEndDate(s: String): LocalDateTime = {
    if (s.contains('T')) {
      LocalDateTime.parse(s)
    } else {
      LocalDate.parse(s).plusDays(1).atStartOfDay
    }
  }
}

object XbrlInstanceElemToRowsConverter {

  val XbrldiNamespace = "http://xbrl.org/2006/xbrldi"

  val XbrliXbrlEName = EName(XbrliNamespace, "xbrl")
  val XbrliContextEName = EName(XbrliNamespace, "context")
  val XbrliUnitEName = EName(XbrliNamespace, "unit")
  val XbrliIdentifierEName = EName(XbrliNamespace, "identifier")
  val XbrliPeriodEName = EName(XbrliNamespace, "period")
  val XbrliInstantEName = EName(XbrliNamespace, "instant")
  val XbrliStartDateEName = EName(XbrliNamespace, "startDate")
  val XbrliEndDateEName = EName(XbrliNamespace, "endDate")
  val XbrliMeasureEName = EName(XbrliNamespace, "measure")

  val XbrldiExplicitMemberEName = EName(XbrldiNamespace, "explicitMember")

  val ContextRefEName = EName("contextRef")
  val UnitRefEName = EName("unitRef")

  final case class Row(
    val path: Path,
    val conceptName: EName,
    val identifierScheme: String,
    val identifierValue: String,
    val periodInstantOption: Option[LocalDateTime],
    val periodStartDateOption: Option[LocalDateTime],
    val periodEndDateOption: Option[LocalDateTime],
    val explicitDimensionMembers: Map[EName, EName],
    val measures: immutable.IndexedSeq[EName])
}
