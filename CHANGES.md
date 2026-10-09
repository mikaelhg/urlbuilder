Changelog

## Unreleased

* Decoder.urlDecode no longer throws NumberFormatException on malformed percent-escapes (e.g. ``%zz``, ``%+1``); they are kept as literal text.

* Decoder.urlDecode is linear-time and returns the input unchanged when there is nothing to decode.

## 2.0.9

* #41 and #42, make Encoder and Decoder methods public

* Upgrade build dependencies

## 2.0.0

* Fix #1 - add W3C tests to the test set.

* Fix various PENDING tests left over from pull request #27, notably unicode to unicode conversion of codepoints spanning multiple JVM ``char``s.

* Incorporate much improved encoding and decoding from @mfulgo's excellent pull request #27.

* Various fixes.

## 1.3.2 (2013-10-13)

* Fix #19 - Fix thinkos in remove methods demonstrated by kindly contributed unit tests by Yaroslav Karandashev

## 1.3.1 (2013-04-28)

* Fix #5 - Path characters gets double-encoded

## 1.3 (2013-04-28)

* Fix #2 - URL parameter ordering should be preserved, as much as possible
