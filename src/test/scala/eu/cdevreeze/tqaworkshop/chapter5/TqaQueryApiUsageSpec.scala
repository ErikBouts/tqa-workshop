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

/**
 * Test specification for using the TQA query API, querying for relationships and taxonomy DOM elements.
 *
 * Before doing this exercise, make sure to have done all exercises of the preceding chapters. This exercise is about
 * 2 things: the notion of relationships (as opposed to lower level XLink arcs), and the use of the TQA query API
 * for finding taxonomy content (relationships or taxonomy DOM elements). This exercise explores the use of TQA
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

  private val rootDir = new File(classOf[TqaQueryApiUsageSpec].getResource("/taxonomy").toURI)

  private val taxoDocBuilder =
    new SaxonDocumentBuilder(processor.newDocumentBuilder(), (uri => uriToLocalUri(uri, rootDir)))

  private val xbrlInstance: XbrlInstance = {
    val elem = instanceDocBuilder.build(classOf[TqaQueryApiUsageSpec].getResource("/kvk-rpt-jaarverantwoording-2016-nlgaap-klein-publicatiestukken.xbrl").toURI)
    XbrlInstance(elem)
  }

  private val SbrDomainMemberItemEName = EName("{http://www.nltaxonomie.nl/2011/xbrl/xbrl-syntax-extension}domainMemberItem")
  private val SbrPresentationTupleEName = EName("{http://www.nltaxonomie.nl/2011/xbrl/xbrl-syntax-extension}presentationTuple")

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
    // The "relationship factory" argument is used for turning XLink arcs into higher level relationships.

    BasicTaxonomy.build(
      taxoBase,
      SubstitutionGroupMap.from(
        Map(SbrDomainMemberItemEName -> ENames.XbrliItemEName, SbrPresentationTupleEName -> ENames.XbrliTupleEName)),
      DefaultRelationshipFactory.LenientInstance)
  } ensuring (_.relationships.nonEmpty)

  private val RjiNamespace = "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-data"
  private val RjtNamespace = "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-tuples"
  private val RjdmNamespace = "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-domains"

  //
  // Exercise 1
  //

  "The TQA query API" should "support XXX" in {
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
