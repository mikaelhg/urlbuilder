package io.mikael.urlbuilder;

import org.junit.jupiter.api.Test;

import java.util.*;

public class HandParserTests {

    @Test
    public void handParserTest() {

        for (final String uri : testUris) {
            final UrlBuilder builder = handParser(uri);
            if (!uri.equals(builder.toString())) {
                // final java.net.URI jnu = java.net.URI.create(uri);
                System.out.printf("<%s> = <%s>%n", uri, builder);
            }
        }

    }

    public UrlBuilder handParser(String inputUri) {
        UrlBuilder builder = UrlBuilder.empty();

        final int firstPound = inputUri.indexOf('#');
        final String fragment;
        if (firstPound != -1) {
            if (inputUri.length() > firstPound + 1) {
                fragment = inputUri.substring(firstPound + 1);
            } else {
                fragment = null;
            }
            inputUri = inputUri.substring(0, firstPound);
        } else {
            fragment = null;
        }

        builder = builder.withFragment(fragment);

        final int firstQuestionMark = inputUri.indexOf('?');
        final String query;
        if (firstQuestionMark != -1) {
            if (inputUri.length() > firstQuestionMark + 1) {
                query = inputUri.substring(firstQuestionMark + 1);
            } else {
                query = null;
            }
            inputUri = inputUri.substring(0, firstQuestionMark);
        } else {
            query = null;
        }

        builder = builder.withQuery(query);

        final int firstColon = inputUri.indexOf(':'); // either for schema, password or port
        final int firstSlash = inputUri.indexOf('/');
        final String schema;
        if ((firstColon != -1 && firstColon < firstSlash) || (firstColon != -1 && firstSlash == -1)) {
            schema = inputUri.substring(0, firstColon);
            inputUri = inputUri.substring(firstColon + 1);
        } else {
            schema = null;
        }

        builder = builder.withScheme(schema);

        final int firstDoubleSlash = inputUri.indexOf("//");
        final String authority;
        if (firstDoubleSlash == 0) {
            final int nextSlash = inputUri.indexOf('/', 2);
            if (nextSlash != -1) {
                authority = inputUri.substring(2, nextSlash);
                inputUri = inputUri.substring(nextSlash);
            } else {
                authority = inputUri.substring(2);
                inputUri = "";
            }
        } else {
            authority = null;
        }

        builder = handAuthorityParser(authority, builder);

        final String path = inputUri;
        builder = builder.withPath(path);

        return builder;
    }

    public UrlBuilder handAuthorityParser(String inputAuthority, UrlBuilder builder) {
        if (inputAuthority == null) {
            return builder;
        }

        final int firstAtSign = inputAuthority.indexOf('@');
        String usernamePassword;
        // username (':' password)? '@'
        if (firstAtSign > -1) {
            usernamePassword = inputAuthority.substring(0, firstAtSign);
            inputAuthority = inputAuthority.substring(firstAtSign + 1);
        } else {
            usernamePassword = null;
        }

        // System.out.printf("usernamePassword: <%s>%n", usernamePassword);
        if (usernamePassword != null && !usernamePassword.isEmpty()) {
            builder = builder.withUserInfo(usernamePassword);
        }

        final int firstSquareBracketOpen = inputAuthority.indexOf('[');
        // [IPv6] | IPv4 | hostname
        String hostname = null;
        if (firstSquareBracketOpen > -1) {
            final int firstSquareBracketClosed = inputAuthority.indexOf(']');
            hostname = inputAuthority.substring(firstSquareBracketOpen, firstSquareBracketClosed + 1);
            inputAuthority = inputAuthority.substring(firstSquareBracketClosed + 1);
        }

        final int firstColon = inputAuthority.indexOf(':');
        if (firstColon > -1 && hostname == null) {
            hostname = inputAuthority.substring(0, firstColon);
            inputAuthority = inputAuthority.substring(firstColon);
        } else if (firstColon == -1 && hostname == null) {
            hostname = inputAuthority;
            inputAuthority = "";
        }

        // System.out.printf("hostname: <%s>%n", hostname);

        if (!hostname.isEmpty()) {
            builder = builder.withHost(hostname);
        }

        final String port;
        // ':' port
        if (inputAuthority.isEmpty()) {
            port = null;
        } else {
            port = inputAuthority.substring(1);
        }

        // System.out.printf("port: <%s>%n", port);

        if (port != null && !port.isEmpty()) {
            builder = builder.withPort(Integer.parseInt(port));
        }

        return builder;
    }

