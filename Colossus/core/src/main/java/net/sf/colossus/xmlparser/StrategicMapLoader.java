package net.sf.colossus.xmlparser;


import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


/**
 * StrategicMapLoader loads the masterboard data.
 *
 * @author Romain Dolbeau
 */
public class StrategicMapLoader
{
    private static final Logger LOGGER = Logger
        .getLogger(StrategicMapLoader.class.getName());

    private boolean[][] show = null;
    private int horizSize = -1;
    private int vertSize = -1;
    private MasterHex[][] hexes = null;

    // we need to cast since JDOM is not generified
    @SuppressWarnings("unchecked")
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

            List<Element> hexlist = root.getChildren("hex");
            for (Element el : hexlist)
            {
                handleHex(el);
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

    // we need to cast since JDOM is not generified
    @SuppressWarnings("unchecked")
    private void handleHex(Element el) throws JDOMException
    {
        String label = el.getAttributeValue("label");
        String terrainId = el.getAttributeValue("terrain");
        int xpos = el.getAttribute("xpos").getIntValue();
        int ypos = el.getAttribute("ypos").getIntValue();

        this.show[xpos][ypos] = true;

        MasterBoardTerrain terrain = TerrainRecruitLoader
            .getTerrainById(terrainId);
        if (terrain == null)
        {
            LOGGER.warning("Null terrain in " + label + ", trying Plains");
            terrain = TerrainRecruitLoader.getTerrainById("Plains");
        }
        MasterHex hex = new MasterHex(label, terrain, xpos, ypos);

        List<Element> exits = el.getChildren("exit");
        int i = 0;
        for (Element exit : exits)
        {
            String sExitType = exit.getAttributeValue("type");
            Constants.HexsideGates iExitType = Constants.HexsideGates
                .valueOf(sExitType);
            hex.setBaseExitType(i, iExitType);
            String exitLabel = exit.getAttributeValue("label");
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
