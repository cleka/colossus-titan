package net.sf.colossus.xmlparser;


import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.MasterHex;
import net.sf.colossus.server.Constants;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


/**
 * StrategicMapLoader loads the masterboard data.
 * @author Romain Dolbeau
 * @version $Id$
 */
public class StrategicMapLoader
{
	private static final Logger LOGGER = Logger.getLogger(StrategicMapLoader.class.getName());

    private boolean[][] show = null;
    private int horizSize = -1;
    private int vertSize = -1;
    private MasterHex[][] hexes = null;

    public StrategicMapLoader(InputStream mapIS)
    {
        SAXBuilder builder = new SAXBuilder();
        try
        {
            Document doc = builder.build(mapIS);
            Element root = doc.getRootElement();
            horizSize = root.getAttribute("width").getIntValue();
            vertSize = root.getAttribute("height").getIntValue();
            hexes = new MasterHex[this.horizSize][this.vertSize];
            show = new boolean[this.horizSize][this.vertSize];

            List hexlist = root.getChildren("hex");
            for (Iterator it = hexlist.iterator(); it.hasNext();)
            {
                Element el = (Element)it.next();
                handleHex(el);
            }
        }
        catch (JDOMException ex)
        {
            LOGGER.log(Level.SEVERE, "JDOM" + ex.toString(), (Throwable)null);
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE, "IO" + ex.toString(), (Throwable)null);
        }
    }

    private void handleHex(Element el)
        throws JDOMException
    {
        String label = el.getAttributeValue("label");
        String terrain = el.getAttributeValue("terrain");
        int xpos = el.getAttribute("xpos").getIntValue();
        int ypos = el.getAttribute("ypos").getIntValue();

        this.show[xpos][ypos] = true;

        MasterHex hex = new MasterHex();
        hex.setLabel(label);
        hex.setTerrain(terrain);
        hex.setXCoord(xpos);
        hex.setYCoord(ypos);

        List exits = el.getChildren("exit");
        int i = 0;
        for (Iterator it = exits.iterator(); it.hasNext();)
        {
            Element exit = (Element)it.next();
            String sExitType = exit.getAttributeValue("type");
            int iExitType = ((Integer)Constants.hexsideMap.get(
                    sExitType)).intValue();
            hex.setBaseExitType(i, iExitType);
            int exitLabel = exit.getAttribute("label").getIntValue();
            hex.setBaseExitLabel(i, exitLabel);
            i++;
        }

        this.hexes[xpos][ypos] = hex;
    }

    public int getHorizSize()
    {
        return horizSize;
    }

    public int getVertSize()
    {
        return vertSize;
    }

    public boolean[][] getShow()
    {
        return show;
    }

    public MasterHex[][] getHexes()
    {
        return hexes;
    }
}
