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

import  java.io.*;
import  com.bluecast.io.*;
import  java.util.*;


/**
 * A Reader for XML documents and streams. This class automatically determines
 * the proper character set to use based on Byte Order Marks and XML
 * declarations.
 *
 * @author Yuval Oren, yuval@bluecast.com
 * @version $Revision$
 */
final public class XMLStreamReader extends XMLInputReader {
    private static final int BYTE_BUFFER_SIZE = 8192;
    private CharsetDecoder decoder = null;
    private int minBytesPerChar, maxBytesPerChar;
    private InputStream in;
    private int[] decodeResult = new int[2];
    private String encoding;
    private boolean useDeclaredEncoding = false;
    private boolean rewindDeclaration;
    private char[] cbuf = new char[MAX_XML_DECL_CHARS];
    private byte[] bbuf = new byte[BYTE_BUFFER_SIZE];
    private int cbufPos = 0, cbufEnd = 0, bbufPos = 0, bbufEnd = 0;
    private boolean eofReached = false;
    // How many characters we should read to parse the <?xml...?> declaration
    private static final int MAX_XML_DECL_CHARS = 100;

    /**
     * Create an XMLStreamReader without providing an InputStream yet.
     * You must call reset() before using.
     */
    public XMLStreamReader () {
    }

    /**
     * Creates an XMLStreamReader.
     *
     * @param     in the InputStream
     * @param     rewindDeclaration a value of false will skip past any
     *            XML declaration. True will dish out the entire document.
     */
    public XMLStreamReader (InputStream in, boolean rewindDeclaration) throws IOException
    {
        this(in, null, rewindDeclaration);
    }

    /**
     * Creates an XMLStreamReader while specifying a character encoding.
     */
    public XMLStreamReader (InputStream in, String encoding,
                            boolean rewindDeclaration) throws IOException
    {
        reset(in, encoding, rewindDeclaration);
    }

    /**
     * Reuses this XMLStreamReader for a different InputStream.
     */
    public void reset (InputStream in, String encoding,
                       boolean rewindDeclaration) throws IOException {
        super.resetInput();
        this.in = in;
        this.rewindDeclaration = rewindDeclaration;
        useDeclaredEncoding = false;
        bbufPos = bbufEnd = 0;
        cbufPos = cbufEnd = 0;
        fillByteBuffer();
        // If we've been given a character set, use it.
        if (encoding != null) {
            this.encoding = getJavaCharset(encoding);

            // If it's Unicode we need to find out which-endian
            // Per the unicode.org FAQ, default to big-endian
            if (this.encoding == "Unicode") {
              this.encoding = guessEncoding();
              if (this.encoding == null
                  || !(this.encoding.equals("UnicodeLittle")))
                this.encoding = "UnicodeBig";
            }
        }
        else {
            this.encoding = guessEncoding();
            if (this.encoding == null) {
                useDeclaredEncoding = true;
                this.encoding = "UTF-8";        // Default to UTF-8
            }
        }
        setEncoding(this.encoding);
        processXMLDecl();
    }

    /** Returns the character set being used by the reader. Note that the
     *  encoding in the XML declaration is ignored if it is not needed to
     *  determine the character set.
     */
    public String getEncoding () {
        return  encoding;
    }


    public void close () throws IOException {
        eofReached = true;
        bbufPos = bbufEnd = cbufPos = cbufEnd = 0;
        if (in != null)
            in.close();
    }

    public void mark (int readAheadLimit) throws IOException {
        throw  new UnsupportedOperationException("mark() not supported");
    }

    public boolean markSupported () {
        return  false;
    }

    public int read () throws IOException {
        if (cbufEnd - cbufPos > 0)
            return  (int)cbuf[cbufPos++];
        else {
            cbufPos = cbufEnd = 0;
            cbufEnd = read(cbuf, cbufPos, MAX_XML_DECL_CHARS);
            if (cbufEnd > 0)
                return  (int)cbuf[cbufPos++];
            else
                return  -1;
        }
    }

    public int read (char[] destbuf) throws IOException {
        return  read(destbuf, 0, destbuf.length);
    }

    public int read (char[] destbuf, int off, int len) throws IOException {
        int charsRead = 0;
        // First copy any characters from the character buffer
        if (cbufEnd - cbufPos > 0) {
            int numToRead = Math.min(cbufEnd - cbufPos, len - charsRead);
            if (numToRead > 0) {
                System.arraycopy(cbuf, cbufPos, destbuf, off, numToRead);
                charsRead += numToRead;
                cbufPos += numToRead;
            }
        }
        while (charsRead < len) {
            if (bbufEnd - bbufPos < maxBytesPerChar) {
                fillByteBuffer();
                if (bbufEnd - bbufPos < minBytesPerChar)
                    return  (charsRead == 0 ? -1 : charsRead);
            }
            decoder.decode(bbuf, bbufPos, bbufEnd - bbufPos, destbuf, off +
                    charsRead, len - charsRead, decodeResult);
            bbufPos += decodeResult[0];
            charsRead += decodeResult[1];
        }
        return  ((charsRead == 0 && eofReached) ? -1 : charsRead);
    }

