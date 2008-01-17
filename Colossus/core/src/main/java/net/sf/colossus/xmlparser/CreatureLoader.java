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

import net.sf.colossus.server.CreatureTypeServerSide;
import net.sf.colossus.server.CreatureTitan;
import net.sf.colossus.variant.HazardTerrain;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


/**
 * CreatureLoader loads the creature descriptions.
 * @author Romain Dolbeau
 * @version $Id$
 * @see net.sf.colossus.server.CreatureTypeServerSide
 */
public class CreatureLoader
{
    private static final Logger LOGGER = Logger.getLogger(CreatureLoader.class
        .getName());

    private final ArrayList<CreatureTypeServerSide> creatures;

    // we need to cast since JDOM is not generified
    @SuppressWarnings("unchecked")
    public CreatureLoader(InputStream creIS)
    {
        this.creatures = new ArrayList<CreatureTypeServerSide>();
        SAXBuilder builder = new SAXBuilder();
        try
        {
            Document doc = builder.build(creIS);
            Element root = doc.getRootElement();

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

    private void handleCreature(Element el) throws JDOMException
    {
        String name = el.getAttributeValue("name");
        int power = el.getAttribute("power").getIntValue();
        int skill = el.getAttribute("skill").getIntValue();
        boolean rangestrikes = el.getAttribute("rangestrikes")
            .getBooleanValue();
        boolean flies = el.getAttribute("flies").getBooleanValue();
        Set<HazardTerrain> nativeTerrains = new HashSet<HazardTerrain>();

        // TODO why is this "bramble" while the string for the terrain is "Brambles"?
        if (el.getAttribute("bramble").getBooleanValue())
        {
            nativeTerrains.add(HazardTerrain.BRAMBLES);
        }
        if (el.getAttribute("drift").getBooleanValue())
        {
            nativeTerrains.add(HazardTerrain.DRIFT);
        }
        if (el.getAttribute("bog").getBooleanValue())
        {
            nativeTerrains.add(HazardTerrain.BOG);
        }
        if (el.getAttribute("sanddune").getBooleanValue())
        {
            nativeTerrains.add(HazardTerrain.SAND);
        }
        boolean slope = el.getAttribute("slope").getBooleanValue();
        if (el.getAttribute("volcano").getBooleanValue())
        {
            nativeTerrains.add(HazardTerrain.VOLCANO);
        }
        boolean river = el.getAttribute("river").getBooleanValue();
        if (el.getAttribute("stone").getBooleanValue())
        {
            nativeTerrains.add(HazardTerrain.STONE);
        }
        if (el.getAttribute("tree").getBooleanValue())
        {
            nativeTerrains.add(HazardTerrain.TREE);
        }
        boolean water = el.getAttribute("water").getBooleanValue();
        if (water)
        {
            nativeTerrains.add(HazardTerrain.LAKE);
        }
        boolean magic_missile = el.getAttribute("magic_missile")
            .getBooleanValue();
        boolean summonable = el.getAttribute("summonable").getBooleanValue();
        boolean lord = el.getAttribute("lord").getBooleanValue();
        boolean demilord = el.getAttribute("demilord").getBooleanValue();
        int count = el.getAttribute("count").getIntValue();
        String plural_name = el.getAttributeValue("plural_name");
        String base_color = el.getAttributeValue("base_color");

        CreatureTypeServerSide creature = null;
        if (name.equals("Titan"))
        {
            creature = new CreatureTitan(name, power, skill, rangestrikes,
                flies, nativeTerrains, slope, river, water, magic_missile,
                summonable, lord, demilord, count, plural_name, base_color);
        }
        else
        {
            creature = new CreatureTypeServerSide(name, power, skill, rangestrikes, flies,
                nativeTerrains, slope, river, water, magic_missile,
                summonable, lord, demilord, count, plural_name, base_color);
        }
        this.creatures.add(creature);
    }

    public List<CreatureTypeServerSide> getCreatures()
    {
        return Collections.unmodifiableList(this.creatures);
    }
}
