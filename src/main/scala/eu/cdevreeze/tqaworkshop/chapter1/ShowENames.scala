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

import java.io.File

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.print.DocumentPrinterUsingDom
import eu.cdevreeze.yaidom.simple.Elem

/**
 * This program shows the (yaidom) ENames and Scopes of all elements occurring in an input XML.
 * These ENames and Scopes are shown by adding attributes "yaidom-ename" and "yaidom-scope" to all
 * elements in the tree.
 *
 * For readability, copy and paste the program XML output into a file, and open this file in a browser.
 *
 * This program has educational value only. It shows the correspondence between QNames and ENames,
 * given a Scope. The QName is the visible lexical QName of the element. (Attributes are ignored in
 * this context.)
 *
 * It also shows the correspondence between Scopes, parent element Scopes and namespace declarations.
 *
 * Note that what the XML Namespaces specification calls "expanded name" is often referred to as
 * "qualified name". Yet this is not the same as the lexical qualified name that can be used in XML documents.
 * Hence when someone talks about "qualified names", always consider if expanded names are meant,
 * or lexical qualified names. For example, the Java JDK `QName` class represents expanded names.
 *
 * For a very good introduction to XML Namespaces, see [[http://www.lenzconsulting.com/namespaces/]].
 * This article uses the terms "expanded name" and "qualified name" correctly.
 *
 * The caller of this program is encouraged to use an alternative to yaidom to implement the
 * same functionality, such that the solution is as precise as this one, and at least as easy to
 * implement (in Scala or Java).
 *
 * @author Chris de Vreeze
 */
object ShowENames {

  def main(args: Array[String]): Unit = {
    // The program may get one argument for the XML input file path, taking a default otherwise

    require(args.size <= 1, s"Usage: ShowENames [ <XML input file path> ]")

    val inputXmlFile =
      if (args.isEmpty) {
        val inputFileUri = classOf[ShowENames].getResource("/sample-Instance-Proof.xml").toURI
        new File(inputFileUri)
      } else {
        new File(args(0))
      }

    // We are going to parse the document as native yaidom "simple" Document.

    val docParser = DocumentParserUsingSax.newInstance()
    val docPrinter = DocumentPrinterUsingDom.newInstance()

    val doc = docParser.parse(inputXmlFile)

    // Add the yaidom-scope and yaidom-ename attributes.

    def enrich(elm: Elem): Elem = {
      elm.plusAttribute(QName("yaidom-scope"), showScope(elm.scope)).
        plusAttribute(QName("yaidom-ename"), elm.resolvedName.toString)
    }

    val enrichedDoc =
      doc.withDocumentElement(doc.documentElement.transformElemsOrSelf(enrich).prettify(2))

    val enrichedXmlString = docPrinter.print(enrichedDoc)

    println(enrichedXmlString)
  }

  private def showScope(scope: Scope): String = {
    val declsString = Declarations.from(scope.prefixNamespaceMap).toStringInXml

    require(declsString.trim == declsString)
    require(declsString.isEmpty || declsString.endsWith("\""))

    if (declsString.isEmpty) {
      declsString
    } else {
      declsString.dropRight(1).replace("\" ", "' ").replace("=\"", "='") + "'"
    }
  }
}

class ShowENames