    public void handQueryParser(String inputQuery) {
        if (inputQuery == null || inputQuery.isEmpty()) {
            return;
        }
        final List<String> segments = new ArrayList<>(8);
        int nextAmpersand;
        while ((nextAmpersand = inputQuery.indexOf('&')) > -1) {
            final String segment = inputQuery.substring(0, nextAmpersand);
            inputQuery = inputQuery.substring(nextAmpersand + 1);
            segments.add(segment);
        }
        segments.add(inputQuery);

        System.out.printf("segments: <%s>%n", segments);

        final List<Map.Entry<String, String>> keyValuePairs = new ArrayList<>(8);
        for (String segment : segments) {
            final int firstEquals = segment.indexOf('=');
            if (firstEquals > -1) {
                keyValuePairs.add(new AbstractMap.SimpleEntry<>(
                        segment.substring(0, firstEquals),
                        segment.substring(firstEquals + 1)));
            } else {
                keyValuePairs.add(new AbstractMap.SimpleEntry<>(segment, null));
            }
        }

        System.out.printf("keyValuePairs: <%s>%n", keyValuePairs);
    }

    @Test
    public void hostOrPath() {
        final java.net.URI uri = java.net.URI.create("//example.com/path");
        System.out.println(uri.getRawAuthority());
        System.out.println(uri.getRawPath());
    }

    String[] testUris = {

            "/%2F/%2F/%2F",

            // Basic cases
            "http://example.com",
            "https://example.com/path",
            "ftp://user:pass@example.com:21",
            "mailto:user@example.com",

            // Partial URIs
            "//example.com/path",  // network-path reference
            "/relative/path",      // absolute-path reference
            "relative/path",       // relative-path reference
            "?query=string",       // query only
            "#fragment",           // fragment only

            // Edge cases with special characters
            "http://example.com/with space",
            "http://example.com/with%20encoded",
            "http://example.com/with+plus",
            "http://example.com/with?query=value&another=value",
            "http://example.com/with#frag?ment",

            // IPv4 addresses
            "http://192.168.1.1",
            "http://127.0.0.1:8080/path",

            // IPv6 addresses (various formats)
            "http://[2001:0db8:85a3:0000:0000:8a2e:0370:7334]",
            "http://[2001:db8::1]:8080",
            "http://[::1]",
            "http://[::]",
            "http://[2001:db8::8a2e:370:7334]/path",
            "http://[fe80::1%en0]",  // with zone index
            "http://user:pass@[::1]:8080/path?query#frag",

            // Minimal cases
            "a:b",                 // minimal valid URI
            "a:",                  // scheme only
            ":",                   // most minimal possible (technically valid)

            // Unusual but valid schemes
            "urn:isbn:0451450523",
            "tel:+1-800-555-1234",

            // Authentication edge cases
            "http://@example.com", // empty username
            "http://:@example.com", // empty username and password
            "http://user@example.com", // username only
            "http://:pass@example.com", // password only

            // Port edge cases
            "http://example.com:", // empty port
            "http://example.com:0", // min port
            "http://example.com:65535", // max port

            // Path edge cases
            "",                    // empty string
            ".",                   // single dot
            "..",                  // double dot
            "/.",                  // path with dot
            "/..",                 // path with parent
            "/...",                // path with multiple dots
            "/path/./to/../file",  // path with . and ..

            // Query and fragment edge cases
            "http://example.com?", // empty query
            "http://example.com#", // empty fragment
            "http://example.com?#", // both empty
            "http://example.com?query#fragment",

            // Percent encoding
            "http://example.com/%7Euser",
            "http://example.com/%41%42%43", // ABC
            "http://example.com/%00",       // null byte

            // International domain names
            "http://例子.测试",
            "http://xn--fsqu00a.xn--0zwm56d",

            // Mixed case
            "HTTP://Example.COM/Path/To/Resource?Query=Value#Fragment",
            "HtTpS://User:Pass@Example.Com:8080"
    };


}
