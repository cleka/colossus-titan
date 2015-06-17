package net.sf.colossus.xmlparser;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.util.ObjectCreationException;
import net.sf.colossus.util.StaticResourceLoader;
import net.sf.colossus.variant.AllCreatureType;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.CreatureTypeTitan;
import net.sf.colossus.variant.HazardTerrain;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


/**
 * CreatureLoader loads the creature descriptions.
 * @author Romain Dolbeau
 * @version $Id: CreatureLoader.java 4053 2009-04-21 12:40:24Z dolbeau $
 * @see net.sf.colossus.variant.CreatureType
 */
public class CreatureLoader implements AllCreatureType
{
    private static final Logger LOGGER = Logger.getLogger(CreatureLoader.class
        .getName());
    private static final String currentVersion = "2";
    private final SortedSet<CreatureType> creatures;
    private final Map<String, CreatureType> byName = new TreeMap<String, CreatureType>();

    public CreatureLoader()
    {
        this.creatures = new TreeSet<CreatureType>();
    }

    // we need to cast since JDOM is not generified
    @SuppressWarnings("unchecked")
    public void fillCreatureLoader(InputStream creIS,
        List<String> varDirectoriesList)
    {
        SAXBuilder builder = new SAXBuilder();
        try
        {
            Document doc = builder.build(creIS);
            Element root = doc.getRootElement();
            Attribute v = root.getAttribute("version");
            if ((v == null) || (!v.getValue().equals(currentVersion)))
            {
                LOGGER.severe("Wrong / missing version in Creature file.");
            }

            List<Element> lcreatures = root.getChildren("creature");
            for (Element el : lcreatures)
            {
                handleCreature(el, varDirectoriesList);
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
        catch (ObjectCreationException ex)
        {
            LOGGER.log(Level.SEVERE, "Failed to create custom object", ex);
        }
    }

    /** Lookup attribute of name name in Element el, and return
     * its value as boolean. Assume lack of attribute means false.
     * @param el The element with the attribute (or not)
     * @param name The name of the attribute
     * @return The boolean value of the attribute, defaulting to false if absent
     */
    private boolean getAttributeBoolean(Element el, String name)
        throws JDOMException
    {
        Attribute a = el.getAttribute(name);
        if (a == null)
            return false;
        return a.getBooleanValue();
    }

    @SuppressWarnings("boxing")
    private void handleCreature(Element el, List<String> varDirectoriesList)
        throws JDOMException, ObjectCreationException
    {
        String name = el.getAttributeValue("name");
        int power = el.getAttribute("power").getIntValue();
        int skill = el.getAttribute("skill").getIntValue();
        boolean rangestrikes = getAttributeBoolean(el, "rangestrikes");
        boolean flies = getAttributeBoolean(el, "flies");
        Set<HazardTerrain> nativeTerrains = new HashSet<HazardTerrain>();

        for (HazardTerrain terrain : HazardTerrain.getAllHazardTerrains())
        {
            if (getAttributeBoolean(el, terrain.getName()))
            {
                nativeTerrains.add(terrain);
            }
        }

        boolean slope = getAttributeBoolean(el, "slope");
        boolean river = getAttributeBoolean(el, "river");
        // maybe the next one should be split in its own attribute ?
        boolean water = getAttributeBoolean(el, "Lake");
        // maybe the next one should be split in its own attribute ?
        boolean dune = getAttributeBoolean(el, "Sand");

        boolean magic_missile = getAttributeBoolean(el, "magic_missile");
        boolean summonable = getAttributeBoolean(el, "summonable");
        boolean lord = getAttributeBoolean(el, "lord");
        boolean demilord = getAttributeBoolean(el, "demilord");
        int count = el.getAttribute("count").getIntValue();
        String plural_name = el.getAttributeValue("plural_name");
        String base_color = el.getAttributeValue("base_color");

        int poison;
        try
        {
            poison = el.getAttribute("poison").getIntValue();
        }
        catch (Exception e)
        {
            // Don't fail if older version doesn't have poison
            poison = 0;
        }

        int slows;
        try
        {
            slows = el.getAttribute("slows").getIntValue();
        }
        catch (Exception e)
        {
            // Don't fail if older version doesn't have slows
            slows = 0;
        }

        String custom_class = el.getAttributeValue("special");

        CreatureType creature = null;
        if (custom_class == null)
        {
            if (name.equals("Titan"))
            {
                creature = new CreatureTypeTitan(name, power, skill,
                    rangestrikes, flies, nativeTerrains, slope, river, dune,
                    water, magic_missile, summonable, lord, demilord, count,
                    plural_name, base_color, poison, slows);
            }
            else
            {
                creature = new CreatureType(name, power, skill, rangestrikes,
                    flies, nativeTerrains, slope, river, dune, water,
                    magic_missile, summonable, lord, demilord, count,
                    plural_name, base_color, poison, slows);
            }
        }
        else
        {
            Object[] parameters = new Object[19];
            parameters[0] = name;
            parameters[1] = power;
            parameters[2] = skill;
            parameters[3] = rangestrikes;
            parameters[4] = flies;
            parameters[5] = nativeTerrains;
            parameters[6] = slope;
            parameters[7] = river;
            parameters[8] = dune;
            parameters[9] = water;
            parameters[10] = magic_missile;
            parameters[11] = summonable;
            parameters[12] = lord;
            parameters[13] = demilord;
            parameters[14] = count;
            parameters[15] = plural_name;
            parameters[16] = base_color;
            parameters[17] = poison;
            parameters[18] = slows;
            creature = (CreatureType)StaticResourceLoader.getNewObject(
                custom_class, varDirectoriesList, parameters);
        }
        this.creatures.add(creature);
        this.byName.put(name, creature);
    }

    public List<CreatureType> getCreatureTypesAsList()
    {
        return Collections.unmodifiableList(new ArrayList<CreatureType>(
            this.creatures));
    }

    public SortedSet<CreatureType> getCreatureTypes()
    {
        return Collections.unmodifiableSortedSet(this.creatures);
    }

    public CreatureType getCreatureTypeByName(String name)
    {
        return this.byName.get(name);
    }
}
