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
 * A Reader for XML documents
 * the proper character set to use based on Byte Order Marks and XML
 * declarations.
 *
 * @author Yuval Oren, yuval@bluecast.com
 * @version $Revision$
 */
abstract public class XMLInputReader extends Reader {
    private String xmlVersion = null;
    private boolean xmlStandaloneDeclared = false;
    private boolean xmlStandalone = false;
    private String xmlDeclaredEncoding = null;

    private XMLDeclParser parser = new XMLDeclParser();

    protected void resetInput() {
        xmlVersion = xmlDeclaredEncoding = null;
        xmlStandaloneDeclared = xmlStandalone = false;
    }

    /**
     * Call this to parse the XML declaration. Returns the number
     * of characters used by the declaration, or zero if no declaration
     * was found.
     */
    protected int parseXMLDeclaration(char[] cbuf, int offset, int length)
    throws IOException {
        parser.reset(cbuf, offset, length);
        if (parser.parse() == parser.SUCCESS) {
            xmlVersion = parser.getXMLVersion();
            xmlStandalone = parser.isXMLStandalone();
            xmlStandaloneDeclared = parser.isXMLStandaloneDeclared();
            xmlDeclaredEncoding = parser.getXMLEncoding();
            return parser.getCharsRead();
        }
        else
            return 0;
    }

    /**
     * Gets the version string from the XML declaration, or null.
     */
    public String getXMLVersion () {
        return  xmlVersion;
    }

    /**
     * Returns the Standalone value from the XML declaration.
     * Defaults to false if no value was declared.
     */
    public boolean isXMLStandalone () {
        return  xmlStandalone;
    }

    /**
     * Returns true if an XML "standalone" declaration was found.
     */
    public boolean isXMLStandaloneDeclared() {
        return xmlStandaloneDeclared;
    }

    /**
     *  Gets the declared encoding from the XML declaration,
     *  or null if no value was found.
     */
    public String getXMLDeclaredEncoding () {
        return  xmlDeclaredEncoding;
    }
}



