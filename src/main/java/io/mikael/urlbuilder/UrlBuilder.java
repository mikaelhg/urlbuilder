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

    /**
     * Construct a UrlBuilder from a full or partial URL string.
     * Assume that the query paremeters were percent-encoded, as the standard suggests, as UTF-8.
     */
    public static UrlBuilder fromString(final String url) {
        return fromString(url, DEFAULT_ENCODING);
    }

    /// Constructs a UrlBuilder from a full or partial URL string.
    ///
    /// When percent-decoding the query parameters, assumes that they were encoded with
    /// **inputEncoding**.
    ///
    /// @throws NumberFormatException if the input contains:
    ///
    ///         - An invalid percent-encoding sequence (%ax)
    ///         - A non-numeric port number
    ///
    public static UrlBuilder fromString(final String url, final String inputEncoding) {
        return fromString(url, Charset.forName(inputEncoding));
    }

    /// Constructs a `UrlBuilder` from a full or partial URL string.
    ///
    /// When percent-decoding query parameters, assumes they were encoded using
    /// the specified `inputEncoding`.
    ///
    /// @throws NumberFormatException if the input contains:
    ///
    ///         - An invalid percent-encoding sequence (e.g., `%ax`)
    ///         - A non-numeric port number
    ///
    public static UrlBuilder fromString(final String url, final Charset inputEncoding) {
        return fromString(url, new Decoder(inputEncoding));
    }

    /// Constructs a `UrlBuilder` from a full or partial URL string.
    ///
    /// Uses the provided decoder for percent-decoding query parameters.
    ///
    /// @throws NumberFormatException if the input contains:
    ///
    ///         - An invalid percent-encoding sequence (e.g., `%ax`)
    ///         - A non-numeric port number
    ///
    public static UrlBuilder fromString(String inputUri, final Decoder decoder) {
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

        final int firstColon = inputUri.indexOf(':'); // either for schema, password or port
        final int firstSlash = inputUri.indexOf('/');
        final String schema;
        if ((firstColon != -1 && firstColon < firstSlash) || (firstColon != -1 && firstSlash == -1)) {
            schema = inputUri.substring(0, firstColon);
            inputUri = inputUri.substring(firstColon + 1);
        } else {
            schema = null;
        }

        final int firstDoubleSlash = inputUri.indexOf("//");
        String authority;
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

        final int firstAtSign = authority != null ? authority.indexOf('@') : -1;
        String userInfo;
        // username (':' password)? '@'
        if (firstAtSign > -1) {
            userInfo = decoder.decodeUserInfo(authority.substring(0, firstAtSign));
            authority = authority.substring(firstAtSign + 1);
        } else {
            userInfo = null;
        }

        final int firstSquareBracketOpen = authority != null ? authority.indexOf('[') : -1;
        // [IPv6] | IPv4 | hostname
        String hostName = null;
        if (firstSquareBracketOpen > -1) {
            final int firstSquareBracketClosed = authority.indexOf(']');
            hostName = authority.substring(firstSquareBracketOpen, firstSquareBracketClosed + 1);
            authority = authority.substring(firstSquareBracketClosed + 1);
        }

        final int firstAuthorityColon = authority != null ? authority.indexOf(':') : -1;
        if (firstAuthorityColon > -1 && hostName == null) {
            hostName = authority.substring(0, firstAuthorityColon);
            authority = authority.substring(firstAuthorityColon);
        } else if (firstAuthorityColon == -1 && hostName == null) {
            hostName = authority;
            authority = null;
        }

        final String portString;
        // ':' port
        if (authority == null || authority.isEmpty()) {
            portString = null;
        } else {
            portString = authority.substring(1);
        }

        final Integer port;
        if (portString != null && !portString.isEmpty()) {
            port = Integer.parseUnsignedInt(portString);
        } else {
            port = null;
        }

        final String path = decoder.decodePath(inputUri);

        final Encoder encoder = new Encoder(DEFAULT_ENCODING);
        return of(decoder, encoder, schema, userInfo, hostName, port, path,
                decoder.parseQueryString(query), fragment);
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
    ///         - An invalid percent-encoding sequence (e.g., `%ax`)
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
