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

import  com.bluecast.util.*;
import  java.util.*;


/**
 * A class to hold information about an element defined
 * within an XML document type declaration.
 *
 * @author Yuval Oren, yuval@bluecast.com
 * @version $Revision$
 */
final public class ElementDefinition {
    String name;
    AttributeDefinition[] attributes;
    Map attributeMap;
    int size = 0;

    public ElementDefinition () {
        this(null);
    }

    public ElementDefinition (String name) {
        this.name = name;
        attributes = new AttributeDefinition[4];
        attributeMap = new HashMap();
        size = 0;
    }

    final public String getName () {
        return  name;
    }

    final public void setName (String name) {
        this.name = name;
    }

    final public AttributeDefinition[] getAttributes () {
        return  attributes;
    }

    final public int getAttributeCount () {
        return  size;
    }

    final public IndexedObject getIndexedAttribute (String name) {
        return  (IndexedObject)attributeMap.get(name);
    }

    final public AttributeDefinition getAttribute (int index) {
        return  attributes[index];
    }

    final public void addAttribute (AttributeDefinition attrib) throws DuplicateKeyException {
        Object newObj = new IndexedObjectImpl(size, attrib);
        Object oldObj = attributeMap.put(attrib.getQName(), newObj);
        // If there was already an attribute with this name, put the original back
        // and throw an exception.
        if (oldObj != null) {
            attributeMap.put(attrib.getQName(), oldObj);
            throw  new DuplicateKeyException("attribute '" + attrib.getQName()
                    + "' is already defined for element '" + name + "'.");
        }
        else {
            if (size >= attributes.length) {
                AttributeDefinition[] newAttributes = new AttributeDefinition[size*2];
                System.arraycopy(attributes, 0, newAttributes, 0, size);
                attributes = newAttributes;
            }
            attributes[size++] = attrib;
        }
    }
}



