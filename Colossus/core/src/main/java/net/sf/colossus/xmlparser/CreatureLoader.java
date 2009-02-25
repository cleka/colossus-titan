package net.sf.colossus.xmlparser;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.variant.CreatureTypeTitan;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardTerrain;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


/**
 * CreatureLoader loads the creature descriptions.
 * @author Romain Dolbeau
 * @version $Id$
 * @see net.sf.colossus.server.CreatureType
 */
public class CreatureLoader
{
    private static final Logger LOGGER = Logger.getLogger(CreatureLoader.class
        .getName());
    private static final String currentVersion = "2";
    private final ArrayList<CreatureType> creatures;

    // we need to cast since JDOM is not generified
    @SuppressWarnings("unchecked")
    public CreatureLoader(InputStream creIS)
    {
        this.creatures = new ArrayList<CreatureType>();
        SAXBuilder builder = new SAXBuilder();
        try
        {
            Document doc = builder.build(creIS);
            Element root = doc.getRootElement();
            Attribute v = root.getAttribute("version");
            if ((v == null) ||
                (!v.getValue().equals(currentVersion))) {
                LOGGER.severe("Wrong / missing version in Creature file.");
            }

            List<Element> creatures = root.getChildren("creature");
            for (Element el : creatures)
            {
                handleCreature(el);
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

    /** Lookup attribute of name name in Element el, and return
     * its value as boolean. Assume lack of attribtue menas false.
     * @param el The element with the attribute (or not)
     * @param name The name of the attribute
     * @return The boolean value of the attribute, defaulting to false if absent
     */
    private boolean getAttributeBoolean(Element el, String name)
            throws JDOMException {
        Attribute a = el.getAttribute(name);
        if (a == null)
            return false;
        return a.getBooleanValue();
    }

    private void handleCreature(Element el) throws JDOMException
    {
        String name = el.getAttributeValue("name");
        int power = el.getAttribute("power").getIntValue();
        int skill = el.getAttribute("skill").getIntValue();
        boolean rangestrikes = getAttributeBoolean(el, "rangestrikes");
        boolean flies = getAttributeBoolean(el, "flies");
        Set<HazardTerrain> nativeTerrains = new HashSet<HazardTerrain>();

        for (HazardTerrain terrain : HazardTerrain.getAllHazardTerrains()) {
            if (getAttributeBoolean(el, terrain.getName())) {
                nativeTerrains.add(terrain);
            }
        }

        boolean slope = getAttributeBoolean(el, "slope");
        boolean river = getAttributeBoolean(el, "river");
        // maybe the next one should be split in its own attribute ?
        boolean water = getAttributeBoolean(el, "Lake");

        boolean magic_missile = getAttributeBoolean(el, "magic_missile");
        boolean summonable = getAttributeBoolean(el, "summonable");
        boolean lord = getAttributeBoolean(el, "lord");
        boolean demilord = getAttributeBoolean(el, "demilord");
        int count = el.getAttribute("count").getIntValue();
        String plural_name = el.getAttributeValue("plural_name");
        String base_color = el.getAttributeValue("base_color");

        CreatureType creature = null;
        if (name.equals("Titan"))
        {
            creature = new CreatureTypeTitan(name, power, skill, rangestrikes,
                flies, nativeTerrains, slope, river, water, magic_missile,
                summonable, lord, demilord, count, plural_name, base_color);
        }
        else
        {
            creature = new CreatureType(name, power, skill, rangestrikes,
                flies, nativeTerrains, slope, river, water, magic_missile,
                summonable, lord, demilord, count, plural_name, base_color);
        }
        this.creatures.add(creature);
    }

    public List<CreatureType> getCreatures()
    {
        return Collections.unmodifiableList(this.creatures);
    }
}
