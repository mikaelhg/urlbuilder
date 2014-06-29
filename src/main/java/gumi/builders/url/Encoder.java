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
package gumi.builders.url;

import static gumi.builders.url.Rfc3986Util.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Percent-encoding according to the URI and URL standards.
 *
 * Don't use this class in application code, as it will likely
 * become package-private at some point. The @Deprecated marker
 * is there to communicate this to application developers.
 */
@Deprecated
public class Encoder {

    private static final boolean IS_PATH = true;

    private static final boolean IS_NOT_PATH = false;

    private static final boolean IS_FRAGMENT = true;

    private static final boolean IS_NOT_FRAGMENT = false;

    private static final boolean IS_USERINFO = true;

    private static final boolean IS_NOT_USERINFO = false;

    private final Charset outputEncoding;

    public Encoder(final Charset outputEncoding) {
        this.outputEncoding = outputEncoding;
    }

    public String encodeUserInfo(String input) {
        if (null == input || input.isEmpty()) {
            return "";
        }
        return urlEncode(input, IS_NOT_PATH, IS_NOT_FRAGMENT, IS_USERINFO);
    }

    public String encodePath(final String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        final StringTokenizer st = new StringTokenizer(input, "/", true);
        while (st.hasMoreElements()) {
            final String element = st.nextToken();
            if ("/".equals(element)) {
                sb.append(element);
            } else if (!element.isEmpty()) {
                sb.append(urlEncode(element, IS_PATH, IS_NOT_FRAGMENT, IS_NOT_USERINFO));
            }
        }
        return sb.toString();
    }

    public String encodeQueryParameters(final UrlParameterMultimap queryParametersMultimap) {
        if (queryParametersMultimap == null)
            throw new IllegalArgumentException("queryParametersMultimap is required to not be null.");
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, String> e : queryParametersMultimap.flatEntryList()) {
            sb.append(encodeQueryElement(e.getKey()));
            if (e.getValue() != null) {
                sb.append('=');
                sb.append(encodeQueryElement(e.getValue()));
            }
            sb.append('&');
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private String encodeQueryElement(final String input) {
        return urlEncode(input, IS_NOT_PATH, IS_NOT_FRAGMENT, IS_NOT_USERINFO);
    }

    public String encodeFragment(final String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return urlEncode(input, IS_NOT_PATH, IS_FRAGMENT, IS_NOT_USERINFO);
    }

    private String urlEncode(final String input, final boolean isPath,
            final boolean isFragment, final boolean isUserInfo) {
        final StringBuilder sb = new StringBuilder();
        final CharBuffer cb = CharBuffer.allocate(1);
        for (final char c : input.toCharArray()) {
            // We're %-encoding + to be on the safe side.
            if ((isPath && isPChar(c) && c != '+')
                    || isFragment && isFragmentSafe(c)
                    || isUserInfo && c == ':'
                    || isUnreserved(c))
            {
                sb.append(c);
            } else {
                cb.put(0, c);
                cb.rewind();
                final ByteBuffer bb = outputEncoding.encode(cb);
                for (int i = 0; i < bb.limit(); i++) {
                    // Until someone has a real problem with the performance of this bit,
                    // I will leave this less optimal, but much simpler implementation in place
                    sb.append('%');
                    sb.append(String.format("%1$02X", bb.get(i)));
                }
            }
        }
        return sb.toString();
    }

}
