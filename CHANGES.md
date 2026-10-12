Changelog

## Unreleased

* Decoder.urlDecode no longer throws NumberFormatException on malformed percent-escapes (e.g. ``%zz``, ``%+1``); they are kept as literal text.

* Decoder.urlDecode is linear-time and returns the input unchanged when there is nothing to decode.

* Decoder.urlDecode is now also linear-time when escapes alternate with literal characters (e.g. ``%41a%41a...``), which was quadratic.

* **Behaviour change:** ``+`` in the user info is now kept as a literal ``+``; it was decoded as a space.

* **Behaviour change:** ``%2F`` and ``%25`` in a path are kept encoded by ``Decoder.decodePath`` (so ``UrlBuilder.path`` and ``fromString``), and passed through as-is by ``Encoder.encodePath`` (so ``withPath``). An encoded slash no longer turns into a path separator: ``http://h/a%2Fb`` used to come out as ``http://h/a/b``.

* **Behaviour change:** percent-escapes that are not valid in the input encoding (e.g. ``%FF`` in UTF-8) are kept as literal text, like malformed escapes; they used to decode to U+FFFD.

* Migrated Cucumber tests to JUnit 5

* UrlBuilder.fromString is now a hand-written single-pass scanner. Fixes: percent-escapes in the fragment are decoded (previously they were double-encoded on output); the user info ends at the last ``@``; ports must be 0-65535 ASCII digits (previously ``:4294967295`` became ``-1``); a scheme must match RFC 3986 syntax; ``null`` input throws a descriptive NullPointerException.

* UrlBuilder.fromString now throws IllegalArgumentException (previously NumberFormatException, or a misleading one) for an unterminated IPv6 literal, trailing characters after it, or an invalid or out-of-range port.

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
