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


package com.bluecast.util;

import java.io.*;
import java.util.*;
import java.net.*;

/**
 * This class can enumerate all the Providers of a particular Service. It
 * searches the classpath for META-INF/services/<service name> and returns
 * the first line of each service. That string is usually the name of the
 * factory class implementing the requested service.
 */
public class FactoryServiceFinder {
    static final String SERVICE = "META-INF/services/";

    /**
     * Find the first listed provider for the given service name
     */
    static public String findService(String name) throws IOException {
        InputStream is =
            ClassLoader.getSystemClassLoader().getResourceAsStream(
                SERVICE + name);
        BufferedReader r = new BufferedReader(
            new InputStreamReader(is,"UTF-8"));
        return r.readLine();
    }

    /**
     * Return an Enumeration of class name Strings of
     * available provider classes for the given service.
     */
    static public Enumeration findServices(String name) throws IOException {
        return new FactoryEnumeration(
            ClassLoader.getSystemClassLoader().getResources(name));
    }

    static private class FactoryEnumeration implements Enumeration {
        Enumeration enum;
        Object next=null;

        FactoryEnumeration(Enumeration enum) {
            this.enum = enum;
            nextElement();
        }

        public boolean hasMoreElements() {
            return (next != null);
        }

        public Object nextElement() {
            Object current = next;

            while (true) {
                try {
                    if (enum.hasMoreElements()) {
                        BufferedReader r = new BufferedReader(
                            new InputStreamReader(
                                ((URL)enum.nextElement()).openStream() ) );
                        next = r.readLine();
                    }
                    else
                        next = null;

                    break;
                }
                catch (IOException e) {
                    /* this one got an error. try the next. */
                }
            }

            return current;
        }
    }

}
