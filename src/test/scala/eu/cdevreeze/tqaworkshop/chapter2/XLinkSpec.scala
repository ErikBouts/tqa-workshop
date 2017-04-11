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

package eu.cdevreeze.tqaworkshop.chapter2

import java.io.File
import java.net.URI

import scala.collection.immutable
import scala.reflect.classTag

import org.scalatest.FlatSpec

import eu.cdevreeze.tqa.backingelem.nodeinfo.SaxonDocumentBuilder
import eu.cdevreeze.tqa.dom.Linkbase
import eu.cdevreeze.tqa.dom.LinkbaseRef
import eu.cdevreeze.tqa.dom.SimpleLink
import eu.cdevreeze.tqa.dom.TaxonomyBase
import eu.cdevreeze.tqa.dom.XsdSchema
import net.sf.saxon.s9api.Processor

/**
 * Test specification for low level XLink processing. In this test case, the low level type-safe taxonomy DOM
 * in TQA is shown, but almost exclusively at the level of XLink content.
 *
 * Exercise: fill in the needed implementations (replacing the "???"), and make this test spec run successfully.
 *
 * To do this exercise, make sure to have the API documentation of the TQA and yaidom libraries available.
 *
 * Make sure to use a Java 8 JDK.
 *
 * @author Chris de Vreeze
 */
class XLinkSpec extends FlatSpec {

  private val taxoRootDir = new File(classOf[XLinkSpec].getResource("/taxonomy").toURI)

  private val processor = new Processor(false)
  private val docBuilder =
    new SaxonDocumentBuilder(processor.newDocumentBuilder(), uriToLocalUri(_, taxoRootDir))

  private val schemaUri =
    URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd")
  private val linkbaseUri =
    URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data-verbose-lab-en.xml")

  private val taxonomyBase: TaxonomyBase = {
    val schemaElem = docBuilder.build(schemaUri)
    val linkbaseElem = docBuilder.build(linkbaseUri)

    val schema = XsdSchema.build(schemaElem)
    val linkbase = Linkbase.build(linkbaseElem)

    TaxonomyBase.build(Vector(schema, linkbase))
  }

  "Linkbase references" should "be XLink simple links" in {
    val schema = taxonomyBase.rootElemUriMap(schemaUri)

    val linkbaseRefs = schema.findAllElemsOfType(classTag[LinkbaseRef])

    // In a similar manner, retrieve all XLink simple links in the schema (as descendant elements of the root)

    val simpleLinks: immutable.IndexedSeq[SimpleLink] = ???

    assertResult(true) {
      linkbaseRefs.map(_.backingElem.path).toSet.subsetOf(simpleLinks.map(_.backingElem.path).toSet)
    }
  }

  "A simple link" should "have xlink:type 'simple'" in {
    val schema = taxonomyBase.rootElemUriMap(schemaUri)

    val simpleLinks = schema.findAllElemsOfType(classTag[SimpleLink])

    val xlinkTypesOfSimpleLinks: Set[String] = ???

    assertResult(Set("simple")) {
      xlinkTypesOfSimpleLinks
    }
  }

  it should "have an xlink:href attribute" in {
    val schema = taxonomyBase.rootElemUriMap(schemaUri)

    val simpleLinks = schema.findAllElemsOfType(classTag[SimpleLink])

    val rawHrefs: Set[URI] = ???

    assertResult(true) {
      val someRawHrefs = Set(
        URI.create("rj-data-documentation-lab-nl.xml"),
        URI.create("rj-data-periodend-lab-en.xml"),
        URI.create("rj-data-terse-lab-en.xml"),
        URI.create("../../../venj/20161214/dictionary/venj-bw2-data-ref.xml"),
        URI.create("rj-venj-bw2-data-ref.xml"))

      someRawHrefs.subsetOf(rawHrefs)
    }
  }

  it should "have an absolute href after resolution against the 'base' URI" in {
    val schema = taxonomyBase.rootElemUriMap(schemaUri)

    val simpleLinks = schema.findAllElemsOfType(classTag[SimpleLink])

    val absoluteHrefs: Set[URI] = ???

    assertResult(true) {
      val someAbsoluteHrefs = Set(
        URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data-documentation-lab-nl.xml"),
        URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data-periodend-lab-en.xml"),
        URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data-terse-lab-en.xml"),
        URI.create("http://www.nltaxonomie.nl/nt11/venj/20161214/dictionary/venj-bw2-data-ref.xml"),
        URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-venj-bw2-data-ref.xml"))

      someAbsoluteHrefs.subsetOf(absoluteHrefs)
    }
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
