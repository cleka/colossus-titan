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


/**
 * A class to hold information about an attribute defined
 * within an XML document type declaration.
 *
 * @author Yuval Oren, yuval@bluecast.com
 * @version $Revision$
 */
final public class AttributeDefinition {
    // Attribute Defaults
    static public final int IMPLIED = 1;
    static public final int REQUIRED = 2;
    static public final int FIXED = 3;
    // Attribute Value Types
    static public final int ENUMERATION = 1;
    static public final int NOTATION = 2;
    static public final int CDATA = 3;
    static public final int ID = 4;
    static public final int IDREF = 5;
    static public final int IDREFS = 6;
    static public final int ENTITY = 7;
    static public final int ENTITIES = 8;
    static public final int NMTOKEN = 9;
    static public final int NMTOKENS = 10;
    static private final String[] valueTypeStrings =  {
        null, "ENUMERATION", "NOTATION", "CDATA", "ID", "IDREF", "IDREFS", "ENTITY",
                "ENTITIES", "NMTOKEN", "NMTOKENS"
    };
    static private final String[] defaultTypeStrings = {
        null, "#IMPLIED","#REQUIRED","#FIXED"
    };

    String prefix, localName, qName;
    int valueType;
    int defaultType;
    String defaultValue;
    String[] possibleValues;

    public AttributeDefinition (String prefix, String localName, String qName,
            int valueType, String[] possibleValues, int defaultType, String defaultValue) {
        this.prefix = prefix;
        this.localName = localName;
        this.qName = qName;
        this.valueType = valueType;
        this.possibleValues = possibleValues;
        this.defaultType = defaultType;
        this.defaultValue = defaultValue;
    }

    public String getPrefix () {
        return  prefix;
    }

    public String getLocalName () {
        return  localName;
    }

    public String getQName () {
        return  qName;
    }

    public int getValueType () {
        return  valueType;
    }

    public String getValueTypeString () {
        return  getValueTypeString(valueType);
    }

    static public String getValueTypeString (int valueType) {
        return  valueTypeStrings[valueType];
    }

    public int getDefaultType () {
        return  defaultType;
    }

    public String getDefaultTypeString () {
        return getDefaultTypeString(defaultType);
    }

    static public String getDefaultTypeString (int defaultType) {
        return defaultTypeStrings[defaultType];
    }

    public String getDefaultValue () {
        return  defaultValue;
    }

    public String[] getPossibleValues () {
        return  possibleValues;
    }
}



