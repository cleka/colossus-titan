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
import com.bluecast.util.FactoryServiceFinder;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import java.io.IOException;
import java.io.*;


/**
 * JAXP factory class for creating SAX parsers. This factory creates
 * an instance of Piccolo when a non-validating parser is requested.
 * <p>
 * If a validating parser is requested, this class will search for
 * another <code>SAXParserFactory</code> to create the validating parser.
 * This class will search for a factory in the following ways:
 * <ol>
 * <li>The system property <code>com.bluecast.xml.ValidatingSAXParserFactory</code>
 * <li>The next listed Service Provider for
 *      <code>javax.xml.parsers.SAXParserFactory</code>
 * <li>Crimson (<code>org.apache.crimson.jaxp.SAXParserFactoryImpl</code>)
 * </ol>
 * If all of the above fail, a <code>ParserConfigurationException</code>
 * will be thrown.
 *
 * @author Yuval Oren, yuval@bluecast.com
 * @version $Revision$
 */
public class JAXPSAXParserFactory extends SAXParserFactory {
    private Map featureMap = new HashMap();
    private static Boolean TRUE = new Boolean(true);
    private static Boolean FALSE = new Boolean(false);
    private Piccolo nvParser = new Piccolo();
    private SAXParserFactory validatingFactory;
    private static final String VALIDATING_PROPERTY 
        = "com.bluecast.xml.ValidatingSAXParserFactory";

    private static Class validatingFactoryClass;
    static {
        validatingFactoryClass = findValidatingFactory();
    }

    // Used to remember the exception we may get from switching
    // between validating and non-validating
    private ParserConfigurationException
        pendingValidatingException=null, pendingNonvalidatingException=null;

    private boolean validating=false,namespaceAware=false;

    public JAXPSAXParserFactory() {
        try {
            if (validatingFactoryClass != null) {
                validatingFactory =
                    (SAXParserFactory) validatingFactoryClass.newInstance();
                validatingFactory.setNamespaceAware(false);
                validatingFactory.setValidating(true);
            }
        }
        catch (InstantiationException e) {
            validatingFactory = null;
        }
        catch (IllegalAccessException e) {
            validatingFactory = null;
        }

        setNamespaceAware(false); // Force false default on Piccolo
    }


    public boolean getFeature(String name) throws ParserConfigurationException,
           SAXNotRecognizedException, SAXNotSupportedException {

        if (validating && validatingFactory != null)
            return validatingFactory.getFeature(name);
        else
            return nvParser.getFeature(name);
    }

    public SAXParser newSAXParser()
    throws ParserConfigurationException, SAXException {
        if (validating) {
            if (validatingFactory == null)
                throw new ParserConfigurationException(
                    "XML document validation is not supported");
            else
            if (pendingValidatingException != null)
                throw pendingValidatingException;
            else
                return validatingFactory.newSAXParser();
        }
        else {
            if (pendingNonvalidatingException != null)
                throw pendingNonvalidatingException;
            else
                return new JAXPSAXParser(new Piccolo(nvParser));
        }
    }

    public void setFeature(String name, boolean enabled)
    throws ParserConfigurationException,
           SAXNotRecognizedException, SAXNotSupportedException {

        // First explicitly set this feature
        featureMap.put(name, enabled? TRUE : FALSE);

        // Configure the validating factory
        if (validatingFactory != null) {
            if (pendingValidatingException != null)
                reconfigureValidating();
            else {
                try {
                    validatingFactory.setFeature(name,enabled);
                }
                catch (ParserConfigurationException e) {
                    pendingValidatingException = e;
                }
            }
        }

        // Configure the nonvalidating parser
        if (pendingNonvalidatingException != null)
            reconfigureNonvalidating();

        // Now throw the appropriate exception if there was a problem
        if (validating && pendingValidatingException != null)
            throw pendingValidatingException;
        else
        if (!validating && pendingNonvalidatingException != null)
            throw pendingNonvalidatingException;
    }

