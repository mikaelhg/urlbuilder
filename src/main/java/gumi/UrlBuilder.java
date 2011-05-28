package gumi;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URL: http://www.ietf.org/rfc/rfc1738.txt
 * URI: http://tools.ietf.org/html/rfc3986
 * @author Mikael Gueck gumi@iki.fi
 */
public class UrlBuilder implements Cloneable {

    private static final Pattern URI_PATTERN =
            Pattern.compile("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");

    private static final Pattern AUTHORITY_PATTERN =
            Pattern.compile("(.*)(?:([0-9]*))");

    public static final String DEFAULT_SCHEME = "http";

    private volatile String inputEncodingName = "UTF-8";

    private volatile String outputEncodingName = "UTF-8";

    private volatile String protocol = DEFAULT_SCHEME;

    private volatile String hostName;

    private volatile Integer port;

    private volatile String path;

    private volatile ConcurrentMap<String, ArrayList<String>> queryParameters =
            new ConcurrentHashMap<String, ArrayList<String>>();

    private volatile String anchor;


    private UrlBuilder() {
    }


    public UrlBuilder clone() {
        final UrlBuilder ret = new UrlBuilder();
        ret.inputEncodingName = this.inputEncodingName;
        ret.outputEncodingName = this.outputEncodingName;
        ret.protocol = this.protocol;
        ret.hostName = this.hostName;
        ret.port = this.port;
        ret.path = this.path;
        ret.queryParameters = new ConcurrentHashMap<String, ArrayList<String>>(this.queryParameters);
        ret.anchor = this.anchor;
        return ret;
    }

    public static UrlBuilder fromString(final String url) {
        return fromString(url, "UTF-8");
    }

    public static UrlBuilder fromString(final String url, final String inputEncoding) {
        final UrlBuilder ret = new UrlBuilder();
        ret.inputEncodingName = inputEncoding;
        final Matcher m = URI_PATTERN.matcher(url);
        if (m.find()) {
            ret.protocol = m.group(2);
            final Matcher n = AUTHORITY_PATTERN.matcher(m.group(4));
            if (n.find()) {
                ret.hostName = n.group(1);
                if (n.groupCount() > 1) {
                    ret.port = Integer.parseInt(n.group(3));
                }
            }
            ret.path = m.group(5);
            ret.queryParameters = ret.decodeQueryParameters(m.group(7));
            ret.anchor = m.group(9);
        }
        return ret;
    }

    public static UrlBuilder fromUri(final URI uri) {
        try {
            return fromUrl(uri.toURL());
        } catch (MalformedURLException e) {
            return new UrlBuilder();
        }
    }

    public static UrlBuilder fromUrl(final URL url) {
        final UrlBuilder ret = new UrlBuilder();
        ret.protocol = url.getProtocol();
        ret.hostName = url.getHost();
        ret.port = url.getPort();
        ret.path = url.getPath();
        ret.queryParameters = ret.decodeQueryParameters(url.getQuery());
        ret.anchor = url.getRef();
        return ret;
    }

    private ConcurrentMap<String, ArrayList<String>> decodeQueryParameters(final String query) {
        final ConcurrentMap<String, ArrayList<String>> ret = new ConcurrentHashMap<String, ArrayList<String>>();
        for (final String part : query.split("&")) {
            final String[] kvp = part.split("=", 2);
            String key, value;
            try {
                key = URLDecoder.decode(kvp[0], this.inputEncodingName);
            } catch (final UnsupportedEncodingException e) {
                key = kvp[0];
            }
            if (kvp.length == 2) {
                try {
                    value = URLDecoder.decode(kvp[1], this.inputEncodingName);
                } catch (final UnsupportedEncodingException e) {
                    value = kvp[1];
                }
            } else {
                value = null;
            }
            final ArrayList<String> valueList;
            if (ret.containsKey(key)) {
                valueList = ret.get(key);
            } else {
                valueList = new ArrayList<String>();
                ret.put(key, valueList);
            }
            valueList.add(value);
        }
        return ret;
    }

