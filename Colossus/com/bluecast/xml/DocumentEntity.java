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
import  org.xml.sax.*;
import  com.bluecast.util.*;
import  java.net.*;


public class DocumentEntity  implements Entity {
    private boolean isOpen = false;
    private URL url = null;
    private String sysID = null;
    private InputSource source = null;
    static private URL defaultContext;
    private boolean isStandalone = false;
    private XMLStreamReader streamReader = null;
    private XMLReaderReader readerReader = null;
    private XMLInputReader activeReader = null;

    static {
        try {
            defaultContext = new URL("file", null, ".");
        } catch (IOException e) {
            defaultContext = null;
        }
    }

    public DocumentEntity () {
    }

    public DocumentEntity (String sysID) throws IOException
    {
        reset(sysID);
    }

    public DocumentEntity (InputSource source) throws IOException
    {
        reset(source);
    }

    public boolean isOpen () {
        return  isOpen;
    }

    public void open () throws IOException, RecursionException {
        String encoding = null;

        // Were we given an InputSource?
        if (source != null) {
            // Try first to get a Reader
            Reader sourceReader = source.getCharacterStream();
            if (sourceReader != null) {
                isOpen = true;
                if (readerReader == null)
                    readerReader = new XMLReaderReader();

                readerReader.reset(sourceReader,true);
                isStandalone = readerReader.isXMLStandalone();
                activeReader = readerReader;

                return;
            }
            // Next try to get an InputStream
            InputStream in = source.getByteStream();
            if (in != null) {
                if (streamReader == null)
                    streamReader = new XMLStreamReader();
                streamReader.reset(in, source.getEncoding(), true);
                isOpen = true;
                isStandalone = streamReader.isXMLStandalone();
                activeReader = streamReader;
                return;
            }
            // Otherwise use the system ID
            url = new URL(defaultContext, source.getSystemId());
            sysID = url.toString();
            encoding = source.getEncoding();
        }
        if (streamReader == null)
            streamReader = new XMLStreamReader();
        streamReader.reset(url.openStream(), encoding, true);
        isStandalone = streamReader.isXMLStandalone();
        activeReader = streamReader;
        isOpen = true;
    }

    public String getDeclaredEncoding() {
        return activeReader.getXMLDeclaredEncoding();
    }

    public boolean isStandaloneDeclared() {
        return activeReader.isXMLStandaloneDeclared();
    }

    public String getXMLVersion() {
        return activeReader.getXMLVersion();
    }

    public void reset (String sysID) throws IOException {
        close();
        isStandalone = false;
        this.source = null;
        try {
          this.url = new URL(defaultContext, sysID);
        }
        catch (MalformedURLException e) {
          this.url = new File(sysID).toURL();
        }

        this.sysID = url.toString();
    }

    public void reset (InputSource source) throws IOException {
        close();
        isStandalone = false;
        this.source = source;
        sysID = source.getSystemId();
        if (sysID != null) {
            try {
              url = new URL(defaultContext, sysID);
            }
            catch (MalformedURLException e) {
              url = new File(sysID).toURL();
            }

            this.sysID = url.toString();
        }
    }

    public void close () throws IOException {
        if (!isOpen)
            return;
        source = null;
        activeReader.close();
        activeReader = null;
        isOpen = false;
    }

    public String getPublicID () {
        return  null;
    }

    public String getSystemID () {
        return  sysID;
    }

    public boolean isStandalone () {
        return  isStandalone;
    }

    public void setStandalone (boolean standalone) {
        isStandalone = standalone;
    }

    public boolean isInternal () {
        return  false;
    }

    public boolean isParsed () {
        return  true;
    }

    public Reader getReader () {
        return activeReader;
    }

    public String stringValue () {
        throw  new UnsupportedOperationException();
    }

    public char[] charArrayValue () {
        throw  new UnsupportedOperationException();
    }
}



