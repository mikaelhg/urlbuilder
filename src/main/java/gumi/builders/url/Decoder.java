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

import static gumi.builders.url.UrlParameterMultimap.*;

import java.nio.ByteBuffer;
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
        final UrlParameterMultimap ret = newMultimap();
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

    protected byte[] nextDecodeableSequence(final String input, final int position) {
        final int len = input.length();
        final byte[] data = new byte[len];
        int j = 0;
        for (int i = position; i < len; i++) {
            final char c0 = input.charAt(i);
            if (c0 != '%' || (len < i + 2)) {
                return Arrays.copyOfRange(data, 0, j);
            } else {
                data[j++] = (byte) Integer.parseInt(input.substring(i + 1, i + 3), 16);
                i += 2;
            }
        }
        return Arrays.copyOfRange(data, 0, j);
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

    protected String urlDecode(final String input, final boolean decodePlusAsSpace) {
        final StringBuilder sb = new StringBuilder();
        final int len = input.length();
        for (int i = 0; i < len; i++) {
            final char c0 = input.charAt(i);
            if (c0 == '+' && decodePlusAsSpace) {
                sb.append(' ');
            } else if (c0 != '%') {
                sb.append(c0);
            } else if (len < i + 2) {
                // the string will end before we will be able to read a sequence
                i += 2;
            } else {
                final byte[] bytes = nextDecodeableSequence(input, i);
                sb.append(inputEncoding.decode(ByteBuffer.wrap(bytes)));
                i += bytes.length * 3 - 1;
            }
        }
        return sb.toString();
    }

}
