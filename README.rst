============
TQA Workshop
============

This workshop introduces the `TQA`_ library, along with corresponding XBRL taxonomy topics.
Some XBRL knowledge is assumed, so the focus of the workshop is on learning TQA.

TQA is gently introduced through the use of code examples and exercises. The exercises have the form of unit tests that
must be made to succeed.

XBRL taxonomies are treated from the bottom-up, starting with plain XML and ending with taxonomy schemas and linkbases.
For each of these XML and XBRL topics, the relevant parts of TQA and the underlying `yaidom`_ API are treated.

.. _`TQA`: https://github.com/dvreeze/tqa
.. _`yaidom`: https://github.com/dvreeze/yaidom


Preparation
===========

Fork this (Scala SBT) project from Github, at `TQA Workshop`_. Make sure to have an installation of `SBT`_ available.

Also download the API documentation of the TQA and yaidom libraries from `Maven central`_.

.. _`TQA Workshop`: https://github.com/dvreeze/tqa-workshop
.. _`SBT`: http://www.scala-sbt.org/download.html
.. _`Maven central`: https://search.maven.org/


Chapter 1
=========

Chapter 1 introduces the yaidom XML querying (Scala) library. TQA is based on yaidom, so yaidom needs to be introduced
first. It may be handy to have a look at some `yaidom documentation`_ first.

Study program ShowXbrlInstanceElemAsMatrix in the source tree for chapter 1. It uses Scala class XbrlInstanceElemToRowsConverter.
Study that class too, concentrating on its usage of yaidom query API calls (having the yaidom API documentation nearby).
Compile and run the ShowXbrlInstanceElemAsMatrix program.

In the test source tree for chapter 1, fill in the missing parts in tests QuerySpec and QueryApiAsTheorySpec, making the
tests run successfully. This gives enough basic understanding of yaidom for the following chapters.

.. _`yaidom documentation`: http://dvreeze.github.io/ 
