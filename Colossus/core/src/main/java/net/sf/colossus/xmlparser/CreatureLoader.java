package net.sf.colossus.xmlparser;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.server.Creature;
import net.sf.colossus.server.CreatureTitan;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


/**
 * CreatureLoader loads the creature descriptions.
 * @author Romain Dolbeau
 * @version $Id$
 * @see net.sf.colossus.server.Creature
 */
public class CreatureLoader
{
    private static final Logger LOGGER = Logger.getLogger(CreatureLoader.class
        .getName());

    private final ArrayList creatures;

    public CreatureLoader(InputStream creIS)
    {
        this.creatures = new ArrayList();
        SAXBuilder builder = new SAXBuilder();
        try
        {
            Document doc = builder.build(creIS);
            Element root = doc.getRootElement();

            List creatures = root.getChildren("creature");
            for (Iterator it = creatures.iterator(); it.hasNext();)
            {
                Element el = (Element)it.next();
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
        // TODO why is this "bramble" while the string for the terrain is "Brambles"?
        boolean bramble = el.getAttribute("bramble").getBooleanValue();
        boolean drift = el.getAttribute("drift").getBooleanValue();
        boolean bog = el.getAttribute("bog").getBooleanValue();
        boolean sanddune = el.getAttribute("sanddune").getBooleanValue();
        boolean slope = el.getAttribute("slope").getBooleanValue();
        boolean volcano = el.getAttribute("volcano").getBooleanValue();
        boolean river = el.getAttribute("river").getBooleanValue();
        boolean stone = el.getAttribute("stone").getBooleanValue();
        boolean tree = el.getAttribute("tree").getBooleanValue();
        boolean water = el.getAttribute("water").getBooleanValue();
        boolean magic_missile = el.getAttribute("magic_missile")
            .getBooleanValue();
        boolean summonable = el.getAttribute("summonable").getBooleanValue();
        boolean lord = el.getAttribute("lord").getBooleanValue();
        boolean demilord = el.getAttribute("demilord").getBooleanValue();
        int count = el.getAttribute("count").getIntValue();
        String plural_name = el.getAttributeValue("plural_name");
        String base_color = el.getAttributeValue("base_color");

        Creature creature = null;
        if (name.equals("Titan"))
        {
            creature = new CreatureTitan(name, power, skill, rangestrikes,
                flies, bramble, drift, bog, sanddune, slope, volcano, river,
                stone, tree, water, magic_missile, summonable, lord, demilord,
                count, plural_name, base_color);
        }
        else
        {
            creature = new Creature(name, power, skill, rangestrikes, flies,
                bramble, drift, bog, sanddune, slope, volcano, river, stone,
                tree, water, magic_missile, summonable, lord, demilord, count,
                plural_name, base_color);
        }
        this.creatures.add(creature);
    }

    public List getCreatures()
    {
        List copy = new ArrayList();
        try
        {
            copy.addAll(this.creatures);
        }
        catch (NullPointerException ex)
        {
            LOGGER.log(Level.WARNING, "Caught in CreatureLoader " + ex);
        }
        return copy;
    }
}
