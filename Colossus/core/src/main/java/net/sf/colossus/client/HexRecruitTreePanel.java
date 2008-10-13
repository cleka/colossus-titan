package net.sf.colossus.client;


/**
 *  Arranges the recruit tree for one hex in a panel, which can be
 *  displayed e.g. in Autoinspector or right-click popup.
 *  
 *  @version $Id$
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;

import net.sf.colossus.server.Constants;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.RecruitGraph;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


public class HexRecruitTreePanel extends Box
{
    private HashMap<Chit, CreatureType> chitToCreatureMap
        = new HashMap<Chit, CreatureType>();

    private ArrayList<ShowCreatureDetails> creatureWindows
        = new ArrayList<ShowCreatureDetails>();

    private JFrame parentFrame;
    
    public HexRecruitTreePanel(int direction, MasterBoardTerrain terrain,
        MasterHex hex, JFrame parent, boolean clickable)
    {
        super(direction);
        this.parentFrame = parent;
        setAlignmentY(0);
        setBorder(BorderFactory.createLineBorder(Color.black));
        setBackground(terrain.getColor());

        JLabel terrainLabel = new JLabel(terrain.getDisplayName());
        terrainLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(terrainLabel);

        List<CreatureType> creatures = TerrainRecruitLoader
            .getPossibleRecruits(terrain, hex);
        Iterator<CreatureType> it = creatures.iterator();
        boolean firstTime = true;
        int scale = 4 * Scale.get();
        CreatureType prevCreature = VariantSupport.getCurrentVariant()
            .getCreatureByName(Constants.titan);
        while (it.hasNext())
        {
            CreatureType creature = it.next();

            int numToRecruit;
            if (firstTime)
            {
                numToRecruit = 0;
                firstTime = false;
            }
            else
            {
                numToRecruit = TerrainRecruitLoader.numberOfRecruiterNeeded(
                    prevCreature, creature, terrain, hex);
            }

            JLabel numToRecruitLabel = new JLabel("");
            if (numToRecruit > 0 && numToRecruit < RecruitGraph.BIGNUM)
            {
                numToRecruitLabel.setText(Integer.toString(numToRecruit));
                numToRecruitLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            }

            add(numToRecruitLabel);
            Chit chit = new Chit(scale, creature.getName());
            add(chit);
            chitToCreatureMap.put(chit, creature);

            // ShowAllRecruits windows uses this, Inspector right now not
            if (clickable)
            {
                MouseListener creListener = new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        if (e.getButton() == MouseEvent.BUTTON1)
                        {
                            showCreatureInfo(e);
                        }
                    }
                };
            
                chit.addMouseListener(creListener);
            }
            chit.repaint();

            prevCreature = creature;
        }
    }

    public void showCreatureInfo(MouseEvent e)
    {
        Object source = e.getSource();
        if (source instanceof Chit)
        {
            CreatureType type = chitToCreatureMap.get(source);
            ShowCreatureDetails creatureWindow = new 
                ShowCreatureDetails(this.parentFrame, type, null, null);
            creatureWindows.add(creatureWindow);
        }
        else
        {
            // showCreaturrInfo called for something which is not a chit?
        }
    }

    // ShowAllRecruits windows uses this, Inspector right now not
    public void closeCreatureWindows()
    {
        for (ShowCreatureDetails window : creatureWindows)
        {
            window.dispose();
        }
    }
}
