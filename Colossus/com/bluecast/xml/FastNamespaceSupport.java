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

package com.bluecast.xml;

import com.bluecast.util.*;
import java.util.Arrays;

/**
 * This class improves performance over NamespaceSupport by
 * assuming that most XML documents have very few namespaces. Therefore,
 * instead of performing expensive copying operations of hash tables,
 * arrays and linear searches are used instead.
 *
 * NOTE: This class is not a drop-in replacement for NamespaceSupport. This
 * class assumes that passed URIs are already internalized! Also, getURI()
 * returns "" instead of null if a prefix is not found.
 *
 * @author Yuval Oren
 * @version $Revision$
 *
 */

public class FastNamespaceSupport {
    public final static String XMLNS =
    "http://www.w3.org/XML/1998/namespace";

    private String[] prefixes = new String[20];
    private String[] uris = new String[20];
    private int prefixPos;



    private String defaultURI;
    private StringStack defaultURIs = new StringStack(20);

    // How many prefixes are there in this context?
    private int prefixCount;
    private IntStack contextPrefixCounts = new IntStack(20);


    // For how many contexts is the current default URI valid?
    private int defaultURIContexts;
    private IntStack defaultURIContextCounts = new IntStack(20);

    public FastNamespaceSupport() {
        reset();
    }

    public void reset() {
        defaultURIs.clear();
        contextPrefixCounts.clear();
        defaultURIContextCounts.clear();

        prefixPos = -1;
        defaultURI = "";
        prefixCount = 0;
        defaultURIContexts = 0;
    }

    public void pushContext() {
        defaultURIContexts++;

        contextPrefixCounts.push(prefixCount);
        prefixCount = 0;
    }

    public void popContext() {
        if (defaultURIContexts <= 0) {
            defaultURIContexts = defaultURIContextCounts.pop();
            defaultURI = defaultURIs.pop();
        }
        else
            defaultURIContexts--;

        prefixPos -= prefixCount;
        prefixCount = contextPrefixCounts.pop();
    }

    public void declarePrefix(String prefix, String uri) {
        if (prefix.length() == 0) {
            if (defaultURIContexts > 0) {
                defaultURIContextCounts.push(defaultURIContexts);
                defaultURIs.push(defaultURI);
                defaultURIContexts = 0;
            }
            defaultURI = uri;
        }
        else {
            // First see if this prefix already exists in this context
            for (int i=0; i < prefixCount; i++) {
                if (prefix == prefixes[prefixPos - i]) {
                    uris[prefixPos - i] = uri;
                    return;
                }
            }

            // Doesn't exist yet; declare this as a new prefix
            prefixPos++;
            prefixCount++;

            // First ensure the array length
            if (prefixPos >= prefixes.length) {
                int oldLength = prefixes.length;
                int newLength = oldLength * 2;
                String[] newPrefixes = new String[newLength];
                String[] newURIs = new String[newLength];
                System.arraycopy(prefixes,0,newPrefixes,0,oldLength);
                System.arraycopy(uris,0,newURIs,0,oldLength);
                prefixes = newPrefixes;
                uris = newURIs;
            }

            prefixes[prefixPos] = prefix;
            uris[prefixPos] = uri;
        }
    }


    public String [] processName(String qName, String parts[],
                                 boolean isAttribute) {
        int colon = qName.indexOf(':');
        parts[2] = qName;
        if (colon < 0) {
            parts[1] = qName;

            if (isAttribute) // Attributes don't use the default URI
                parts[0] = "";
            else
                parts[0] = defaultURI;
            return parts;
        }
        else {
            String prefix = qName.substring(0,colon);
            parts[1] = qName.substring(colon+1);
            if ( (parts[0] = getURI(prefix)) == "" )
                return null;
            else
                return parts;
        }
    }


    public String getDefaultURI() {
        return defaultURI;
    }

    public String getURI(String prefix) {
        if (prefix == null || prefix.length() == 0)
            return defaultURI;
        else if (prefix == "xml")
            return XMLNS;

        for (int i=prefixPos; i >= 0; i--) {
            if (prefix == prefixes[i])
                return uris[i];
        }
        return "";
    }

    /// Returns the number of prefix mappings in the current context
    public int getContextSize() {
        return prefixCount
            + ((defaultURIContexts==0 && defaultURI != "")? 1 : 0);
    }

    public String getContextPrefix(int index) {
        // Do they want the default context?
        if (index == prefixCount
            && (defaultURIContexts==0 && defaultURI != ""))
            return "";

        return prefixes[prefixPos - index];
    }

    public String getContextURI(int index) {
        // Do they want the default context?
        if (index == prefixCount
            && (defaultURIContexts==0 && defaultURI != ""))
            return defaultURI;

        return uris[prefixPos - index];
    }

/* Here are the methods from NamespaceSupport that we don't implement:
    public Enumeration getPrefixes() {
    }

    public String getPrefix(String uri) {
    }

    public Enumeration getPrefixes(String uri) {
    }

    public Enumeration getDeclaredPrefixes() {
    }
*/
}
