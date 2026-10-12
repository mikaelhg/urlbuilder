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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Map;

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

    /**
     * Percent-encodes a decoded path, keeping {@code /} as the segment separator.
     *
     * <p>{@code %2F} and {@code %25} (either case) are emitted as-is, as the encoded slash and
     * percent sign that {@link Decoder#decodePath} keeps encoded. Any other {@code %} is encoded.
     */
    public String encodePath(final String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        final int len = input.length();
        final StringBuilder sb = new StringBuilder(len);
        int segmentStart = 0;
        int i = 0;
        while (i < len) {
            final char c = input.charAt(i);
            if (c == '/') {
                appendPathSegment(sb, input, segmentStart, i);
                sb.append('/');
                i++;
                segmentStart = i;
            } else if (c == '%' && isPathDelimiterEscape(input, i)) {
                appendPathSegment(sb, input, segmentStart, i);
                sb.append('%').append('2').append(Character.toUpperCase(input.charAt(i + 2)));
                i += 3;
                segmentStart = i;
            } else {
                i++;
            }
        }
        appendPathSegment(sb, input, segmentStart, len);
        return sb.toString();
    }

    private void appendPathSegment(final StringBuilder sb, final String input, final int start, final int end) {
        if (start < end) {
            sb.append(urlEncode(input.substring(start, end), IS_PATH, IS_NOT_FRAGMENT, IS_NOT_USERINFO));
        }
    }

    private static boolean isPathDelimiterEscape(final String input, final int i) {
        if (i + 2 >= input.length() || input.charAt(i + 1) != '2') {
            return false;
        }
        final char c = input.charAt(i + 2);
        return c == 'F' || c == 'f' || c == '5';
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

    public String encodeQueryElement(final String input) {
        return urlEncode(input, IS_NOT_PATH, IS_NOT_FRAGMENT, IS_NOT_USERINFO);
    }

    public String encodeFragment(final String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return urlEncode(input, IS_NOT_PATH, IS_FRAGMENT, IS_NOT_USERINFO);
    }

    /**
     * Percent-encodes every character that is not safe in the given component.
     *
     * <p>Characters the output encoding cannot represent, and unpaired surrogates, are
     * replaced with the charset's replacement bytes, usually {@code %3F} ({@code ?}).
     */
    public String urlEncode(final String input, final boolean isPath,
            final boolean isFragment, final boolean isUserInfo)
    {
        return input.codePoints()
                .sequential()
                .mapToObj(Character::toChars)
                .collect(StringBuffer::new,
                        (sb, chars) -> {
                            boolean needsEncoding = true;
                            if (chars.length == 1) {
                                char c = chars[0];
                                if ((isPath && Rfc3986Util.isPChar(c))
                                        || (isFragment && Rfc3986Util.isFragmentSafe(c))
                                        || (isUserInfo && c == ':')
                                        || Rfc3986Util.isUnreserved(c)) {
                                    sb.append(c);
                                    needsEncoding = false;
                                }
                            }
                            if (needsEncoding) {
                                final CharBuffer cb = CharBuffer.wrap(chars);
                                final ByteBuffer bb = outputEncoding.encode(cb);
                                for (int j = 0; j < bb.limit(); j++) {
                                    sb.append(appendPercentEncodedByte(bb.get(j)));
                                }
                            }
                        },
                        StringBuffer::append
                )
                .toString();
    }

    static char[] appendPercentEncodedByte(final byte b) {
        final int unsignedByte = b & 0xFF;
        final int highNibble = (unsignedByte >> 4) & 0xF;
        final int lowNibble = unsignedByte & 0xF;
        return new char[] {'%', getHexChar(highNibble), getHexChar(lowNibble) };
    }

    static char getHexChar(final int nibble) {
        return (nibble < 10) ? (char) ('0' + nibble) : (char) ('A' - 10 + nibble);
    }

}
