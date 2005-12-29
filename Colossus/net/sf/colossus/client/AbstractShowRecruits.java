package net.sf.colossus.client;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;

import net.sf.colossus.server.Constants;
import net.sf.colossus.server.Creature;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.RecruitGraph;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;

/**
 * Common class for displaying recruit trees information.
 * @version $Id$
 * @author David Ripton
 * @author Barrie Treloar
 */
public class AbstractShowRecruits extends KDialog implements MouseListener,
        WindowListener
{

    AbstractShowRecruits(JFrame parentFrame)
    {
        super(parentFrame, "Recruits", false);

        setBackground(Color.lightGray);
        addWindowListener(this);
        getContentPane().setLayout(
                new BoxLayout(getContentPane(), BoxLayout.X_AXIS));

        addMouseListener(this);
    }

    void doOneTerrain(String terrain, String hexLabel)
    {
        Box panel = new Box(BoxLayout.Y_AXIS);
        panel.setAlignmentY(0);
        panel.setBorder(BorderFactory.createLineBorder(Color.black));
        panel.setBackground(TerrainRecruitLoader.getTerrainColor(terrain));

        JLabel terrainLabel = new JLabel(terrain);
        terrainLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(terrainLabel);

        List creatures = TerrainRecruitLoader.getPossibleRecruits(terrain,
                hexLabel);
        Iterator it = creatures.iterator();
        boolean firstTime = true;
        int scale = 4 * Scale.get();
        Creature prevCreature = Creature.getCreatureByName(Constants.titan);
        while (it.hasNext())
        {
            Creature creature = (Creature) it.next();

            int numToRecruit;
            if (firstTime)
            {
                numToRecruit = 0;
                firstTime = false;
            } else
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

            panel.add(numToRecruitLabel);
            numToRecruitLabel.addMouseListener(this);

            Chit chit = new Chit(scale, creature.getName(), this);
            panel.add(chit);
            chit.addMouseListener(this);

            chit.repaint();

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

    public void dispose()
    {
        super.dispose();
    }
}
