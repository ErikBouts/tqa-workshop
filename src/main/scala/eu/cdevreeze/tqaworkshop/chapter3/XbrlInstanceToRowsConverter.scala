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

package eu.cdevreeze.tqaworkshop.chapter3

import java.time.LocalDateTime

import scala.collection.immutable

import eu.cdevreeze.tqaworkshop.xbrlinstance.ItemFact
import eu.cdevreeze.tqaworkshop.xbrlinstance.XbrlInstance
import eu.cdevreeze.tqaworkshop.xbrlinstance.XbrliContext
import eu.cdevreeze.tqaworkshop.xbrlinstance.XbrliUnit
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path

/**
 * See `XbrlInstanceElemToRowsConverter` in chapter 1, but here we use a "yaidom dialect" for XBRL instances.
 * Note how this XBRL instance model cleans up the code, not in the last place the exceptions.
 *
 * This code requires Java 8 or later!
 *
 * @author Chris de Vreeze
 */
final class XbrlInstanceToRowsConverter {

  import XbrlInstanceToRowsConverter._

  def convertXbrlInstance(xbrlInstance: XbrlInstance): immutable.IndexedSeq[Row] = {
    // Assumption: no tuples used, so all top level facts are item facts.
    val topLevelItemFacts: immutable.IndexedSeq[ItemFact] =
      xbrlInstance.allTopLevelItems

    val itemFactRows =
      topLevelItemFacts map { fact =>
        // An item fact must have a contextRef attribute, or else an exception is thrown
        val contextRef = fact.contextRef
        val unitRefOption = fact.unitRefOption

        // Non-resolvable contextRefs not allowed
        val xbrlContext =
          xbrlInstance.allContextsById.getOrElse(contextRef, sys.error(s"Missing context with ID $contextRef"))

        // Non-resolvable unitRefs not allowed
        val xbrlUnitOption =
          unitRefOption.map(unitRef => xbrlInstance.allUnitsById.getOrElse(unitRef, sys.error(s"Missing unit with ID $unitRef")))

        convertItemFact(fact, xbrlContext, xbrlUnitOption)
      }

    itemFactRows
  }

  private def convertItemFact(fact: ItemFact, xbrlContext: XbrliContext, xbrlUnitOption: Option[XbrliUnit]): Row = {
    Row(
      fact.path,
      fact.conceptEName,
      xbrlContext.identifierScheme,
      xbrlContext.identifierValue,
      if (xbrlContext.period.isInstantPeriod) Some(xbrlContext.period.asInstantPeriod.instant.dateTime) else None,
      if (xbrlContext.period.isStartEndDatePeriod) Some(xbrlContext.period.asStartEndDatePeriod.startDate.dateTime) else None,
      if (xbrlContext.period.isStartEndDatePeriod) Some(xbrlContext.period.asStartEndDatePeriod.endDate.dateTime) else None,
      xbrlContext.explicitDimensionMembers,
      xbrlUnitOption.toIndexedSeq.flatMap(u => u.measures))
  }
}

object XbrlInstanceToRowsConverter {

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
