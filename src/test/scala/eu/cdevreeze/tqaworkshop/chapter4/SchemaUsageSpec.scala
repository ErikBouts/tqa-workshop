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

package eu.cdevreeze.tqaworkshop.chapter4

import java.io.File
import java.net.URI

import org.scalatest.FlatSpec

import eu.cdevreeze.tqa.ENames
import eu.cdevreeze.tqa.SubstitutionGroupMap
import eu.cdevreeze.tqa.backingelem.nodeinfo.SaxonDocumentBuilder
import eu.cdevreeze.tqa.dom.GlobalElementDeclaration
import eu.cdevreeze.tqa.dom.NamedTypeDefinition
import eu.cdevreeze.tqa.dom.TaxonomyBase
import eu.cdevreeze.tqa.dom.TaxonomyElem
import eu.cdevreeze.tqa.relationship.DefaultRelationshipFactory
import eu.cdevreeze.tqa.taxonomy.BasicTaxonomy
import eu.cdevreeze.tqaworkshop.xbrlinstance.XbrlInstance
import eu.cdevreeze.yaidom.core.EName
import net.sf.saxon.s9api.Processor

/**
 * Test specification for finding declarations and definitions in (taxonomy) schemas. Schema content is either found
 * within a chosen schema document, using the TQA DOM API, or across collections of schema documents, using the TQA
 * query API (which in turn returns TQA DOM objects or collections thereof).
 *
 * Before doing this exercise, make sure to have some basic understanding of XML Schema (outside the realm of XBRL).
 *
 * Exercise: fill in the needed implementations (replacing the "???"), and make this test spec run successfully.
 *
 * To do this exercise, make sure to have the API documentation of the TQA and yaidom libraries available.
 *
 * Make sure to use a Java 8 JDK.
 *
 * @author Chris de Vreeze
 */
class SchemaUsageSpec extends FlatSpec {

  // Parsing the instance into an "BackingElemApi-backed" XbrlInstance with Saxon, although the use of Saxon does not influence the querying code.

  private val processor = new Processor(false)

  private val instanceDocBuilder =
    new SaxonDocumentBuilder(processor.newDocumentBuilder(), (uri => uri))

  private val rootDir = new File(classOf[SchemaUsageSpec].getResource("/taxonomy").toURI)

  private val taxoDocBuilder =
    new SaxonDocumentBuilder(processor.newDocumentBuilder(), (uri => uriToLocalUri(uri, rootDir)))

  private val xbrlInstance: XbrlInstance = {
    val elem = instanceDocBuilder.build(classOf[SchemaUsageSpec].getResource("/kvk-rpt-jaarverantwoording-2016-nlgaap-klein-publicatiestukken.xbrl").toURI)
    XbrlInstance(elem)
  }

  private val taxo: BasicTaxonomy = {
    val docUris = Vector(
      "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-codes.xsd",
      "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-domains.xsd",
      "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-data.xsd",
      "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-tuples.xsd",
      "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-data-verbose-lab-en.xml",
      "http://www.nltaxonomie.nl/nt11/sbr/20160610/dictionary/nl-types.xsd").map(s => URI.create(s))

    // Building the taxonomy DOM, one root element per parsed taxonomy document.

    val rootElems = docUris.map(uri => TaxonomyElem.build(taxoDocBuilder.build(uri)))

    val taxoBase = TaxonomyBase.build(rootElems)

    // Building a taxonomy that offers the TQA taxonomy query API.
    // It wraps the "DOM-level taxonomy".

    // Note that it only contains a few taxonomy documents, and is by no means closed under URI resolution,
    // so XLink simple links and locators and xs:import elements may be "dead links".
    // This also means that we may have to explicitly add some substitution group knowledge, which is indeed the case here.
    // More about that later in this exercise.
    // The "relationship factory" argument can now be ignored.

    BasicTaxonomy.build(
      taxoBase,
      SubstitutionGroupMap.from(Map(
        EName("{http://www.nltaxonomie.nl/2011/xbrl/xbrl-syntax-extension}domainMemberItem") -> ENames.XbrliItemEName)),
      DefaultRelationshipFactory.LenientInstance)
  } ensuring (_.relationships.nonEmpty)

  private val RjiNamespace = "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-data"

  //
  // Exercise 1
  //

