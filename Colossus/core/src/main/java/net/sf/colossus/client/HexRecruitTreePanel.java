package net.sf.colossus.client;


/**
 *  Arranges the recruit tree for one hex in a panel, which can be
 *  displayed e.g. in Autoinspector or right-click popup.
 *  
 *  @version $Id$
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;

import net.sf.colossus.server.Constants;
import net.sf.colossus.server.Creature;
import net.sf.colossus.util.RecruitGraph;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


public class HexRecruitTreePanel extends Box
{
    public HexRecruitTreePanel(int direction, String terrain, String hexLabel,
        MouseListener listener)
    {
        super(direction);
        setAlignmentY(0);
        setBorder(BorderFactory.createLineBorder(Color.black));
        setBackground(TerrainRecruitLoader.getTerrainColor(terrain));

        JLabel terrainLabel = new JLabel(terrain);
        terrainLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(terrainLabel);

        List<Creature> creatures = TerrainRecruitLoader.getPossibleRecruits(
            terrain, hexLabel);
        Iterator<Creature> it = creatures.iterator();
        boolean firstTime = true;
        int scale = 4 * Scale.get();
        Creature prevCreature = Creature.getCreatureByName(Constants.titan);
        while (it.hasNext())
        {
            Creature creature = it.next();

            int numToRecruit;
            if (firstTime)
            {
                numToRecruit = 0;
                firstTime = false;
            }
            else
            {
                numToRecruit = TerrainRecruitLoader.numberOfRecruiterNeeded(
                    prevCreature, creature, terrain, hexLabel);
            }

            JLabel numToRecruitLabel = new JLabel("");
            if (numToRecruit > 0 && numToRecruit < RecruitGraph.BIGNUM)
            {
                numToRecruitLabel.setText(Integer.toString(numToRecruit));
                numToRecruitLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            }

            add(numToRecruitLabel);
            numToRecruitLabel.addMouseListener(listener);

            Chit chit = new Chit(scale, creature.getName());
            add(chit);
            chit.addMouseListener(listener);

            chit.repaint();

            prevCreature = creature;
        }
    }
}
