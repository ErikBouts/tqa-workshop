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
import scala.reflect.classTag

import org.scalatest.FlatSpec

import eu.cdevreeze.tqa.ENames
import eu.cdevreeze.tqa.SubstitutionGroupMap
import eu.cdevreeze.tqa.backingelem.nodeinfo.SaxonDocumentBuilder
import eu.cdevreeze.tqa.dom.GlobalElementDeclaration
import eu.cdevreeze.tqa.dom.ItemDeclaration
import eu.cdevreeze.tqa.dom.NamedTypeDefinition
import eu.cdevreeze.tqa.dom.TaxonomyBase
import eu.cdevreeze.tqa.dom.TaxonomyElem
import eu.cdevreeze.tqa.dom.TupleDeclaration
import eu.cdevreeze.tqa.relationship.DefaultRelationshipFactory
import eu.cdevreeze.tqa.taxonomy.BasicTaxonomy
import eu.cdevreeze.tqaworkshop.xbrlinstance.XbrlInstance
import eu.cdevreeze.yaidom.core.EName
import net.sf.saxon.s9api.Processor
import eu.cdevreeze.tqa.taxonomybuilder.DefaultDtsCollector
import eu.cdevreeze.tqa.taxonomybuilder.TaxonomyBuilder

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

    val documentCollector = DefaultDtsCollector(Set(entrypointUri))

    val relationshipFactory = DefaultRelationshipFactory.StrictInstance

    // Below, the "relationship factory" argument is used for turning XLink arcs into (higher level) relationships.
    // Without it, we would not be able to query the taxonomy for relationships.

    val taxoBuilder =
      TaxonomyBuilder.
        withDocumentBuilder(taxoDocBuilder).
        withDocumentCollector(documentCollector).
        withRelationshipFactory(relationshipFactory)

    // Use the TaxonomyBuilder to create the BasicTaxonomy. It offers the TQA taxonomy query API,
    // whose functions mainly return taxonomy content and relationships.

    taxoBuilder.build()
  } ensuring (_.relationships.nonEmpty)

  private val RjiNamespace = "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-data"
  private val RjtNamespace = "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-tuples"
  private val RjdmNamespace = "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-domains"

  //
  // Exercise 1
  //

  "The TQA query API" should "support retrieval of concept labels" in {
  }

  //
  // Exercise 2
  //

  it should "support retrieval of concept references" in {
  }

  //
  // Exercise 3
  //

  it should "support retrieval of parent-child relationships" in {
  }

  //
  // Exercise 4
  //

  it should "support retrieval of parent-child relationship paths" in {
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
