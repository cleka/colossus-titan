package net.sf.colossus.client;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import net.sf.colossus.server.Creature;
import net.sf.colossus.util.KDialog;


/** 
 *  Viewer for a collection, say the graveyard or the creature keeper
 *  @version $Id$
 *  @author Tom Fruchterman
 *  @author David Ripton
 */
class CreatureCollectionView extends KDialog implements WindowListener
{
    private Client client;
    private static Point location;

    /** hash by creature name to the label that displays the count */
    Map countMap = new HashMap();


    CreatureCollectionView(JFrame frame, Client client)
    {
        super(frame, "Caretaker's Stacks", false);

        this.client = client;

        JPanel panel = makeCreaturePanel();
        getContentPane().add(panel, BorderLayout.CENTER);

        addWindowListener(this);

        pack();

        if (location == null)
        {
            upperRightCorner();
            location = getLocation();
        }
        else
        {
            setLocation(location);
        }
        setVisible(true);
    }

    /** the count for an individual creature */
    class CreatureCount extends JPanel
    {
        CreatureCount(String name)
        {
            super(new BorderLayout());
                                
            setBorder(BorderFactory.createLineBorder(Color.black));

            Chit chit = new Chit(4 * Scale.get(), name, this);
            JLabel label = new JLabel(Integer.toString(
                client.getCreatureCount(name)), SwingConstants.CENTER);
            countMap.put(name, label);

            // jikes whines because add is defined in both JPanel and JDialog.
            this.add(chit, BorderLayout.CENTER);
            this.add(label, BorderLayout.SOUTH);
        }
    }

    private JPanel makeCreaturePanel()
    {
        java.util.List creatures = Creature.getCreatures();
        JPanel creaturePanel = 
            new JPanel(new GridLayout(5, creatures.size() / 5));
        Iterator it = creatures.iterator();
        while (it.hasNext())
        {
            Creature creature = (Creature)it.next();
            creaturePanel.add(new CreatureCount(creature.getName()));
        }
                        
        return creaturePanel;
    }

    public void update()
    {
        Iterator it = countMap.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry entry = (Map.Entry)it.next();
            String name = (String)entry.getKey();
            JLabel label = (JLabel)entry.getValue();
            int count = client.getCreatureCount(name);
            String color;
            if (count == 0)
            {
                color = "red";
            }
            else if (count == Creature.getCreatureByName(name).
                getMaxCount())
            {
                color = "green";
            }
            else
            {
                color = "black";
            }
            String htmlCount = htmlColorize(Integer.toString(count), color);
            label.setText(htmlCount);
        }

        repaint();
    }


    /** Wrap the input string with html font color tags. */
    private String htmlColorize(String input, String color)
    {
        StringBuffer sb = new StringBuffer("<html><font color=");
        sb.append(color);
        sb.append(">");
        sb.append(input);
        sb.append("</font></html>");
        return sb.toString();
    }


    public void dispose()
    {
        super.dispose();
        location = getLocation();
    }

    void rescale()
    {
        getContentPane().removeAll();
        JPanel panel = makeCreaturePanel();
        getContentPane().add(panel, BorderLayout.CENTER);
        pack();
        upperRightCorner();
        location = getLocation();
        update();
    }

    public void windowClosing(WindowEvent e)
    {
        dispose();
    }
}