    public void setNamespaceAware(boolean awareness) {
        super.setNamespaceAware(awareness);
        namespaceAware = awareness;

        try {
            nvParser.setFeature(
                "http://xml.org/sax/features/namespaces",awareness);
            nvParser.setFeature(
                "http://xml.org/sax/features/namespace-prefixes",!awareness);
        }
        catch (SAXNotSupportedException e) {
            pendingNonvalidatingException = new ParserConfigurationException(
                "Error setting namespace feature: " + e.toString());
        }
        catch (SAXNotRecognizedException e) {
            pendingNonvalidatingException = new ParserConfigurationException(
                "Error setting namespace feature: " + e.toString());
        }

        if (validatingFactory != null)
            validatingFactory.setNamespaceAware(awareness);
    }

    public void setValidating(boolean value) {
        super.setValidating(value);
        validating = value;
    }

    private static Class findValidatingFactory() {
        // Find a factory for creating validating parsers

        // First try the system property
        try {
            String validatingClassName = System.getProperty(
                VALIDATING_PROPERTY);
            if (validatingClassName != null)
                return Class.forName(validatingClassName);
        }
        catch (ClassNotFoundException e) { }

        // Next try looking in jaxp.properties
        try {
            String javah=System.getProperty( "java.home" );
            String configFile = javah + File.separator +
                "lib" + File.separator + "jaxp.properties";
            File f=new File( configFile );
            if( f.exists()) {
                Properties props=new Properties();
                props.load( new FileInputStream(f));
                String validatingClassName = props.getProperty(VALIDATING_PROPERTY);
                if (validatingClassName != null)
                    return Class.forName(validatingClassName);
            }
        } catch(Exception e ) { }



        // Next try the Service Provider Interface
        try {
            Enumeration enum = FactoryServiceFinder.findServices(
                                    "javax.xml.parsers.SAXParserFactory");
            while (enum.hasMoreElements()) {
                try {
                    String factory = (String) enum.nextElement();
                    if (!factory.equals("com.bluecast.xml.Piccolo"))
                        return Class.forName(factory);
                }
                catch (ClassNotFoundException e) { }
            }
        }
        catch (IOException e) { }

        // Finally try Crimson
        try {
            return Class.forName(
                "org.apache.crimson.jaxp.SAXParserFactoryImpl");
        }
        catch (ClassNotFoundException e) {
            return null;
        }
    }


    private void reconfigureValidating() {
        if (validatingFactory == null)
            return;

        try {
            Iterator iter = featureMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                validatingFactory.setFeature(
                    (String)entry.getKey(),
                    ((Boolean)entry.getValue()).booleanValue());
            }
        }
        catch (ParserConfigurationException e) {
            pendingValidatingException = e;
        }
        catch (SAXNotRecognizedException e) {
            pendingValidatingException =
                new ParserConfigurationException(e.toString());
        }
        catch (SAXNotSupportedException e) {
            pendingValidatingException =
                new ParserConfigurationException(e.toString());
        }
    }

    private void reconfigureNonvalidating() {
        try {
            Iterator iter = featureMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                nvParser.setFeature(
                    (String)entry.getKey(),
                    ((Boolean)entry.getValue()).booleanValue());
            }
        }
        catch (SAXNotRecognizedException e) {
            pendingNonvalidatingException =
                new ParserConfigurationException(e.toString());
        }
        catch (SAXNotSupportedException e) {
            pendingNonvalidatingException =
                new ParserConfigurationException(e.toString());
        }
    }

    static class JAXPSAXParser extends SAXParser {
        Piccolo parser;

        JAXPSAXParser(Piccolo parser) {
            this.parser = parser;
        }

        public Parser getParser() { return parser; }

        public Object getProperty(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException {
            return parser.getProperty(name);
        }

        public XMLReader getXMLReader() { return parser; }

        public boolean isNamespaceAware() {
            return parser.fNamespaces;
        }

        public boolean isValidating() { return false; }

        public void setProperty(String name, Object value)
        throws SAXNotRecognizedException, SAXNotSupportedException {
            parser.setProperty(name,value);
        }
    }
}