    public boolean ready () throws IOException {
        return  ((cbufEnd - cbufPos > 0) || (bbufEnd - bbufPos > maxBytesPerChar)
                || (in.available() > 0));
    }

    public void reset () throws IOException {
        super.resetInput();
        in.reset();
        bbufPos = bbufEnd = cbufPos = cbufEnd = 0;
    }

    public long skip (long n) throws IOException {
        long skipped = 0;
        if (cbufEnd - cbufPos > 0) {
            skipped = Math.min((long)cbufEnd - cbufPos, n);
            cbufPos += skipped;
        }
        while (skipped < n) {
            cbufPos = 0;
            cbufEnd = read(cbuf, 0, MAX_XML_DECL_CHARS);
            if (cbufEnd > 0) {
                cbufPos = (int)Math.min((long)cbufEnd, n - skipped);
                skipped += cbufPos;
            }
            else {
                cbufEnd = 0;
                return  skipped;
            }
        }
        return  skipped;
    }

    private void setEncoding (String encoding) throws IOException {
        decoder = XMLDecoderFactory.createDecoder(encoding);
        this.encoding = encoding;
        if (decoder == null)
            throw  new UnsupportedEncodingException(encoding);
        minBytesPerChar = decoder.minBytesPerChar();
        maxBytesPerChar = decoder.maxBytesPerChar();
    }

    private int fillByteBuffer () throws IOException {
        int bytesLeft = bbufEnd - bbufPos;
        if (bytesLeft > 0)
            System.arraycopy(bbuf, bbufPos, bbuf, 0, bytesLeft);
        bbufPos = 0;
        bbufEnd = bytesLeft;
        int bytesRead = 0;
        while (bbufEnd < BYTE_BUFFER_SIZE && (bytesRead = in.read(bbuf, bbufEnd,
                BYTE_BUFFER_SIZE - bbufEnd)) != -1) {
            bbufEnd += bytesRead;
        }
        if (bytesRead == -1)
            eofReached = true;
        return  bytesRead;
    }
    static private HashMap charsetTable = new HashMap(31);
    static {
        charsetTable.put("EBCDIC-CP-US", "Cp037");
        charsetTable.put("EBCDIC-CP-CA", "Cp037");
        charsetTable.put("EBCDIC-CP-NL", "Cp037");
        charsetTable.put("EBCDIC-CP-WT", "Cp037");
        charsetTable.put("EBCDIC-CP-DK", "Cp277");
        charsetTable.put("EBCDIC-CP-NO", "Cp277");
        charsetTable.put("EBCDIC-CP-FI", "Cp278");
        charsetTable.put("EBCDIC-CP-SE", "Cp278");
        charsetTable.put("EBCDIC-CP-IT", "Cp280");
        charsetTable.put("EBCDIC-CP-ES", "Cp284");
        charsetTable.put("EBCDIC-CP-GB", "Cp285");
        charsetTable.put("EBCDIC-CP-FR", "Cp297");
        charsetTable.put("EBCDIC-CP-AR1", "Cp420");
        charsetTable.put("EBCDIC-CP-GR", "Cp423");
        charsetTable.put("EBCDIC-CP-HE", "Cp424");
        charsetTable.put("EBCDIC-CP-BE", "Cp500");
        charsetTable.put("EBCDIC-CP-CH", "Cp500");
        charsetTable.put("EBCDIC-CP-ROECE", "Cp870");
        charsetTable.put("EBCDIC-CP-YU", "Cp870");
        charsetTable.put("EBCDIC-CP-IS", "Cp871");
        charsetTable.put("EBCDIC-CP-TR", "Cp905");
        charsetTable.put("EBCDIC-CP-AR2", "Cp918");
        charsetTable.put("UTF-16", "Unicode");
        charsetTable.put("ISO-10646-UCS-2", "Unicode");
    }

    // Get the java name for a possibly standard character set name.
    private String getJavaCharset (String charset) {
        if (charset == null)
            return  null;
        String xlated = (String)charsetTable.get(charset);
        if (xlated != null)
            return  xlated;
        else
            return  charset;
    }

