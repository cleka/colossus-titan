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
 * A decoder for big-endian Unicode text. Also converts
 * carriage returns into linefeeds and CRLF into LF.
 *
 * @author Yuval Oren, yuval@bluecast.com
 * @version $Revision$
 */

final public class UnicodeBigXMLDecoder
        implements CharsetDecoder {

    public CharsetDecoder newInstance() { return new UnicodeBigXMLDecoder(); }

    public int minBytesPerChar () {
        return  1;
    }

    public int maxBytesPerChar () {
        return  1;
    }

    public void decode (byte[] in_buf, int in_off, int in_len, char[] out_buf,
            int out_off, int out_len, int[] result) throws CharConversionException {
        int i, o;
        boolean sawCR = false;
        for (i = o = 0; i + 1 < in_len && o < out_len; i += 2) {
            char c = (char)(((0xFF & in_buf[in_off + i]) << 8) | (0xFF & in_buf[
                    in_off + i + 1]));
            if (c >= 0x20) {
                if ((c <= 0xD7FF) ||
                   (c >= 0xE000 && c <= 0xFFFD) ||
                   (c >= 0x10000 && c <= 0x10FFFF)) {
                    sawCR = false;
                    out_buf[out_off + o++] = (char)c;
                   }
                else
                    throw new CharConversionException(
                        "Illegal XML Character: 0x"+Integer.toHexString(c));
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