    private String encodeQueryParameters() {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, ArrayList<String>> e : this.queryParameters.entrySet()) {
            for (final String value : e.getValue()) {
                try {
                    sb.append(URLEncoder.encode(e.getKey(), this.outputEncodingName));
                } catch (final UnsupportedEncodingException ex) {
                    sb.append(e.getKey());
                }
                if (value != null) {
                    sb.append('=');
                    try {
                        sb.append(URLEncoder.encode(value, this.outputEncodingName));
                    } catch (final UnsupportedEncodingException ex) {
                        sb.append(value);
                    }
                }
                sb.append('&');
            }
        }
        sb.deleteCharAt(sb.length());
        return sb.toString();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (this.protocol == null) {
            sb.append(DEFAULT_SCHEME);
        } else {
            sb.append(this.protocol);
        }
        sb.append("://");
        if (this.hostName != null) {
            sb.append(this.hostName);
        }
        if (this.port != null) {
            sb.append(':');
            sb.append(this.port);
        }
        if (this.path != null) {
            sb.append(this.path);
        }
        if (!this.queryParameters.isEmpty()) {
            sb.append('?');
            sb.append(this.encodeQueryParameters());
        }
        if (this.anchor != null) {
            sb.append('#');
            sb.append(this.anchor);
        }
        return sb.toString();
    }

    public URI toUriWithException() throws URISyntaxException {
        return new URI(this.toString());
    }

    public URI toUri() {
        try {
            return toUriWithException();
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public URL toUrlWithException() throws MalformedURLException {
        return new URL(this.toString());
    }

    public URL toUrl() {
        try {
            return toUrlWithException();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }


    public UrlBuilder encodeAs(final Charset charset) {
        final UrlBuilder ret = clone();
        ret.outputEncodingName = charset.name();
        return ret;
    }

    public UrlBuilder encodeAs(final String charsetName) {
        final UrlBuilder ret = clone();
        ret.outputEncodingName = charsetName;
        return ret;
    }


    public UrlBuilder withProtocol(final String protocol) {
        final UrlBuilder ret = clone();
        ret.protocol = protocol;
        return ret;
    }

    public UrlBuilder withHost(final String hostName) {
        final UrlBuilder ret = clone();
        ret.hostName = hostName;
        return ret;
    }

    public UrlBuilder withPort(final int port) {
        final UrlBuilder ret = clone();
        ret.port = port;
        return ret;
    }

    public UrlBuilder withPath(final String path) {
        final UrlBuilder ret = clone();
        ret.path = path;
        return ret;
    }

    public UrlBuilder withQuery(final String query) {
        final UrlBuilder ret = clone();
        ret.queryParameters = ret.decodeQueryParameters(query);
        return ret;
    }

    public UrlBuilder withAnchor(final String anchor) {
        final UrlBuilder ret = clone();
        ret.anchor = anchor;
        return ret;
    }


    public UrlBuilder addQueryParameter(final String key, final String value) {
        final UrlBuilder ret = clone();
        final ArrayList<String> valueList;
        if (ret.queryParameters.containsKey(key)) {
            valueList = ret.queryParameters.get(key);
        } else {
            valueList = new ArrayList<String>();
            ret.queryParameters.put(key, valueList);
        }
        valueList.add(value);
        return ret;
    }

    public UrlBuilder setQueryParameter(final String key, final String value) {
        final UrlBuilder ret = clone();
        final ArrayList<String> valueList = new ArrayList<String>();
        valueList.add(value);
        ret.queryParameters.put(key, valueList);
        return ret;
    }

    public UrlBuilder removeQueryParameter(final String key, final String value) {
        final UrlBuilder ret = clone();
        if (ret.queryParameters.containsKey(key)) {
            ret.queryParameters.get(key).remove(value);
        }
        return ret;
    }

    public UrlBuilder removeQueryParameters(final String key) {
        final UrlBuilder ret = clone();
        if (ret.queryParameters.containsKey(key)) {
            ret.queryParameters.remove(key);
        }
        return ret;
    }

}
