Java URL builder
================

[![Build Status](https://travis-ci.org/mikaelhg/urlbuilder.png)](https://travis-ci.org/mikaelhg/urlbuilder)

Create and modify URLs and URL parameters easily, with a builder class.

    UrlBuilder.fromString("http://www.google.com/")
        .addParameter("q", "charlie brown")
        .toString() == "http://www.google.com/?q=charlie+brown"

    UrlBuilder.fromString("http://foo/h%F6pl%E4", "ISO-8859-1")
        .encodeAs("UTF-8")
        .toString() == "http://foo/h%C3%B6pl%C3%A4"

    final UrlBuilder ub1 = UrlBuilder.fromEmpty()
        .withProtocol("http")
        .withHost("www.example.com")
        .withPath("/")
        .addParameter("foo", "bar");

    final java.net.URI uri1 = ub1.toUri();

    try {
        final java.net.URI uri2 = ub1.toUriWithException();
    } catch (java.net.URISyntaxException ex) {
        // handle the exception
    }

    final java.net.URL url1 = ub1.toUrl();

    try {
        final java.net.URL url2 = ub1.toUrlWithException();
    } catch (java.net.MalformedURLException ex) {
        // handle the exception
    }

Todo:
-----

* More unit tests for corner cases (your help is needed!)
* Preserve URL property order

Use with Maven:
-----------------------

    <repositories>
      <repository>
        <id>maven2.gueck.com-releases</id>
        <url>http://maven2.gueck.com/releases</url>
      </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>mikaelhg</groupId>
            <artifactId>urlbuilder</artifactId>
            <version>1.3.1</version>
        </dependency>
    </dependencies>
