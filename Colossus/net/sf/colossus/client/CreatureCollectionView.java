package net.sf.colossus.client;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import net.sf.colossus.server.Creature;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Options;


/** 
 *  Viewer for a collection, say the graveyard or the creature keeper
 *  @version $Id$
 *  @author Tom Fruchterman
 *  @author David Ripton
 */
class CreatureCollectionView extends KDialog implements WindowListener
{
    private Client client;
    private Point location;

    /** hash by creature name to the label that displays the count */
    Map countMap = new HashMap();
    private SaveWindow saveWindow;
    private final static Font countFont =
        new Font("Monospaced", Font.PLAIN, 12);
    private final static String baseString = "--/--/--";
    private final static JLabel baseLabel =
        new JLabel(baseString, SwingConstants.CENTER);
    private final static JLabel legendLabel =
        new JLabel(htmlizeOnly(
            htmlColorizeOnly("Values are: ", "black") +
            htmlColorizeOnly("Total", "blue") +
            htmlColorizeOnly("/", "black") +
            htmlColorizeOnly("In Stack", "black") +
            htmlColorizeOnly("/", "black") +
            htmlColorizeOnly("In Game", "green") +
            htmlColorizeOnly("/", "black") +
            htmlColorizeOnly("Dead", "red")));
    
    static
    {
        baseLabel.setFont(countFont);
    }

    CreatureCollectionView(JFrame frame, Client client)
    {
        super(frame, "Caretaker's Stacks", false);

        this.client = client;

        getContentPane().setLayout(new BorderLayout());

        JPanel panel = makeCreaturePanel();
        getContentPane().add(panel, BorderLayout.CENTER);

        getContentPane().add(legendLabel, BorderLayout.SOUTH);

        addWindowListener(this);

        pack();

        saveWindow = new SaveWindow(client, "CreatureCollectionView");

        if (location == null)
        {
            location = saveWindow.loadLocation();
        }
        if (location == null)
        {
            upperRightCorner();
            location = getLocation();
        }
        else
        {
            setLocation(location);
        }
        update();
        setVisible(true);
    }

    /** the count for an individual creature */
    class CreatureCount extends JPanel
    {
        private JLabel label;
        private JLabel topLabel;
        private Chit chit;

        CreatureCount(String name)
        {
            super(new BorderLayout());
                                
            setBorder(BorderFactory.createLineBorder(Color.black));
            if (!(name.equals("Titan")))
                chit = new Chit(4 * Scale.get(), name, this);
            else
                chit = new Chit(4 * Scale.get(), "Titan-6-Black", this);
            label = new JLabel(baseString, SwingConstants.CENTER);
            topLabel =
                new JLabel(htmlizeOnly(
                             htmlColorizeOnly(
                               Integer.toString(
                                 client.getCreatureMaxCount(name)), "blue")),
                           SwingConstants.CENTER);
            label.setFont(countFont);
            topLabel.setFont(countFont);
            countMap.put(name, label);
            
            // jikes whines because add is defined in both JPanel and JDialog.
            this.add(topLabel, BorderLayout.NORTH);
            this.add(chit, BorderLayout.CENTER);
            this.add(label, BorderLayout.SOUTH);
        }

        public Dimension getPreferredSize()
        {
            Dimension labelDim = label.getPreferredSize();
            Rectangle chitDim = chit.getBounds();
            int minX = chitDim.width + 1;
            int minY = chitDim.height + (2 * (int)labelDim.getHeight()) + 1;
            if (minX < (int)labelDim.getWidth() + 2)
                minX = (int)labelDim.getWidth() + 2;
            return new Dimension(minX, minY);
        }
    }

    private JPanel makeCreaturePanel()
    {
        java.util.List creatures = Creature.getCreatures();
        JPanel creaturePanel = 
            new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
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
            int maxcount = client.getCreatureMaxCount(name);
            int deadCount = client.getCreatureDeadCount(name);
            int inGameCount = maxcount - (deadCount + count);
            String color;
            if (count == 0)
            {
                color = "yellow";
            }
            else if (count == maxcount)
            {
                color = "green";
            }
            else
            {
                color = "black";
            }
            String htmlCount =
                htmlColorizeOnly((count < 10 ? "0" : "") + 
                                 Integer.toString(count), color);
            String htmlTotalCount =
                htmlColorizeOnly((maxcount < 10 ? "0" : "") + 
                                 Integer.toString(maxcount),
                                 "blue");
            String htmlDeadCount =
                htmlColorizeOnly(
                             Creature.getCreatureByName(name).isImmortal() ?
                             "--" :
                             (deadCount < 10 ? "0" : "") + 
                             Integer.toString(deadCount),
                             "red");
            String htmlInGameCount =
                htmlColorizeOnly((inGameCount < 10 ? "0" : "") + 
                                 Integer.toString(inGameCount),
                                 "green");
            String htmlSlash = htmlColorizeOnly("/", "black");
            label.setText(htmlizeOnly(htmlCount + htmlSlash +
                                      htmlInGameCount + htmlSlash +
                                      htmlDeadCount));
        }

        repaint();
    }

    private static String htmlColorizeOnly(String input, String color)
    {
        StringBuffer sb = new StringBuffer("<font color=");
        sb.append(color);
        sb.append(">");
        sb.append(input);
        sb.append("</font>");
        return sb.toString();
    }

    private static String htmlizeOnly(String input)
    {
        StringBuffer sb = new StringBuffer("<html>");
        sb.append(input);
        sb.append("</html>");
        return sb.toString();
    }

    /** Wrap the input string with html font color tags. */
    private static String htmlColorize(String input, String color)
    {
        return htmlizeOnly(htmlColorizeOnly(input, color));
    }

    public void dispose()
    {
        super.dispose();
        location = getLocation();
        saveWindow.saveLocation(location);
    }

    void rescale()
    {
        getContentPane().removeAll();
        JPanel panel = makeCreaturePanel();
        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(legendLabel, BorderLayout.SOUTH);
        pack();
        upperRightCorner();
        location = getLocation();
        update();
    }

    public void windowClosing(WindowEvent e)
    {
        dispose();
    }

    public Dimension getPreferredSize()
    {
        java.util.List creatures = Creature.getCreatures();
        /* default : 5 creatures wide */
        
        int minSingleX = (4 * Scale.get()) + 8;
        if (minSingleX < (int)baseLabel.getPreferredSize().getWidth() + 8)
            minSingleX = (int)baseLabel.getPreferredSize().getWidth() + 8;

        int minX = minSingleX * 5;
        int minY = (((4 * Scale.get()) + 8 +
                     (2 * (int)baseLabel.getPreferredSize().getHeight())) *
                    ((creatures.size() + 4 ) / 5)) + 60;
        
        return new Dimension(minX, minY);
    }
}
