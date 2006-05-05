package net.sf.colossus.xmlparser;


import java.util.*;
import java.io.*;

import org.jdom.*;
import org.jdom.input.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.client.BattleHex;


/**
 * BattlelandLoader loads the battle hex data
 * @author Romain Dolbeau
 * @version $Id$
 */
public class BattlelandLoader
{

    /** hold the list of label for the startlist */
    private java.util.List startlist = null;

    /** is the terrain a Tower ? */
    private boolean isTower = false;

    /** optional subtitle for the Battlelands */
    private String subtitle = null;

    public BattlelandLoader(InputStream batIS, BattleHex[][] h)
    {
        SAXBuilder builder = new SAXBuilder();
        try
        {
            Document doc = builder.build(batIS);
            Element root = doc.getRootElement();
            // @todo check XML definition, here was: root.getAttributeValue("terrain");
            isTower = root.getAttribute("tower").getBooleanValue();
            subtitle = root.getAttributeValue("subtitle");

            List hexlist = root.getChildren("battlehex");
            for (Iterator it = hexlist.iterator(); it.hasNext();)
            {
                Element el = (Element)it.next();
                handleHex(el, h);
            }
            Element startlistEl = root.getChild("startlist");
            if (startlistEl != null)
            {
                // towi: the DTD "battlehex" definitions clashed.
                //    renamed to "battlehexref"
                List startlistHexes = startlistEl.getChildren("battlehexref");
                if (startlistHexes.size() == 0)
                {
                    // support old format with warning
                    startlistHexes = startlistEl.getChildren("battlehex");
                    if (startlistHexes.size() > 0)
                    {
                        Log.warn("DEPRECATION WARNING: in 'startlist' use "
                            +"'battlehexref' instead of 'battlehex'!");
                    }                    
                }
                
                for (Iterator it = startlistHexes.iterator(); it.hasNext();)
                {
                    Element el = (Element)it.next();
                    handleStartlistHex(el);
                }
            }
        }
        catch (JDOMException ex)
        {
            // towi TODO : is it really good to swallow the exception? 
            Log.error("JDOM " + ex.toString());
        }
        catch (IOException ex)
        {
            // towi TODO: is it really good to swallow the exception? 
            Log.error("IO " + ex.toString());
        }
    }

    private void handleHex(Element el, BattleHex[][] h)
        throws JDOMException
    {
        int xpos = el.getAttribute("x").getIntValue();
        int ypos = el.getAttribute("y").getIntValue();
        BattleHex hex = h[xpos][ypos];

        String terrain = el.getAttributeValue("terrain");
        hex.setTerrain(terrain);

        int elevation = el.getAttribute("elevation").getIntValue();
        hex.setElevation(elevation);

        List borders = el.getChildren("border");
        for (Iterator it = borders.iterator(); it.hasNext();)
        {
            Element border = (Element)it.next();
            int number = border.getAttribute("number").getIntValue();
            char type = border.getAttributeValue("type").charAt(0);
            hex.setHexside(number, type);
        }
    }

    private void handleStartlistHex(Element el)
        throws JDOMException
    {
        String label = el.getAttributeValue("label");
        if (startlist == null && label != null)
        {
            startlist = new ArrayList();
        }
        if (label != null)
        {
            startlist.add(label);
        }
    }

    public java.util.List getStartList()
    {
        return startlist;
    }

    public String getSubtitle()
    {
        return subtitle;
    }

    public boolean isTower()
    {
        return isTower;
    }
}
