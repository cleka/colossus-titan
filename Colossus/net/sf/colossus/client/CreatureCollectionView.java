package net.sf.colossus.client;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import net.sf.colossus.server.Creature;


/** Viewer for a collection, say the graveyard or the creature keeper
 * <P><B>TODO:</B>remove overlap with existing colossus
 *  @version $Id$
 *  @author Tom Fruchterman
 */
class CreatureCollectionView extends JDialog
{
    private Client client;
    private static Point location;


    CreatureCollectionView(JFrame frame, Client client)
    {
        super(frame);
        setTitle("Caretaker's Stacks");

        this.client = client;

        JPanel oPanel = makeCreaturePanel();
        getContentPane().add(oPanel, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                dispose();
            }
        });

        pack();

        if (location == null)
        {
            // Place dialog at upper right corner of screen.
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            location = new Point(d.width - getSize().width, 0);
        }
        setLocation(location);

        show();
    }

    /** hash by creature name to the label that displays the count */
    HashMap m_oCountHash = new HashMap();

    /** the count for an individual creature */
    class CreatureCount extends JPanel
    {
        String m_strName;

        CreatureCount(String strName)
        {
            super(new BorderLayout());
                                
            setBorder(BorderFactory.createLineBorder(Color.black));

            m_strName = strName;
            JLabel lblCreatureName = 
                new JLabel(strName, SwingConstants.CENTER);
            String strPath = Chit.getImagePath(strName);
            ImageIcon oIcon = Chit.getImageIcon(strPath);
            JLabel lblCreatureIcon = new JLabel(oIcon);
            JLabel lblCreatureCount = new JLabel(Integer.toString(
                client.getCreatureCount(strName)), SwingConstants.CENTER);
            m_oCountHash.put(strName, lblCreatureCount);

            // jikes whines because add is defined in both JPanel
            // and JDialog.
            this.add(lblCreatureName, BorderLayout.NORTH);
            this.add(lblCreatureIcon, BorderLayout.CENTER);
            this.add(lblCreatureCount, BorderLayout.SOUTH);
        }

        void update()
        {
        }
    }

    private JPanel makeCreaturePanel()
    {
        java.util.List oCharacterList = Creature.getCreatures();
        int nNumCharacters = oCharacterList.size();
        JPanel oCreaturePanel = 
            new JPanel(new GridLayout(5, nNumCharacters / 5));
        Iterator it = oCharacterList.iterator();
        while (it.hasNext())
        {
            Creature creature = (Creature)it.next();
            oCreaturePanel.add(new CreatureCount(creature.getName()));
        }
                        
        return oCreaturePanel;
    }

    public void update()
    {
        Iterator it = m_oCountHash.entrySet().iterator();
        while(it.hasNext())
        {
            Map.Entry entry = (Map.Entry)it.next();
            String strName = (String)entry.getKey();
            JLabel lbl = (JLabel)entry.getValue();
            int count = client.getCreatureCount(strName);
            String color;
            if (count == 0)
            {
                color = "red";
            }
            else if (count == Creature.getCreatureByName(strName).getMaxCount())
            {
                color = "green";
            }
            else
            {
                color = "black";
            }
            String htmlCount = htmlColorize(Integer.toString(count), color);
            lbl.setText(htmlCount);
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
}
