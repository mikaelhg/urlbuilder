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
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

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
            return urlDecode(userInfo, DO_NOT_DECODE_PLUS_AS_SPACE);
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
        return escapedBytes(input, position, escapeRunEnd(input, position, false));
    }

    /**
     * Returns the end of the run of valid {@code %XX} escapes starting at {@code position}.
     * With {@code keepPathDelimiters}, the run stops before {@code %2F} and {@code %25}.
     */
    private static int escapeRunEnd(final String input, final int position, final boolean keepPathDelimiters) {
        int i = position;
        while (isEscape(input, i)) {
            if (keepPathDelimiters && isPathDelimiterEscape(input, i)) {
                break;
            }
            i += 3;
        }
        return i;
    }

    private static boolean isEscape(final String input, final int i) {
        return i + 2 < input.length()
                && input.charAt(i) == '%'
                && hexValue(input.charAt(i + 1)) >= 0
                && hexValue(input.charAt(i + 2)) >= 0;
    }

    private static boolean isPathDelimiterEscape(final String input, final int i) {
        final int b = escapedByte(input, i);
        return b == '/' || b == '%';
    }

    private static byte escapedByte(final String input, final int i) {
        return (byte) (hexValue(input.charAt(i + 1)) << 4 | hexValue(input.charAt(i + 2)));
    }

    private static byte[] escapedBytes(final String input, final int start, final int end) {
        final byte[] data = new byte[(end - start) / 3];
        for (int j = 0; j < data.length; j++) {
            data[j] = escapedByte(input, start + j * 3);
        }
        return data;
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

    /**
     * Decodes a percent-encoded path.
     *
     * <p>{@code %2F} and {@code %25} are kept encoded (normalised to upper case), so that an encoded
     * slash stays distinguishable from a segment separator and {@link Encoder#encodePath} restores
     * the original path.
     */
    public String decodePath(final String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return decode(input, DO_NOT_DECODE_PLUS_AS_SPACE, true);
    }

    /**
     * Decodes {@code %XX} escapes, and optionally {@code +} as a space.
     *
     * <p>Malformed escapes, and escaped bytes that are not valid in the input encoding,
     * are kept as their original text.
     */
    public String urlDecode(final String input, final boolean decodePlusAsSpace) {
        return decode(input, decodePlusAsSpace, false);
    }

    private String decode(final String input, final boolean decodePlusAsSpace, final boolean keepPathDelimiters) {
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
        CharsetDecoder charsetDecoder = null;
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
                final int end = escapeRunEnd(input, i, keepPathDelimiters);
                if (end > i) {
                    if (charsetDecoder == null) {
                        charsetDecoder = inputEncoding.newDecoder();
                    }
                    decodeEscapes(input, i, end, charsetDecoder, sb);
                    i = end;
                } else if (keepPathDelimiters && isEscape(input, i)) {
                    sb.append('%')
                            .append(Character.toUpperCase(input.charAt(i + 1)))
                            .append(Character.toUpperCase(input.charAt(i + 2)));
                    i += 3;
                } else {
                    sb.append('%');
                    i++;
                }
            }
        }
        return sb.toString();
    }

    /**
     * Appends the decoded escapes in {@code input[start, end)} to {@code sb}. Byte sequences that
     * the charset rejects are appended as their original escape text.
     */
    private static void decodeEscapes(final String input, final int start, final int end,
            final CharsetDecoder charsetDecoder, final StringBuilder sb)
    {
        final ByteBuffer in = ByteBuffer.wrap(escapedBytes(input, start, end));
        final CharBuffer out = CharBuffer.allocate((int) (in.capacity() * charsetDecoder.maxCharsPerByte()) + 1);
        charsetDecoder.reset();
        while (true) {
            final CoderResult result = charsetDecoder.decode(in, out, true);
            if (result.isError()) {
                drain(out, sb);
                final int bad = in.position();
                sb.append(input, start + bad * 3, start + (bad + result.length()) * 3);
                in.position(bad + result.length());
            } else if (result.isOverflow()) {
                drain(out, sb);
            } else {
                break;
            }
        }
        while (charsetDecoder.flush(out).isOverflow()) {
            drain(out, sb);
        }
        drain(out, sb);
    }

    private static void drain(final CharBuffer out, final StringBuilder sb) {
        out.flip();
        sb.append(out);
        out.clear();
    }

}
