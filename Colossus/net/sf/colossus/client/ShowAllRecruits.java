package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Constants;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.parser.TerrainRecruitLoader;


/**
 * Displays recruit trees for all MasterHex types.
 * @version $Id$
 * @author David Ripton
 */

final class ShowAllRecruits extends KDialog implements MouseListener,
    WindowListener
{
    ShowAllRecruits(JFrame parentFrame, String[] terrains,
                    Point point,
                    String singleTerrainHexLabel)
    {
        super(parentFrame, "Recruits", false);

        setBackground(Color.lightGray);
        addWindowListener(this);
        getContentPane().setLayout(new BoxLayout(getContentPane(), 
            BoxLayout.X_AXIS));

        for (int i = 0; i < terrains.length; i++)
        {
            doOneTerrain(terrains[i], singleTerrainHexLabel);
        }

        pack();
        addMouseListener(this);

        if (point != null)
        {
            placeRelative(parentFrame, point);
        }

        setVisible(true);
        repaint();
    }

    void doOneTerrain(String terrain, String hexLabel)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentY(0);
        panel.setBorder(BorderFactory.createLineBorder(Color.black));
        panel.setBackground(TerrainRecruitLoader.getTerrainColor(terrain));

        JLabel terrainLabel = new JLabel(MasterHex.getTerrainName(terrain));
        terrainLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(terrainLabel);

        java.util.List creatures = 
            TerrainRecruitLoader.getPossibleRecruits(terrain, hexLabel);
        Iterator it = creatures.iterator();
        boolean firstTime = true;
        int scale = 4 * Scale.get();
        Creature prevCreature = Creature.getCreatureByName(Constants.titan);
        while (it.hasNext())
        {
            Creature creature = (Creature)it.next();
    
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
            if (numToRecruit > 0 && numToRecruit < 99)
            {
                numToRecruitLabel.setText(Integer.toString(numToRecruit));
                numToRecruitLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            }
    
            panel.add(numToRecruitLabel);
            numToRecruitLabel.addMouseListener(this);
            
            Chit chit = new Chit(scale, creature.getName(), this);
            panel.add(chit);
            chit.addMouseListener(this);
    
            prevCreature = creature;
        }
        getContentPane().add(panel);
    }


    public void mouseClicked(MouseEvent e)
    {
        dispose();
    }

    public void mousePressed(MouseEvent e)
    {
        dispose();
    }

    public void mouseReleased(MouseEvent e)
    {
        dispose();
    }

    public void windowClosing(WindowEvent e)
    {
        dispose();
    }
}
