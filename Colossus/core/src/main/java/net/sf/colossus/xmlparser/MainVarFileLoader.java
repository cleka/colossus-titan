package net.sf.colossus.xmlparser;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


/**
 * Loads the top-level variant file
 *
 * @author Romain Dolbeau
 */
public class MainVarFileLoader
{
    private static final Logger LOGGER = Logger
        .getLogger(MainVarFileLoader.class.getName());

    private String map;
    private String ter;
    final private List<String> cre = new ArrayList<String>();
    private String hintName;
    private int maxPlayers = 0;
    private final List<String> depends = new ArrayList<String>();

    // no generics in JDOM
    @SuppressWarnings("unchecked")
    public MainVarFileLoader(InputStream varIS)
    {
        SAXBuilder builder = new SAXBuilder();
        try
        {
            Document doc = builder.build(varIS);
            Element root = doc.getRootElement();
            // TODO check XML definition, here was: root.getAttributeValue("name");

            Element deps = root.getChild("depends");
            if (deps != null)
            {
                List<Element> dep = deps.getChildren("depend");
                for (Element el : dep)
                {
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
            List<Element> lcreatures = root.getChildren("creatures");
            for (Element creatures : lcreatures)
            {
                cre.add(creatures.getAttributeValue("filename"));
            }
            Element terrain_recruits = root.getChild("terrain_recruits");
            if (terrain_recruits != null)
            {
                ter = terrain_recruits.getAttributeValue("filename");
            }
            Element max_players = root.getChild("max_players");
            if (max_players != null)
            {
                String s = max_players.getAttributeValue("num");
                maxPlayers = Integer.parseInt(s);
            }
        }
        catch (JDOMException ex)
        {
            LOGGER.log(Level.SEVERE, "JDOM exception caught", ex);
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE, "IO exception caught", ex);
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

    public List<String> getCre()
    {
        assert !cre.isEmpty() : "No Creatures file listed in variant.";
        return Collections.unmodifiableList(cre);
    }

    public String getHintName()
    {
        return hintName;
    }

    public int getMaxPlayers()
    {
        return maxPlayers;
    }

    public List<String> getDepends()
    {
        return Collections.unmodifiableList(depends);
    }

}
