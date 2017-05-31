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

package eu.cdevreeze.tqaworkshop.chapter5

import java.io.File
import java.net.URI

import scala.collection.immutable

import org.scalatest.FlatSpec

import eu.cdevreeze.tqa.backingelem.nodeinfo.SaxonDocumentBuilder
import eu.cdevreeze.tqa.dom.ConceptLabelResource
import eu.cdevreeze.tqa.dom.ConceptReferenceResource
import eu.cdevreeze.tqa.relationship.ConceptLabelRelationship
import eu.cdevreeze.tqa.relationship.ConceptReferenceRelationship
import eu.cdevreeze.tqa.relationship.DefaultRelationshipFactory
import eu.cdevreeze.tqa.taxonomy.BasicTaxonomy
import eu.cdevreeze.tqa.taxonomybuilder.DefaultDtsCollector
import eu.cdevreeze.tqa.taxonomybuilder.TaxonomyBuilder
import eu.cdevreeze.tqaworkshop.xbrlinstance.XbrlInstance
import eu.cdevreeze.yaidom.core.EName
import net.sf.saxon.s9api.Processor

/**
 * Test specification for using the TQA query API, querying for relationships and taxonomy DOM elements.
 *
 * Before doing this exercise, make sure to have done all exercises of the preceding chapters. This exercise is about
 * 2 things: the notion of relationships (as opposed to lower level XLink arcs), and the use of the TQA query API
 * for finding taxonomy content (relationships and taxonomy DOM elements). This exercise explores the use of TQA
 * in practice, and may therefore be the most important exercise of the TQA workshop.
 *
 * Exercise: fill in the needed implementations (replacing the "???"), and make this test spec run successfully.
 *
 * To do this exercise, make sure to have the API documentation of the TQA and yaidom libraries available.
 *
 * Make sure to use a Java 8 JDK.
 *
 * @author Chris de Vreeze
 */
class TqaQueryApiUsageSpec extends FlatSpec {

  // Parsing the instance into an "BackingElemApi-backed" XbrlInstance with Saxon, although the use of Saxon does not influence the querying code.

  private val processor = new Processor(false)

  private val instanceDocBuilder =
    new SaxonDocumentBuilder(processor.newDocumentBuilder(), (uri => uri))

  private val xbrlInstance: XbrlInstance = {
    val elem = instanceDocBuilder.build(classOf[TqaQueryApiUsageSpec].getResource("/kvk-rpt-jaarverantwoording-2016-nlgaap-klein-publicatiestukken.xbrl").toURI)
    XbrlInstance(elem)
  }

  private val taxo: BasicTaxonomy = {
    // Like in the chapter 4 SchemaUsageSpec exercise, we create a BasicTaxonomy.
    // Unlike that exercise, here we use a TaxonomyBuilder, and we build a "closed DTS" for one entrypoint schema.
    // See the Core XBRL specification for the notion of a DTS (discoverable taxonomy set) and
    // the rules to gather an entire DTS. Due to the creation of a DTS, we do not need to provide any external knowledge
    // about substitution groups.

    val rootDir = new File(classOf[TqaQueryApiUsageSpec].getResource("/taxonomy").toURI)

    val taxoDocBuilder =
      new SaxonDocumentBuilder(processor.newDocumentBuilder(), (uri => uriToLocalUri(uri, rootDir)))

    val entrypointUri =
      URI.create("http://www.nltaxonomie.nl/nt11/kvk/20161214/entrypoints/kvk-rpt-jaarverantwoording-2016-nlgaap-klein-publicatiestukken.xsd")

    // The DocumentCollector knows which taxonomy documents belong to the resulting BasicTaxonomy.
    // Here the DocumentCollector is a DefaultDtsCollector, which collects the documents belonging to the DTS.

    val documentCollector = DefaultDtsCollector(Set(entrypointUri))

    // The "relationship factory" is used for turning XLink arcs into (higher level) relationships.
    // Without it, we would not be able to query the taxonomy for relationships.

    val relationshipFactory = DefaultRelationshipFactory.StrictInstance

    val taxoBuilder =
      TaxonomyBuilder.
        withDocumentBuilder(taxoDocBuilder).
        withDocumentCollector(documentCollector).
        withRelationshipFactory(relationshipFactory)

    // Use the TaxonomyBuilder to create the BasicTaxonomy. It offers the TQA taxonomy query API,
    // whose functions mainly return taxonomy content and relationships.

    taxoBuilder.build()
  } ensuring (taxo => taxo.relationships.nonEmpty)

  private val RjiNamespace = "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-data"
  private val RjtNamespace = "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-tuples"
  private val RjdmNamespace = "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-domains"
  private val VenjBw2iNamespace = "http://www.nltaxonomie.nl/nt11/venj/20161214/dictionary/venj-bw2-data"
  private val VenjBw2AbstrNamespace = "http://www.nltaxonomie.nl/nt11/venj/20161214/presentation/venj-bw2-abstracts"
  private val KvkAbstrNamespace = "http://www.nltaxonomie.nl/nt11/kvk/20161214/presentation/kvk-abstracts"

