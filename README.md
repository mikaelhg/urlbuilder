Java URL builder
================

[![Build Status](https://travis-ci.org/mikaelhg/urlbuilder.png?branch=master)](https://travis-ci.org/mikaelhg/urlbuilder)
[![Coverage Status](https://coveralls.io/repos/mikaelhg/urlbuilder/badge.svg?branch=master)](https://coveralls.io/r/mikaelhg/urlbuilder?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/io.mikael/urlbuilder.svg)](https://repo1.maven.org/maven2/io/mikael/urlbuilder/)

Create and modify URLs and URL parameters easily, with a builder class.

Builder instances are immutable, thread-safe and reusable. Every change creates a new instance.

```java
UrlBuilder.fromString("http://www.google.com/")
    .addParameter("q", "charlie brown")
    .toString() == "http://www.google.com/?q=charlie+brown"

UrlBuilder.fromString("http://foo/h%F6pl%E4", "ISO-8859-1")
    .encodeAs("UTF-8")
    .toString() == "http://foo/h%C3%B6pl%C3%A4"

final UrlBuilder ub1 = UrlBuilder.empty()
    .withScheme("http")
    .withHost("www.example.com")
    .withPath("/")
    .addParameter("foo", "bar");

final java.net.URI uri1 = ub1.toUri();

try {
    final java.net.URI uri2 = ub1.toUriWithException();
} catch (java.net.URISyntaxException ex) {
    // handle the exception
}

final java.net.URL url1 = ub1.toUrl();

try {
    final java.net.URL url2 = ub1.toUrlWithException();
} catch (java.net.MalformedURLException ex) {
    // handle the exception
}
```

Todo:
-----

* More unit tests for corner cases. Please send in pull requests, your help is needed.

Use with Gradle:
-----------------------

```kotlin
implementation("io.mikael:urlbuilder:2.0.9")
```

```groovy
implementation "io.mikael:urlbuilder:2.0.9"
```

Use with Maven:
-----------------------

```xml
<dependencies>
    <dependency>
        <groupId>io.mikael</groupId>
        <artifactId>urlbuilder</artifactId>
        <version>2.0.9</version>
    </dependency>
</dependencies>
```
