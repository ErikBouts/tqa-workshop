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

package eu.cdevreeze.tqaworkshop

/**
 * Chapter 2 treats XLink, XML Base and XPointer (as used in XBRL), and introduces the corresponding parts of TQA.
 *
 * See [[https://github.com/dvreeze/tqa]] for TQA. XLink content is of type `XLinkElem` or one of its sub-types.
 * XPointer expressions are of type `XPointer` or one of its sub-types. These types all reside in the "DOM" package
 * of TQA. XML Base support is part of yaidom, and TQA uses that.
 *
 * XLink, XML Base and XPointer (as supported by TQA) are treated by exercises. No sample programs are given.
 *
 * @author Chris de Vreeze
 */
package object chapter2
