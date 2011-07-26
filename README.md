Java URL builder
================

Create and modify URLs and URL parameters easily, with a builder class.

	UrlBuilder
	    .fromString("http://www.google.com/")
	    .addParameter("q", "charlie brown")
	    .toString() == "http://www.google.com/?q=charlie+brown"

        UrlBuilder
            .fromString("http://foo/h%F6pl%E4", "ISO-8859-1")
            .encodeAs("UTF-8")
            .toString() == "http://foo/h%C3%B6pl%C3%A4"
