package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Game;


/**
 * Displays recruit trees for all MasterHex types.
 * @version $Id$
 * @author David Ripton
 */

final class ShowAllRecruits extends JDialog implements MouseListener,
    WindowListener
{
    ShowAllRecruits(JFrame parentFrame, Client client)
    {
        super(parentFrame, "All Recruits", false);

        setBackground(Color.lightGray);
        addWindowListener(this);
        int scale = 4 * Scale.get();
        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.X_AXIS));

        char [] terrains = Game.getTerrains();
        for (int i = 0; i < terrains.length; i++)
        {
            char terrain = terrains[i];

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setAlignmentY(0);
            panel.setBorder(BorderFactory.createLineBorder(Color.black));
            panel.setBackground(Game.getTerrainColor(terrain));

            JLabel terrainLabel = new JLabel(MasterHex.getTerrainName(
                terrain));
            terrainLabel.setAlignmentX(Label.CENTER_ALIGNMENT);
            panel.add(terrainLabel);

            java.util.List creatures = Game.getPossibleRecruits(terrain);
            Iterator it = creatures.iterator();
            boolean firstTime = true;
            Creature prevCreature = Creature.getCreatureByName("Titan");
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
                    numToRecruit = Game.numberOfRecruiterNeeded(prevCreature,
                        creature, terrain);
                }
    
                JLabel numToRecruitLabel = new JLabel("", JLabel.CENTER);
                if (numToRecruit > 0 && numToRecruit <= 3)
                {
                    numToRecruitLabel.setText(Integer.toString(numToRecruit));
                    numToRecruitLabel.setAlignmentX(Label.CENTER_ALIGNMENT);
                }
    
                panel.add(numToRecruitLabel);
                numToRecruitLabel.addMouseListener(this);

                Chit chit = new Chit(scale, creature.getImageName(), this);
                panel.add(chit);
                chit.addMouseListener(this);
    
                prevCreature = creature;
            }
            contentPane.add(panel);
        }

        pack();
        addMouseListener(this);
        setVisible(true);
        repaint();
    }


    public void mouseClicked(MouseEvent e)
    {
        dispose();
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
        dispose();
    }

    public void mouseReleased(MouseEvent e)
    {
        dispose();
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
        dispose();
    }

    public void windowDeactivated(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowOpened(WindowEvent e)
    {
    }
}
