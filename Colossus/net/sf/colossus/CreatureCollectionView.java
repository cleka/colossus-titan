package net.sf.colossus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;


/** Viewer for a collection, say the graveyard or the creature keeper
 * <P><B>TODO:</B>remove overlap with existing colossus
 *  @version $Id$
 *  @author Tom Fruchterman
 */
public class CreatureCollectionView extends JDialog
{
    private ICreatureCollection m_oCollection;

    protected IImageUtility m_oImageUtility;

    private static Point location;


    public CreatureCollectionView(JFrame frame, ICreatureCollection oCollection,
        IImageUtility oImageUtility)
    {
        super(frame, oCollection.getName());

        m_oCollection = oCollection;
        m_oImageUtility = oImageUtility;

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
            String strPath = m_oImageUtility.getImagePath(strName);
            ImageIcon oIcon = m_oImageUtility.getImageIcon(strPath);
            JLabel lblCreatureIcon = new JLabel(oIcon);
            JLabel lblCreatureCount = new JLabel(Integer.toString(
                m_oCollection.getCount(strName)), SwingConstants.CENTER);
            m_oCountHash.put(strName, lblCreatureCount);
            add(lblCreatureName, BorderLayout.NORTH);
            add(lblCreatureIcon, BorderLayout.CENTER);
            add(lblCreatureCount, BorderLayout.SOUTH);
        }

        void update()
        {
        }
    }

    private JPanel makeCreaturePanel()
    {
        java.util.List oCharacterList = CharacterArchetype.getCharacters();
        int nNumCharacters = oCharacterList.size();
        JPanel oCreaturePanel = 
                new JPanel(new GridLayout(5, nNumCharacters / 5));
        Iterator i = oCharacterList.iterator();
        for(; i.hasNext();)
        {
            String strName = (String)i.next();
            oCreaturePanel.add(new CreatureCount(strName));
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
            int count = m_oCollection.getCount(strName);
            String color;
            if (count == 0)
            {
                color = "red";
            }
            else if (count == CharacterArchetype.getMaxCount(strName))
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
