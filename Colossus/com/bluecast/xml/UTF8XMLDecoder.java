/*
 * $Id$
 *
 * (C) Copyright 2002 by Yuval Oren. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *   
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */


package  com.bluecast.xml;
import com.bluecast.io.CharsetDecoder;
import  java.io.CharConversionException;


/**
 * A decoder for UTF-8 text. Also converts
 * carriage returns into linefeeds and CRLF into LF.
 *
 * @author Yuval Oren, yuval@bluecast.com
 * @version $Revision$
 */

final public class UTF8XMLDecoder
        implements CharsetDecoder {
    private boolean sawCR = false;

    public CharsetDecoder newInstance() { return new UTF8XMLDecoder(); }

    public int minBytesPerChar () {
        return  1;
    }

    public int maxBytesPerChar () {
        return  3;
    }

    public void decode (byte[] in_buf, int in_off, int in_len, char[] out_buf,
            int out_off, int out_len, int[] result) throws CharConversionException {
        int i, o;

        for (i = o = 0; i < in_len && o < out_len; i++) {
            // A UTF-8 character can be 1 or more bytes. Length
            // is determined by the bitmask of the first byte
            int c, c2, c3, c4;
            c = in_buf[in_off + i];
            // Check for 1 byte first, since it's the most common.
            // One byte: 0xxxxxxx
            if ((c & 0x80) == 0) {
            /* We got the character */
            }
            else {
                // It's at least two bytes long
                if (++i < in_len)
                    c2 = in_buf[in_off + i];
                else {
                    result[0] = i - 1;
                    result[1] = o;
                    return;
                }

                // Two bytes: 110xxxxx 10xxxxxx
                if ((c & 0xE0) == 0xC0) {
                    // Subsequent bytes must begin with 0x10
                    if ((c2 & 0x80) != 0x80)
                        throw new CharConversionException(
                            "Malformed UTF-8 character: 0x"
                            + Integer.toHexString(c&0xFF) + " 0x"
                            + Integer.toHexString(c2&0xFF));

                    c = ((c & 0x1F) << 6) | (c2 & 0x3F);
                    // Make sure this is not an overlong character:
                    // this character must not fit within 7 bits
                    if ((c & 0x780) == 0)
                        throw  new CharConversionException("2-byte UTF-8 character is overlong: 0x"
                                + Integer.toHexString(in_buf[in_off + i - 1] & 0xFF)
                                + " 0x" + Integer.toHexString(c2 & 0xFF));


                }
                // Three bytes: 1110xxxx 10xxxxxx 10xxxxxx
                else if ((c & 0xF0) == 0xE0) {
                    if (++i < in_len)
                        c3 = in_buf[in_off + i];
                    else {
                        result[0] = i - 2;
                        result[1] = o;
                        return;
                    }

                    // Subsequent bytes must begin with 0x10
                    if ( ((c2 & 0x80) != 0x80) ||
                         ((c3 & 0x80) != 0x80) )
                        throw new CharConversionException(
                            "Malformed UTF-8 character: 0x"
                            + Integer.toHexString(c&0xFF) + " 0x"
                            + Integer.toHexString(c2&0xFF) + " 0x"
                            + Integer.toHexString(c3&0xFF));


                    c = ((c & 0xF) << 12) | ((c2 & 0x3F) << 6) | (c3 & 0x3F);
                    // Make sure this is not an overlong character:
                    // this character must not fit within 11 bits
                    if ((c & 0xF800) == 0)
                        throw  new CharConversionException("3-byte UTF-8 character is overlong: 0x"
                                + Integer.toHexString(in_buf[in_off+i-2] & 0xFF)
                                + " 0x" + Integer.toHexString(c2 & 0xFF) + " 0x" + Integer.toHexString(c3 & 0xFF));
                }

                // Four bytes: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
                // This is returned as a surrogate pair of characters
                else if ((c & 0xF0) == 0xF0) {
                    if (i + 2 < in_len) {
                        c3 = in_buf[in_off + (++i)];
                        c4 = in_buf[in_off + (++i)];
                    }
                    else {
                        result[0] = i - 2;
                        result[1] = o;
                        return;
                    }

                    // Subsequent bytes must begin with 0x10
                    if ( ((c2 & 0x80) != 0x80) ||
                         ((c3 & 0x80) != 0x80) ||
                         ((c4 & 0x80) != 0x80) )
                        throw new CharConversionException(
                            "Malformed UTF-8 character: 0x"
                            + Integer.toHexString(c&0xFF) + " 0x"
                            + Integer.toHexString(c2&0xFF) + " 0x"
                            + Integer.toHexString(c3&0xFF) + " 0x"
                            + Integer.toHexString(c4&0xFF));

                    c = ((c & 0x7) << 18) | ((c2 & 0x3F) << 12) | ((c3 & 0x3F) << 6) | (c4 & 0x3F);

                    if (c < 0x10000 || c > 0x10FFFF)
                        throw new CharConversionException("Illegal XML character: 0x"
                            + Integer.toHexString(c));

                    // Construct the surrogate pair
                    c -= 0x10000;
                    out_buf[out_off + (o++)] = (char)((c >> 10) | 0xD800);
                    out_buf[out_off + (o++)] = (char)((c & ((1 << 10) - 1)) | 0xDC00);
                    sawCR = false;
                    continue;
                }
                else {
                    throw  new CharConversionException("Characters larger than 4 bytes are "
                            + "not supported: byte 0x" + Integer.toHexString(c & 0xFF) + " implies a length of more than 4 bytes");
                }

                if ( (c >= 0xD800 && c < 0xE000)
                    || (c == 0xFFFE || c == 0xFFFF) )
                    throw new CharConversionException("Illegal XML character: 0x"
                            + Integer.toHexString(c));
            }
            // Now condense CRLF into LF and transform a lone CR into LF
            if (c >= 0x20) {
                sawCR = false;
                out_buf[out_off + o++] = (char)c;
            }
            else {
                switch (c) {
                    case '\n':
                        if (sawCR) {
                            sawCR = false;
                        }
                        else
                            out_buf[out_off + o++] = '\n';
                        break;

                    case '\r':
                        sawCR = true;
                        out_buf[out_off + o++] = '\n';
                        break;

                    case '\t':
                        out_buf[out_off + o++] = '\t';
                        break;

                    default:
                        throw new CharConversionException(
                        "Illegal XML character: 0x"
                            + Integer.toHexString(c));
                }
            }
        }
        result[0] = i;
        result[1] = o;
    }
}



