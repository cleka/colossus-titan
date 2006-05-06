package net.sf.colossus.xmlparser;


import java.util.*;
import java.io.*;

import org.jdom.*;
import org.jdom.input.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.client.MasterHex;
import net.sf.colossus.server.Constants;


/**
 * StrategicMapLoader loads the masterboard data.
 * @author Romain Dolbeau
 * @version $Id$
 */
public class StrategicMapLoader
{
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
            Log.error("JDOM" + ex.toString());
        }
        catch (IOException ex)
        {
            Log.error("IO" + ex.toString());
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