  "The TQA DOM" should "support finding a specific global element declaration" in {
    // Finding rj-i:InvestmentProperties facts in instance, and finding the corresponding global element declaration in taxonomy schema
    // We look up the global element declaration the hard way in this exercise, via the schema root element.

    // Let's first find all rj-i:InvestmentProperties facts in the instance
    // We find 2 facts for item concept rj-i:InvestmentProperties, with fact values 135000 and 130000, respectively

    val RjiInvestmentPropertiesEName = EName(RjiNamespace, "InvestmentProperties")

    assertResult(2) {
      xbrlInstance.filterFacts(_.conceptEName == RjiInvestmentPropertiesEName).size
    }
    assertResult(Set("135000", "130000")) {
      xbrlInstance.filterFacts(_.conceptEName == RjiInvestmentPropertiesEName).map(_.text).toSet
    }

    // If we want the XBRL instance to be valid with respect to the taxonomy, it must at least be schema-valid.
    // This means for our RJ facts that there must be a corresponding global element declaration in the correct
    // RJ schema, and that this schema must be found.

    // The XBRL instance contains a reference to the so-called entrypoint schema, and through a process called DTS
    // discovery the correct RJ schema is found, but that is beyond the scope of this exercise. For now, assume that
    // the correct RJ schema is found and that it is part of the taxonomy.

    // Let's find the root element of the schema for the "rj-i" (target) namespace.

    val xsdRootElem = taxo.findAllXsdSchemas.filter(_.targetNamespaceOption.contains(RjiNamespace)).head

    assertResult(URI.create("http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-data.xsd")) {
      // The original http URI, not the local file URI, is stored with the schema document root element
      xsdRootElem.docUri
    }

    // Implement getting the global element declaration for the XBRL concept rj-i:InvestmentProperties.
    // Note that XBRL concepts are just global element declarations in terms of XML Schema.

    val conceptDecl: GlobalElementDeclaration = {
      // Use the xsdRootElem above as the starting point for obtaining the correct global element declaration.
      // Also use the GlobalElementDeclaration.targetEName method to match on the correct concept name.

      ???
    }

    assertResult(RjiInvestmentPropertiesEName) {
      // We use the term "target EName" for the combination of target namespace (of the schema root element) and
      // name attribute (of the global element declaration itself).

      conceptDecl.targetEName
    }
    assertResult(RjiInvestmentPropertiesEName) {
      // We know the schema has a target namespace attribute, so this is safe
      val tns = conceptDecl.backingElem.rootElem.attribute(ENames.TargetNamespaceEName)
      val name = conceptDecl.nameAttributeValue
      EName(tns, name)
    }
  }

  //
  // Exercise 2
  //

  "The TQA query API" should "support finding a specific global element declaration" in {
    // Finding the rj-i:InvestmentProperties global element declaration in the taxonomy.
    // We look up the global element declaration the easy way in this exercise, directly from the taxonomy using its query API,
    // and without first looking up a specific schema document root element.

    val RjiInvestmentPropertiesEName = EName(RjiNamespace, "InvestmentProperties")

    // Again, if we want the XBRL instance to be valid with respect to the taxonomy, it must at least be schema-valid.
    // This means for our RJ facts that there must be a corresponding global element declaration in the correct
    // RJ schema, and that this schema must be found.

    // The XBRL instance contains a reference to the so-called entrypoint schema, and through a process called DTS
    // discovery the correct RJ schema is found, but that is beyond the scope of this exercise. For now, assume that
    // the correct RJ schema is found and that it is part of the taxonomy.

    // Implement getting the global element declaration for the XBRL concept rj-i:InvestmentProperties.
    // Note that XBRL concepts are just global element declarations in terms of XML Schema.

    val conceptDecl: GlobalElementDeclaration = {
      // Use the entire taxonomy called "taxo" as the starting point for obtaining the correct global element declaration.

      ???
    }

    assertResult(RjiInvestmentPropertiesEName) {
      // Again, we use the term "target EName" for the combination of target namespace (of the schema root element) and
      // name attribute (of the global element declaration itself).

      conceptDecl.targetEName
    }
  }

  //
  // Exercise 3
  //

  it should "support finding the type definition of a type (of a global element declaration)" in {
    // Finding the type definition of the type of the rj-i:InvestmentProperties global element declaration in the taxonomy.

    val RjiInvestmentPropertiesEName = EName(RjiNamespace, "InvestmentProperties")

    val conceptDecl: GlobalElementDeclaration =
      taxo.getGlobalElementDeclaration(RjiInvestmentPropertiesEName)

    val typeOption: Option[EName] = conceptDecl.typeOption

    assertResult(conceptDecl.attributeAsResolvedQNameOption(ENames.TypeEName)) {
      typeOption
    }
    assertResult(Some(EName("{http://www.nltaxonomie.nl/nt11/sbr/20160610/dictionary/nl-types}monetaryNoDecimalsItemType"))) {
      typeOption
    }

    // Implement getting the type definition for the above-mentioned type nl-types:monetaryNoDecimalsItemType.

    val typeDef: NamedTypeDefinition = {
      taxo.getNamedTypeDefinition(typeOption.get)

      ???
    }

    assertResult(typeOption.get) {
      // We use the term "target EName" for the combination of target namespace (of the schema root element) and
      // name attribute (of the named type definition itself).

      typeDef.targetEName
    }
    assertResult(typeOption.get) {
      // We know the schema has a target namespace attribute, so this is safe
      val tns = typeDef.backingElem.rootElem.attribute(ENames.TargetNamespaceEName)
      val name = typeDef.nameAttributeValue
      EName(tns, name)
    }
  }

  // Find the definition of the schema type of the global element declaration (item type is complex type with simple content)

  // Determine the substitution group, and whether it is an XBRL item declaration

  // Find some "domain member" declaration in rj-domains.xsd, and determine if this is an item declaration or not

  // Query for concrete RJ item declarations, and check if RJ item facts in the instance are indeed RJ items

  // Query for concrete RJ tuple declarations, and check if RJ tuple facts in the instance are indeed RJ tuples

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
