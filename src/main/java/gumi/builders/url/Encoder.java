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
 */
public class Encoder {

    protected static final boolean IS_PATH = true;

    protected static final boolean IS_NOT_PATH = false;

    protected static final boolean IS_FRAGMENT = true;

    protected static final boolean IS_NOT_FRAGMENT = false;

    protected static final boolean IS_USERINFO = true;

    protected static final boolean IS_NOT_USERINFO = false;

    protected final Charset outputEncoding;

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

    protected String encodeQueryElement(final String input) {
        return urlEncode(input, IS_NOT_PATH, IS_NOT_FRAGMENT, IS_NOT_USERINFO);
    }

    public String encodeFragment(final String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return urlEncode(input, IS_NOT_PATH, IS_FRAGMENT, IS_NOT_USERINFO);
    }

    protected String urlEncode(final String input, final boolean isPath,
            final boolean isFragment, final boolean isUserInfo)
    {
        final StringBuilder sb = new StringBuilder();
        final char[] inputChars = input.toCharArray();
        for (int i = 0; i < Character.codePointCount(inputChars, 0, inputChars.length); i++) {
            final CharBuffer cb;
            final int codePoint = Character.codePointAt(inputChars, i);
            if (isBmpCodePoint(codePoint)) {
                final char c = Character.toChars(codePoint)[0];
                if ((isPath && isPChar(c) && c != '+')
                        || isFragment && isFragmentSafe(c)
                        || isUserInfo && c == ':'
                        || isUnreserved(c))
                {
                    sb.append(c);
                    continue;
                } else {
                    cb = CharBuffer.allocate(1);
                    cb.append(c);
                }
            } else {
                cb = CharBuffer.allocate(2);
                cb.append(highSurrogate(codePoint));
                cb.append(lowSurrogate(codePoint));
            }
            cb.rewind();
            final ByteBuffer bb = outputEncoding.encode(cb);
            for (int j = 0; j < bb.limit(); j++) {
                // Until someone has a real problem with the performance of this bit,
                // I will leave this less optimal, but much simpler implementation in place
                sb.append('%');
                sb.append(String.format("%1$02X", bb.get(j)));
            }
        }
        return sb.toString();
    }

    /** Character.highSurrogate is not available in Java 6... **/
    protected static char highSurrogate(int codePoint) {
        return (char) ((codePoint >>> 10)
                + (Character.MIN_HIGH_SURROGATE - (Character.MIN_SUPPLEMENTARY_CODE_POINT >>> 10)));
    }

    /** Character.lowSurrogate is not available in Java 6... **/
    protected static char lowSurrogate(int codePoint) {
        return (char) ((codePoint & 0x3ff) + Character.MIN_LOW_SURROGATE);
    }

    /** Character.isBmpCodePoint is not available in Java 6... **/
    protected static boolean isBmpCodePoint(int codePoint) {
        return codePoint >>> 16 == 0;
    }
}
