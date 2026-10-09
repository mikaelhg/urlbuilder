/*
Copyright 2014 Mikael Gueck

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package io.mikael.urlbuilder;

import io.mikael.urlbuilder.util.*;

import static io.mikael.urlbuilder.util.UrlParameterMultimap.*;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/// A utility class for building and manipulating URLs.
///
/// Instances of this class are immutable after construction.
///
/// References:
///
///     - URL specification: <a href="https://tools.ietf.org/html/rfc1738">RFC 1738</a>
///     - URI specification: <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a>
///
/// @author Mikael Gueck {@literal <gumi@iki.fi>}
public final class UrlBuilder {

    private static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

    private final Decoder decoder;

    private final Encoder encoder;

    public final String scheme;

    public final String userInfo;

    public final String hostName;

    public final Integer port;

    public final String path;

    public final Map<String, List<String>> queryParameters;

    private final UrlParameterMultimap.Immutable queryParametersMultimap;

    public final String fragment;

    private UrlBuilder() {
        this(null, null, null, null, null, null, null, null, null);
    }

    private UrlBuilder(
        final Decoder decoder,
        final Encoder encoder,
        final String scheme,
        final String userInfo,
        final String hostName,
        final Integer port,
        final String path,
        final UrlParameterMultimap queryParametersMultimap,
        final String fragment
    ) {
        this.decoder = Objects.requireNonNullElseGet(decoder, () -> new Decoder(DEFAULT_ENCODING));
        this.encoder = Objects.requireNonNullElseGet(encoder, () -> new Encoder(DEFAULT_ENCODING));
        this.scheme = scheme;
        this.userInfo = userInfo;
        this.hostName = hostName;
        this.port = port;
        this.path = path;
        this.queryParametersMultimap = Objects.requireNonNullElseGet(
                queryParametersMultimap, UrlParameterMultimap::newMultimap).immutable();
        this.queryParameters = this.queryParametersMultimap;
        this.fragment = fragment;
    }

    /**
     * Construct an empty builder instance.
     */
    public static UrlBuilder empty() {
        return new UrlBuilder();
    }

    private static UrlBuilder of(
        final Decoder decoder,
        final Encoder encoder,
        final String scheme,
        final String userInfo,
        final String hostName,
        final Integer port,
        final String path,
        final UrlParameterMultimap queryParameters,
        final String fragment
    ) {
        return new UrlBuilder(decoder, encoder, scheme, userInfo, hostName, port, path, queryParameters, fragment);
    }

    /// Constructs a `UrlBuilder` from a full or partial URL string.
    ///
    /// Assumes that the URL was percent-encoded as UTF-8, as the standard suggests.
    ///
    /// @throws NullPointerException if `url` is null
    /// @throws IllegalArgumentException if the authority is malformed; see
    ///         {@link #fromString(String, Decoder)}
    public static UrlBuilder fromString(final String url) {
        return fromString(url, DEFAULT_ENCODING);
    }

    /// Constructs a `UrlBuilder` from a full or partial URL string.
    ///
    /// Assumes that the URL was percent-encoded with `inputEncoding`.
    ///
    /// @throws NullPointerException if `url` is null
    /// @throws IllegalArgumentException if the authority is malformed; see
    ///         {@link #fromString(String, Decoder)}
    public static UrlBuilder fromString(final String url, final String inputEncoding) {
        return fromString(url, Charset.forName(inputEncoding));
    }

    /// Constructs a `UrlBuilder` from a full or partial URL string.
    ///
    /// Assumes that the URL was percent-encoded with `inputEncoding`.
    ///
    /// @throws NullPointerException if `url` is null
    /// @throws IllegalArgumentException if the authority is malformed; see
    ///         {@link #fromString(String, Decoder)}
    public static UrlBuilder fromString(final String url, final Charset inputEncoding) {
        return fromString(url, new Decoder(inputEncoding));
    }

    /// Constructs a `UrlBuilder` from a full or partial URL string.
    ///
    /// Uses the provided decoder for percent-decoding the user info, path, query and fragment.
    /// Malformed percent-escapes are kept as literal text.
    ///
    /// The string is scanned left to right, without backtracking:
    ///
    ///     - The fragment starts at the first `#`, the query at the first `?` before that.
    ///     - A scheme is a letter followed by letters, digits, `+`, `-` or `.`, terminated by `:`.
    ///       Note that `host:8080/x` therefore parses with the scheme `host`.
    ///     - If `//` follows, the authority extends to the next `/` or the end of the input.
    ///     - The user info ends at the last `@` in the authority.
    ///     - The port is 0-65535, in ASCII digits.
    ///
    /// @throws NullPointerException if `inputUri` or `decoder` is null
    /// @throws IllegalArgumentException if the authority contains an unterminated or
    ///         trailing-garbage IPv6 literal, or a port that is not a number from 0 to 65535
    public static UrlBuilder fromString(final String inputUri, final Decoder decoder) {
        Objects.requireNonNull(inputUri, "inputUri");
        Objects.requireNonNull(decoder, "decoder");

        int end = inputUri.length();

        final String fragment;
        final int hash = inputUri.indexOf('#');
        if (hash != -1) {
            fragment = hash + 1 < end ? decoder.decodeFragment(inputUri.substring(hash + 1)) : null;
            end = hash;
        } else {
            fragment = null;
        }

        final String query;
        final int questionMark = inputUri.indexOf('?');
        if (questionMark != -1 && questionMark < end) {
            query = questionMark + 1 < end ? inputUri.substring(questionMark + 1, end) : null;
            end = questionMark;
        } else {
            query = null;
        }

        int pos = 0;
        final String scheme;
        final int schemeEnd = schemeLength(inputUri, end);
        if (schemeEnd > 0) {
            scheme = inputUri.substring(0, schemeEnd);
            pos = schemeEnd + 1;
        } else {
            scheme = null;
        }

        String userInfo = null;
        String hostName = null;
        Integer port = null;
        if (inputUri.startsWith("//", pos)) {
            final int authorityStart = pos + 2;
            int authorityEnd = inputUri.indexOf('/', authorityStart);
            if (authorityEnd == -1 || authorityEnd > end) {
                authorityEnd = end;
            }
            pos = authorityEnd;

            int hostStart = authorityStart;
            final int at = inputUri.lastIndexOf('@', authorityEnd - 1);
            if (at >= authorityStart) {
                userInfo = decoder.decodeUserInfo(inputUri.substring(authorityStart, at));
                hostStart = at + 1;
            }

            final int hostEnd;
            int portStart = -1;
            if (hostStart < authorityEnd && inputUri.charAt(hostStart) == '[') {
                final int close = inputUri.indexOf(']', hostStart);
                if (close == -1 || close >= authorityEnd) {
                    throw new IllegalArgumentException("Unterminated IPv6 literal in authority");
                }
                hostEnd = close + 1;
                if (hostEnd < authorityEnd) {
                    if (inputUri.charAt(hostEnd) != ':') {
                        throw new IllegalArgumentException("Unexpected characters after IPv6 literal");
                    }
                    portStart = hostEnd + 1;
                }
            } else {
                int colon = inputUri.indexOf(':', hostStart);
                if (colon == -1 || colon >= authorityEnd) {
                    hostEnd = authorityEnd;
                } else {
                    hostEnd = colon;
                    portStart = colon + 1;
                }
            }
            hostName = inputUri.substring(hostStart, hostEnd);
            if (portStart != -1) {
                port = parsePort(inputUri, portStart, authorityEnd);
            }
        }

        final String path = decoder.decodePath(inputUri.substring(pos, end));

        return of(decoder, new Encoder(DEFAULT_ENCODING), scheme, userInfo, hostName, port, path,
                decoder.parseQueryString(query), fragment);
    }

    /// Returns the length of the scheme at the start of `s[0, end)`, or 0 if there is none.
    private static int schemeLength(final String s, final int end) {
        if (end == 0 || !isAsciiLetter(s.charAt(0))) {
            return 0;
        }
        for (int i = 1; i < end; i++) {
            final char c = s.charAt(i);
            if (c == ':') {
                return i;
            } else if (!isAsciiLetter(c) && !(c >= '0' && c <= '9') && c != '+' && c != '-' && c != '.') {
                return 0;
            }
        }
        return 0;
    }

    private static boolean isAsciiLetter(final char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /// Parses `s[start, end)` as a port number; an empty range means no port.
    private static Integer parsePort(final String s, final int start, final int end) {
        if (start == end) {
            return null;
        }
        if (end - start > 5) {
            throw new IllegalArgumentException("Invalid port in authority");
        }
        int port = 0;
        for (int i = start; i < end; i++) {
            final char c = s.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("Invalid port in authority");
            }
            port = port * 10 + (c - '0');
        }
        if (port > 65535) {
            throw new IllegalArgumentException("Port out of range: " + port);
        }
        return port;
    }

    /**
     * Constructs a {@link UrlBuilder} from a {@link java.net.URI}.
     */
    public static UrlBuilder fromUri(final URI uri) {
        final Decoder decoder = new Decoder(DEFAULT_ENCODING);
        return of(decoder, new Encoder(DEFAULT_ENCODING),
                uri.getScheme(), uri.getUserInfo(), uri.getHost(),
                uri.getPort() == -1 ? null : uri.getPort(),
                decoder.decodePath(uri.getRawPath()),
                decoder.parseQueryString(uri.getRawQuery()),
                decoder.decodeFragment(uri.getFragment()));
    }

    /// Constructs a [UrlBuilder] from a [java.net.URL].
    ///
    /// @throws NumberFormatException if the URL contains:
    ///
    ///         - A non-numeric port number
    ///
    public static UrlBuilder fromUrl(final URL url) {
        final Decoder decoder = new Decoder(DEFAULT_ENCODING);
        return of(decoder, new Encoder(DEFAULT_ENCODING),
                url.getProtocol(), url.getUserInfo(), url.getHost(),
                url.getPort() == -1 ? null : url.getPort(),
                decoder.decodePath(url.getPath()),
                decoder.parseQueryString(url.getQuery()),
                decoder.decodeFragment(url.getRef()));
    }

    public void toString(final Appendable out) throws IOException {
        if (null != this.scheme) {
            out.append(this.scheme);
            out.append(':');
        }
        if (null != this.hostName) {
            out.append("//");
            if (this.userInfo != null) {
                out.append(encoder.encodeUserInfo(this.userInfo));
                out.append('@');
            }
            out.append(IDN.toASCII(this.hostName));
        }
        if (null != this.port) {
            out.append(':');
            out.append(Integer.toString(this.port));
        }
        if (null != this.path) {
            if (null != this.hostName && !this.path.isEmpty() && this.path.charAt(0) != '/') {
                /* RFC 3986 section 3.3: If a URI contains an authority component, then the path component
                   must either be empty or begin with a slash ("/") character. */
                out.append('/');
            }
            out.append(encoder.encodePath(this.path));
        }
        if (null != this.queryParametersMultimap && !this.queryParametersMultimap.isEmpty()) {
            out.append('?');
            out.append(encoder.encodeQueryParameters(queryParametersMultimap));
        }
        if (null != this.fragment) {
            out.append('#');
            out.append(encoder.encodeFragment(this.fragment));
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        try {
            this.toString(sb);
        } catch (final IOException ex) {
            // will never happen, with StringBuilder
        }
        return sb.toString();
    }

    public URI toUriWithException() throws URISyntaxException {
        return new URI(this.toString());
    }

    public URI toUri() throws RuntimeURISyntaxException {
        try {
            return toUriWithException();
        } catch (final URISyntaxException e) {
            throw new RuntimeURISyntaxException(e);
        }
    }

    public URL toUrlWithException() throws MalformedURLException {
        // Keep the deprecated URL constructor until there's a real solution
        // to the incompatibilities between URL and URI.
        return new URL(this.toString());
    }

    public URL toUrl() throws RuntimeMalformedURLException {
        try {
            return toUrlWithException();
        } catch (final MalformedURLException e) {
            throw new RuntimeMalformedURLException(e);
        }
    }

    public UrlBuilder withDecoder(final Decoder decoder) {
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    public UrlBuilder withEncoder(final Encoder encoder) {
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * When percent-escaping the StringBuilder's output, use this character set.
     */
    public UrlBuilder encodeAs(final Charset charset) {
        final Encoder encoder = new Encoder(charset);
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * When percent-escaping the StringBuilder's output, use this character set.
     */
    public UrlBuilder encodeAs(final String charsetName) {
        final Encoder encoder = new Encoder(Charset.forName(charsetName));
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * Set the protocol (or scheme), such as "http" or "https".
     */
    public UrlBuilder withScheme(final String scheme) {
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * Set the userInfo. It's usually either of the form "username" or "username:password".
     */
    public UrlBuilder withUserInfo(final String userInfo) {
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * Set the host name. Accepts internationalized host names, and decodes them.
     */
    public UrlBuilder withHost(final String name) {
        final String hostName = IDN.toUnicode(name);
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /// Sets the port number.
    ///
    /// Use `null` to indicate the protocol's default port.
    public UrlBuilder withPort(final Integer port) {
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * Set the decoded, non-url-encoded path.
     */
    public UrlBuilder withPath(final String path) {
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * Decodes and sets the path from a url-encoded string.
     */
    public UrlBuilder withPath(final String path, final Charset encoding) {
        final Decoder pathDecoder = new Decoder(encoding);
        return of(decoder, encoder, scheme, userInfo, hostName, port, pathDecoder.decodePath(path), queryParametersMultimap, fragment);
    }

    /**
     * Decodes and sets the path from a url-encoded string.
     */
    public UrlBuilder withPath(final String path, final String encoding) {
        return withPath(path, Charset.forName(encoding));
    }

    /// Sets the query parameters to a deep copy of the specified parameters.
    ///
    /// Passing `null` will remove the entire query section.
    ///
    /// @param query the query parameters to copy (may be `null`)
    public UrlBuilder withQuery(final UrlParameterMultimap query) {
        final UrlParameterMultimap q;
        if (query == null) {
            q = newMultimap();
        } else {
            q = query.deepCopy();
        }
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, q, fragment);
    }

    /**
     * Decodes the input string, and sets the query string.
     */
    public UrlBuilder withQuery(final String query) {
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, decoder.parseQueryString(query), fragment);
    }

    /**
     * Decodes the input string, and sets the query string.
     */
    public UrlBuilder withQuery(final String query, final Charset encoding) {
        final Decoder queryDecoder = new Decoder(encoding);
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryDecoder.parseQueryString(query), fragment);
    }

    /**
     * Sets the parameters.
     */
    public UrlBuilder withParameters(final UrlParameterMultimap parameters) {
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, parameters, fragment);
    }

    /**
     * Adds a query parameter. New parameters are added to the end of the query string.
     */
    public UrlBuilder addParameter(final String key, final String value) {
        final UrlParameterMultimap qp = queryParametersMultimap.deepCopy().add(key, value);
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, qp, fragment);
    }

    /**
     * Replaces a query parameter.
     * Existing parameters with this name are removed, and the new one added to the end of the query string.
     */
    public UrlBuilder setParameter(final String key, final String value) {
        final UrlParameterMultimap qp = queryParametersMultimap.deepCopy().replaceValues(key, value);
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, qp, fragment);
    }

    /**
     * Removes a query parameter for a key and value.
     */
    public UrlBuilder removeParameter(final String key, final String value) {
        final UrlParameterMultimap qp = queryParametersMultimap.deepCopy().remove(key, value);
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, qp, fragment);
    }

    /**
     * Removes all query parameters with this key.
     */
    public UrlBuilder removeParameters(final String key) {
        final UrlParameterMultimap qp = queryParametersMultimap.deepCopy().removeAllValues(key);
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, qp, fragment);
    }

    /**
     * Sets the fragment/anchor.
     */
    public UrlBuilder withFragment(final String fragment) {
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * Add URI path segments.
     */
    public UrlBuilder addPathSegments(final String ... pathSegments) {
        final StringBuilder sb = new StringBuilder(this.path);
        for (final String p : pathSegments) {
            final char lastChar = sb.charAt(sb.length() - 1);
            final char firstChar = p.charAt(0);
            if ('/' == lastChar && '/' == firstChar) {
                sb.append(p.substring(1));
            } else if ('/' == lastChar || '/' == firstChar) {
                sb.append(p);
            } else {
                sb.append('/');
                sb.append(p);
            }
        }
        final String path = sb.toString();
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

}
