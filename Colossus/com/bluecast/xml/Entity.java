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
import  java.net.*;
import  com.bluecast.util.*;
import  org.xml.sax.*;


public interface Entity {

    public boolean isOpen ();

    public void open () throws IOException, SAXException, RecursionException;
    public void close () throws IOException;

    public String getPublicID ();
    public String getSystemID ();

    public boolean isStandalone ();
    public void setStandalone (boolean standalone);

    public boolean isInternal ();
    public boolean isParsed ();

    // These apply only to external entities
    public String getDeclaredEncoding();
    public boolean isStandaloneDeclared();
    public String getXMLVersion();



    public Reader getReader ();
    public String stringValue ();
    public char[] charArrayValue ();
}



