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
package io.mikael.urlbuilder.util;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.StringTokenizer;

/**
 * Percent-decoding according to the URI and URL standards.
 */
public class Decoder {

    protected static final boolean DECODE_PLUS_AS_SPACE = true;

    protected static final boolean DO_NOT_DECODE_PLUS_AS_SPACE = false;

    protected final Charset inputEncoding;

    public Decoder(final Charset inputEncoding) {
        this.inputEncoding = inputEncoding;
    }

    public String decodeUserInfo(final String userInfo) {
        if (null == userInfo || userInfo.isEmpty()) {
            return userInfo;
        } else {
            return urlDecode(userInfo, DECODE_PLUS_AS_SPACE);
        }
    }

    public String decodeFragment(final String fragment) {
        if (fragment == null || fragment.isEmpty()) {
            return fragment;
        }
        return urlDecode(fragment, DO_NOT_DECODE_PLUS_AS_SPACE);
    }

    public UrlParameterMultimap parseQueryString(final String query) {
        final UrlParameterMultimap ret = UrlParameterMultimap.newMultimap();
        if (query == null || query.isEmpty()) {
            return ret;
        }
        for (final String part : query.split("&")) {
            final String[] kvp = part.split("=", 2);
            final String key, value;
            key = urlDecode(kvp[0], DECODE_PLUS_AS_SPACE);
            if (kvp.length == 2) {
                value = urlDecode(kvp[1], DECODE_PLUS_AS_SPACE);
            } else {
                value = null;
            }
            ret.add(key, value);
        }
        return ret;
    }

    public byte[] nextDecodeableSequence(final String input, final int position) {
        final int len = input.length();
        final byte[] data = new byte[Math.max(0, (len - position) / 3)];
        int j = 0;
        int i = position;
        while (i + 2 < len && input.charAt(i) == '%') {
            final int hi = hexValue(input.charAt(i + 1));
            final int lo = hexValue(input.charAt(i + 2));
            if (hi < 0 || lo < 0) {
                break;
            }
            data[j++] = (byte) (hi << 4 | lo);
            i += 3;
        }
        return j == data.length ? data : Arrays.copyOfRange(data, 0, j);
    }

    private static int hexValue(final char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        } else if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    public String decodePath(final String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        final boolean RETURN_DELIMETERS = true;
        final StringTokenizer st = new StringTokenizer(input, "/", RETURN_DELIMETERS);

        while (st.hasMoreElements()) {
            final String element = st.nextToken();
            if ("/".equals(element)) {
                sb.append(element);
            } else if (!element.isEmpty()) {
                sb.append(urlDecode(element, DO_NOT_DECODE_PLUS_AS_SPACE));
            }
        }
        return sb.toString();
    }

    public String urlDecode(final String input, final boolean decodePlusAsSpace) {
        final int len = input.length();
        int first = 0;
        while (first < len) {
            final char c = input.charAt(first);
            if (c == '%' || (c == '+' && decodePlusAsSpace)) {
                break;
            }
            first++;
        }
        if (first == len) {
            return input;
        }

        final StringBuilder sb = new StringBuilder(len).append(input, 0, first);
        int i = first;
        while (i < len) {
            final char c = input.charAt(i);
            if (c == '+' && decodePlusAsSpace) {
                sb.append(' ');
                i++;
            } else if (c != '%') {
                sb.append(c);
                i++;
            } else {
                final byte[] bytes = nextDecodeableSequence(input, i);
                if (bytes.length == 0) {
                    sb.append('%');
                    i++;
                } else {
                    sb.append(new String(bytes, inputEncoding));
                    i += bytes.length * 3;
                }
            }
        }
        return sb.toString();
    }

}
