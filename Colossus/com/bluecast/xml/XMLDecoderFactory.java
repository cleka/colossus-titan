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
import  java.io.UnsupportedEncodingException;
import  java.util.HashMap;


/**
 * Factory class for creating CharsetDecoders that also
 * convert carriage returns to linefeeds and check for invalid XML characters,
 * as per the XML 1.0 specification.
 *
 * @author Yuval Oren, yuval@bluecast.com
 * @version $Revision$
 */
public class XMLDecoderFactory {
    private static HashMap decoders = new HashMap();
    static {
        UTF8XMLDecoder utf8 = new UTF8XMLDecoder();
        ASCIIXMLDecoder ascii = new ASCIIXMLDecoder();
        ISO8859_1XMLDecoder iso8859 = new ISO8859_1XMLDecoder();
        UnicodeBigXMLDecoder utf16be = new UnicodeBigXMLDecoder();
        UnicodeLittleXMLDecoder utf16le = new UnicodeLittleXMLDecoder();
        decoders.put("UTF-8", utf8);
        decoders.put("UTF8", utf8);
        decoders.put("US-ASCII", ascii);
        decoders.put("ASCII", ascii);
        decoders.put("ISO-8859-1", iso8859);
        decoders.put("ISO8859_1", iso8859);
        decoders.put("UTF-16LE", utf16le);
        decoders.put("UNICODELITTLE", utf16le);
        decoders.put("UNICODELITTLEUNMARKED", utf16le);
        decoders.put("UTF-16BE", utf16be);
        decoders.put("UNICODEBIG", utf16be);
        decoders.put("UNICODEBIGUNMARKED", utf16be);
    }

    static public CharsetDecoder createDecoder (String encoding) throws UnsupportedEncodingException {
        CharsetDecoder d = (CharsetDecoder)decoders.get(encoding.toUpperCase());
        if (d != null)
            return  d.newInstance();
        else
            throw  new UnsupportedEncodingException("Encoding '" + encoding
                    + "' not supported");
    }
}



