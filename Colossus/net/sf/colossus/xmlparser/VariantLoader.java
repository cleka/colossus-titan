package net.sf.colossus.xmlparser;


import java.util.*;
import java.io.*;

import org.jdom.*;
import org.jdom.input.*;

import net.sf.colossus.util.Log;


/**
 * Loads the top-level variant file
 * @author Romain Dolbeau
 * @version $Id$
 */
public class VariantLoader
{
    private String map;
    private String ter;
    private String cre;
    private String hintName;
    private int maxPlayers = 0;
    private ArrayList depends = new ArrayList();

    public VariantLoader(InputStream varIS)
    {
        SAXBuilder builder = new SAXBuilder();
        try
        {
            Document doc = builder.build(varIS);
            Element root = doc.getRootElement();
            // @todo check XML definition, here was: root.getAttributeValue("name");

            Element deps = root.getChild("depends");
            if (deps != null)
            {
                List dep = deps.getChildren("depend");
                for (Iterator it = dep.iterator(); it.hasNext();)
                {
                    Element el = (Element)it.next();
                    depends.add(el.getAttributeValue("variant"));
                }
            }

            Element hint = root.getChild("hint");
            if (hint != null)
            {
                hintName = hint.getAttributeValue("classname");
            }
            Element strategicMap = root.getChild("strategic_map");
            if (strategicMap != null)
            {
                map = strategicMap.getAttributeValue("filename");
            }
            Element creatures = root.getChild("creatures");
            if (creatures != null)
            {
                cre = creatures.getAttributeValue("filename");
            }
            Element terrain_recruits = root.getChild("terrain_recruits");
            if (terrain_recruits != null)
            {
                ter = terrain_recruits.getAttributeValue("filename");
            }
            Element max_players = root.getChild("max_players");
            if (max_players != null)
            {
                // towi: use value from num-attribute, not from elem:
                String s = max_players.getAttributeValue("num"); 
                maxPlayers = Integer.parseInt(s);
            }
        }
        catch (JDOMException ex)
        {
            Log.error("JDOM" + ex.toString());
        }
        catch (IOException ex)
        {
            Log.error("IO" + ex.toString());
        }
    }

    public String getMap()
    {
        return map;
    }

    public String getTer()
    {
        return ter;
    }

    public String getCre()
    {
        return cre;
    }

    public String getHintName()
    {
        return hintName;
    }

    public int getMaxPlayers()
    {
        return maxPlayers;
    }

    public List getDepends()
    {
        return (List)depends.clone();
    }

}
