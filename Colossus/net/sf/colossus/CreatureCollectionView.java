package net.sf.colossus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

// XXX Importing from the default package is not recommended and might
// not work in some JDKs.
import Log;
import Client;
import Options;


/** Viewer for a collection, say the graveyard or the creature keeper
 * <P><B>TODO:</B>remove overlap with existing colossus
 *  @version $Id$
 *  @author Tom Fruchterman
 */
public class CreatureCollectionView extends JFrame
{
    private ICreatureCollection m_oCollection;

    protected IImageUtility m_oImageUtility;

    CreatureCount m_oCreatureCount;

    private Client client;
    private static Point location;

    private static boolean disposeLock;


    public CreatureCollectionView(ICreatureCollection oCollection,
                                  IImageUtility oImageUtility,
                                  final Client client)
    {
        // third arg says we are NOT modal
        super(oCollection.getName());

        m_oCollection = oCollection;
        m_oImageUtility = oImageUtility;

        // XXX This may be unclean.
        if (client != null)
        {
            this.client = client;
        }

        JPanel oButtonPanel = new JPanel(new FlowLayout());
        JPanel oPanel = makeCreaturePanel();
        getContentPane().add(oPanel, BorderLayout.CENTER);

        getContentPane().add(oButtonPanel, BorderLayout.SOUTH);
        JButton oCloseButton = new JButton("Close");
        oButtonPanel.add(oCloseButton);
        oCloseButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evtAction)
            { 
                dispose();
            }
        });

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
            // Place dialog at middle right side of screen.
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            location = new Point(d.width - getSize().width,
                (d.height - getSize().height) / 2);
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
            String strName = (String) i.next();
            oCreaturePanel.add(new CreatureCount(strName));
        }
                        
        return oCreaturePanel;
    }

    public void update()
    {
        Log.debug("CreatureCollectionView.update");

        Iterator it = m_oCountHash.entrySet().iterator();
        while(it.hasNext())
        {
            Map.Entry entry = (Map.Entry)it.next();
            String strName = (String)entry.getKey();
            JLabel lbl = (JLabel)entry.getValue();
            String strNewCount = Integer.toString(
                m_oCollection.getCount(strName));
            String strOldCount = lbl.getText();
            Log.debug("CreatureCollectionView.update: " 
                + strName + ", from " + strOldCount 
                + " to " + strNewCount);
            lbl.setText(strNewCount);
        }

        repaint();
    }


    public void dispose()
    {
        super.dispose();
        location = getLocation();

        // Tell the client to turn off the menu option.
        if (client != null && !disposeLock)
        {
            disposeLock = true;
            client.setOption(Options.showCaretaker, false);
        }
        disposeLock = false;
    }
}