    // Guesses the encoding of a stream from the first 4 bytes
    // All bytes read will be "unread" back into the stream
    // Returns an encoding name or null if unknown
    private String guessEncoding () throws IOException {
        if (bbufEnd < 4)
            return  null;
        switch (bbuf[0]) {
            case (byte)0xEF:
                if (bbuf[1] == (byte)0xBB && bbuf[2] == (byte)0xBF) {
                    bbufPos = 3;                // Skip the Byte Order Mark
                    return  "UTF-8";
                }
                else
                    return  null;
            case (byte)'<':                     // UTF-8/ASCII/etc, UTF-16LE, or UCS-4
                switch (bbuf[1]) {
                    case (byte)'?':
                        // UTF-8/ASCII/etc, but we're not sure which
                        if (bbuf[2] == (byte)0x78 && bbuf[3] == (byte)0x6D) {
                            useDeclaredEncoding = true;
                            return  "UTF-8";
                        }
                        else
                            return  null;
                    case (byte)0x00:
                        if (bbuf[2] == (byte)'?' && bbuf[3] == (byte)0x00)
                            return  "UnicodeLittleUnmarked";
                        else if (bbuf[2] == (byte)0x00 && bbuf[3] == (byte)0x00)
                            return  "UCS-4";
                        else
                            return  null;
                    default:
                        return  null;
                }
            case (byte)0xFE:                    // UTF-16BE or UCS-4 unusual (3412)
                if (bbuf[1] == (byte)0xFF) {
                    if (bbuf[2] == (byte)0x00 && bbuf[3] == (byte)0x00) {
                        bbufPos = 4;            // Skip the Byte Order Mark
                        return  "UCS-4";        // Not supported by Java
                    }
                    else {
                        bbufPos = 2;            // Skip the Byte Order Mark
                        return  "UnicodeBig";
                    }
                }
                else
                    return  null;
            case (byte)0xFF:                    // UTF-16LE or UCS-4LE
                if (bbuf[1] == (byte)0xFE) {
                    if (bbuf[2] == (byte)0x00 && bbuf[3] == (byte)0x00) {
                        bbufPos = 4;            // Skip the Byte Order Mark
                        return  "UCS-4";        // LE, not supported by Java
                    }
                    else {
                        bbufPos = 2;            // Skip the Byte Order Mark
                        return  "UnicodeLittle";
                    }
                }
                else
                    return  null;
            case (byte)0x00:                    // UCS-4BE or UCS-4 unusual (2143),
                // or if there's no BOM, UTF-16BE or UCS-4
                switch (bbuf[1]) {
                    case (byte)0x00:
                        if (bbuf[2] == (byte)0xFE && bbuf[3] == (byte)0xFF) {
                            bbufPos = 4;        // Skip the Byte Order Mark
                            return  "UCS-4";                    // BE, unsupported by Java
                        }
                        else if (bbuf[2] == (byte)0xFF && bbuf[3] == (byte)0xFE) {
                            bbufPos = 4;        // Skip the Byte Order Mark
                            return  "UCS-4";                    // Unusual (2143)
                        }
                        // UCS-4 without a byte order mark
                        else if ((bbuf[2] == (byte)'<' && bbuf[3] == (byte)0x00)
                                || (bbuf[2] == (byte)0x00 && bbuf[3] == (byte)'<'))
                            return  "UCS-4";
                        else
                            return  null;
                    case (byte)'<':             // UCS-4 or UTF-16BE
                        if (bbuf[2] == (byte)0x00 && bbuf[3] == (byte)'?')
                            return  "UnicodeBigUnmarked";
                        else if (bbuf[2] == (byte)0x00 && bbuf[3] == (byte)0x00)
                            return  "UCS-4";
                        else
                            return  null;
                    default:
                        return  null;
                }
            case (byte)0x4C:                    // EBCDIC
                if (bbuf[1] == (byte)0x6F && bbuf[2] == (byte)0xA7 && bbuf[3]
                        == (byte)0x94) {
                    useDeclaredEncoding = true;
                    return  "Cp037";
                }
                else
                    return  null;
            default:            // Unknown
                useDeclaredEncoding = true;
                return  null;
        }
    }

    /* Read [max] characters, parse the <?xml...?> tag
     * push it back onto the stream. Create a reader. Then, if there was
     * no error parsing the declaration, eat up the declaration.
     */
    private void processXMLDecl () throws IOException {
        int initialBBufPos = bbufPos;
        // Convert the byte buffer to characters
        decoder.decode(bbuf, bbufPos, bbufEnd - bbufPos,
                       cbuf, cbufPos, cbuf.length,
                decodeResult);
        bbufPos += decodeResult[0];
        cbufEnd = decodeResult[1];

        int numCharsParsed = parseXMLDeclaration(cbuf,0,cbufEnd);

        if (numCharsParsed > 0) {
            // Declaration found and parsed

            String declaredEncoding = getJavaCharset(getXMLDeclaredEncoding());

            // Skip the XML declaration unless told otherwise
            if (!rewindDeclaration)
                cbufPos += numCharsParsed;

            // If another encoding was specified, use it instead of the guess.
            if (useDeclaredEncoding
                && (declaredEncoding != null)
                && !declaredEncoding.equalsIgnoreCase(encoding)) {

                cbufPos = cbufEnd = 0;

                if (rewindDeclaration)
                    bbufPos = initialBBufPos;
                else
                    bbufPos = numCharsParsed*minBytesPerChar;

                setEncoding(declaredEncoding);
            }
        }
    }
}