  //
  // Exercise 1
  //

  "The TQA query API" should "support retrieval of concept labels" in {
    // For all facts in the sample XBRL instance, find their (item and tuple) concepts in the taxonomy, and their
    // verbose Dutch labels (falling back to standard labels if the verbose label does not exist).

    // Note that in XBRL there are 2 entirely different "label" terms. One "label" term refers to XLink labels, and
    // the "label" term used here refers to label texts associated with concepts in so-called label links.

    // First find the (item and tuple) concepts for all facts in the instance.
    // The EName of a fact identifies its concept in the taxonomy.

    val concepts: Set[EName] = xbrlInstance.findAllFacts.map(_.resolvedName).toSet

    val verboseLabelRole = "http://www.xbrl.org/2003/role/verboseLabel"
    val standardLabelRole = "http://www.xbrl.org/2003/role/label"

    // Implement getting the verbose and otherwise standard Dutch label of the given concept. Not trivial, but
    // many hints are given.

    // To find a concept label, we have to find a ConceptLabelRelationship, and return its ConceptLabelResource.
    // There is no need to descend to the level of XLink arcs, locators and resources. Just query for concept-label
    // relationships and their label resources.

    def findConceptLabelResource(concept: EName): Option[ConceptLabelResource] = {
      // Use the taxo to find the (first) correct ConceptLabelResource, if any

      // Filter all concept-label relationships outgoing from the concept having language "nl".
      // Use query API method filterOutgoingConceptLabelRelationships to that end.

      val conceptLabelRelationships: immutable.IndexedSeq[ConceptLabelRelationship] =
        ???

      // In this collection of concept-label relationships, find the first one having
      // the resource role "http://www.xbrl.org/2003/role/verboseLabel", falling back
      // to "http://www.xbrl.org/2003/role/label" if there is no verbose Dutch label.

      val conceptLabelRelationshipOption: Option[ConceptLabelRelationship] =
        ???

      // Return the optional label resource of the optional concept-label relationship

      conceptLabelRelationshipOption.map(rel => ???)
    }

    val conceptLabelTexts: Map[EName, String] =
      concepts.toSeq.map(concept => (concept -> findConceptLabelResource(concept))).
        filter(_._2.nonEmpty).toMap.mapValues(_.head.trimmedText)

    assertResult(Some("Som van de herwaarderingen van materiële vaste activa op de balansdatum")) {
      // Verbose label
      conceptLabelTexts.get(EName(VenjBw2iNamespace, "PropertyPlantEquipmentAccumulatedRevaluations"))
    }

    assertResult(Some("Type grondslag")) {
      // Standard label, because verbose Dutch label is missing
      conceptLabelTexts.get(EName(VenjBw2iNamespace, "BasisOfPreparation"))
    }
  }

  //
  // Exercise 2
  //

  it should "support retrieval of concept references" in {
    // For all facts in the sample XBRL instance, find their (item and tuple) concepts in the taxonomy, and their
    // standard references.

    // First find the (item and tuple) concepts for all facts in the instance.
    // The EName of a fact identifies its concept in the taxonomy.

    val concepts: Set[EName] = xbrlInstance.findAllFacts.map(_.resolvedName).toSet

    val standardReferenceRole = "http://www.xbrl.org/2003/role/reference"

    // Implement getting the standard reference of the given concept. Not trivial, but after exercise 1 not too hard either.
    // The implementation approach is very similar to that of exercise 1.

    // To find a concept reference, we have to find a ConceptReferenceRelationship, and return its ConceptReferenceResource.
    // There is no need to descend to the level of XLink arcs, locators and resources. Just query for concept-reference
    // relationships and their reference resources.

    def findConceptReferenceResource(concept: EName): Option[ConceptReferenceResource] = {
      // Use the taxo to find the (first) standard ConceptReferenceResource for the given concept, if any

      val conceptReferenceRelationshipOption: Option[ConceptReferenceRelationship] =
        ???

      conceptReferenceRelationshipOption.map(rel => ???)
    }

    val conceptReferences: Map[EName, ConceptReferenceResource] =
      concepts.toSeq.map(concept => (concept -> findConceptReferenceResource(concept))).
        filter(_._2.nonEmpty).toMap.mapValues(_.head)

    assertResult(true) {
      val expectedDocUris =
        Set(
          URI.create("http://www.nltaxonomie.nl/nt11/venj/20161214/dictionary/venj-bw2-data-ref.xml"),
          URI.create("http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-venj-bw2-data-ref.xml"))

      val foundDocUris =
        conceptReferences.filterKeys(_.namespaceUriOption.contains(VenjBw2iNamespace)).values.map(_.docUri).toSet

      foundDocUris.subsetOf(expectedDocUris)
    }
  }

  //
  // Exercise 3
  //

  it should "support retrieval of parent-child relationships" in {
    // Find all abstract top level sources of parent-child relationships in a given ELR.

    // Implement the following function. Somewhat challenging.

    // Hint: first find the parent-child relationships of the given ELR, then determine their top-level source
    // concepts (that is, sources that are not targets in any of the found relationships), and finally filter
    // those concepts that are abstract (which follows from their global element declarations).

    def findAllAbstractTopLevelSourceConceptsInParentChildTree(parentChildElr: String): Set[EName] = {
      // Use the taxo variable to query for parent-child relationships and concept declarations

      ???
    }

    val elr = "urn:kvk:linkrole:notes-income-tax-expense"

    val expectedAbstractTopLevelSources =
      Set(EName(KvkAbstrNamespace, "IncomeTaxExpenseDisclosureTitle"))

    assertResult(expectedAbstractTopLevelSources) {
      findAllAbstractTopLevelSourceConceptsInParentChildTree(elr)
    }
  }

  //
  // Exercise 4
  //

  it should "support retrieval of parent-child relationship paths" in {
    // Find all longest parent-child relationship paths in a given ELR, outgoing from a given concept.

    // Implement the following function. The challenge is in finding the most appropriate TQA query method
    // for this task.

    def findLongestParentChildRelationshipPaths(
      startConcept: EName,
      elr: String): immutable.IndexedSeq[taxo.ParentChildRelationshipPath] = {

      // Return all longest parent-child relationship paths starting with the given concept,
      // where each relationship in the path has the given ELR.

      ???
    }

    val elr = "urn:kvk:linkrole:balance-sheet"
    val startConcept = EName(KvkAbstrNamespace, "BalanceSheetCompleteTitle")

    val relationshipPaths = findLongestParentChildRelationshipPaths(startConcept, elr)

    assertResult(Set(startConcept)) {
      relationshipPaths.map(_.sourceConcept).toSet
    }

    val someExpectedLeafConcepts =
      Set(
        EName(VenjBw2iNamespace, "BalanceSheetBeforeAfterAppropriationResults"),
        EName(VenjBw2iNamespace, "IntangibleAssets"),
        EName(VenjBw2iNamespace, "PropertyPlantEquipment"),
        EName(RjiNamespace, "InvestmentProperties"),
        EName(VenjBw2iNamespace, "FinancialAssets"),
        EName(VenjBw2iNamespace, "Inventories"),
        EName(RjiNamespace, "ConstructionContractsAssets"),
        EName(VenjBw2iNamespace, "Receivables"),
        EName(VenjBw2iNamespace, "SecuritiesCurrent"),
        EName(VenjBw2iNamespace, "Provisions"),
        EName(VenjBw2iNamespace, "LiabilitiesNoncurrent"),
        EName(VenjBw2iNamespace, "ShareCapital"),
        EName(VenjBw2iNamespace, "SharePremium"),
        EName(VenjBw2iNamespace, "RevaluationReserve"),
        EName(VenjBw2iNamespace, "StatutoryReserves"),
        EName(VenjBw2iNamespace, "ReservesOther"),
        EName(VenjBw2iNamespace, "RetainedEarnings"),
        EName(VenjBw2iNamespace, "ResultForTheYear"))

    assertResult(true) {
      someExpectedLeafConcepts.subsetOf(relationshipPaths.map(_.targetConcept).toSet)
    }

    // All found relationships have the same ELR

    assertResult(Set(elr)) {
      relationshipPaths.flatMap(_.relationships).map(_.elr).toSet
    }
  }

  //
  // Exercise 5
  //

  it should "support retrieval of parent-child relationships and affected concept declarations" in {
  }

  //
  // Exercise 6
  //

  it should "support retrieval of definition relationships" in {
  }

  //
  // Exercise 7
  //

  it should "support retrieval of generic element-label relationships" in {
  }

  //
  // Exercise 8
  //

  it should "support retrieval of custom generic relationships" in {
  }

  //
  // Exercise 9
  //

  it should "support queries for properties of concepts (like substitution groups) in parent-child trees" in {
  }

  //
  // Exercise 10
  //

  it should "support queries for kinds of relationships of concrete item concepts" in {
  }

  //
  // Exercise 11
  //

  it should "support rewriting specific relationship queries in terms of more general query API methods" in {
  }

  //
  // Exercise 12
  //

  it should "support interesting queries about label resource uniqueness" in {
  }

  //
  // Exercise 13
  //

  it should "support interesting queries about ELRs having generic labels" in {
  }

  //
  // Exercise 14
  //

  it should "support interesting queries about all locators being used in relationships" in {
  }

  //
  // Exercise 15
  //

  it should "support interesting queries about networks of relationships" in {
  }

  private def uriToLocalUri(uri: URI, rootDir: File): URI = {
    // Not robust
    val relativePath = uri.getScheme match {
      case "http"  => uri.toString.drop("http://".size)
      case "https" => uri.toString.drop("https://".size)
      case _       => sys.error(s"Unexpected URI $uri")
    }

    val f = new File(rootDir, relativePath.dropWhile(_ == '/'))
    f.toURI
  }
}
