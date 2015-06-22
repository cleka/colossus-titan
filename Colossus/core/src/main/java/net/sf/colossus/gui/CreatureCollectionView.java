package net.sf.colossus.gui;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import net.sf.colossus.common.Options;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.variant.CreatureType;


/**
 * Viewer for a collection, say the graveyard or the creature keeper
 *
 * @author Tom Fruchterman
 * @author David Ripton
 */
class CreatureCollectionView extends KDialog
{
    private static final Logger LOGGER = Logger
        .getLogger(CreatureCollectionView.class.getName());

    private final ClientGUI gui;
    private final EditLegion editLegion;
    private static final int CHIT_SIZE = 60;

    private boolean gone;

    /**
     * Maps each creature type to the bottom label with all counts.
     */
    Map<CreatureType, JLabel> countMap = new HashMap<CreatureType, JLabel>();

    /**
     * Maps each creature type to the top label with the total count.
     */
    Map<CreatureType, JLabel> topCountMap = new HashMap<CreatureType, JLabel>();

    /**
     * Maps each creature type to the chit (for crossing out).
     */
    Map<CreatureType, Chit> chitMap = new HashMap<CreatureType, Chit>();

    private JScrollPane scrollPane;
    private JFrame parentFrame;
    private final static Font countFont = new Font("Monospaced", Font.PLAIN,
        12);
    private final static String baseString = "--/--/--";
    private final static JLabel baseLabel = new JLabel(baseString,
        SwingConstants.CENTER);
    private final static JLabel legendLabel = new JLabel(
        htmlizeOnly(htmlColorizeOnly("Values are: ", "black")
            + htmlColorizeOnly("Total", "blue")
            + htmlColorizeOnly("/", "black")
            + htmlColorizeOnly("In Stack", "black")
            + htmlColorizeOnly("/", "black")
            + htmlColorizeOnly("In Game", "green")
            + htmlColorizeOnly("/", "black") + htmlColorizeOnly("Dead", "red")));

    static
    {
        baseLabel.setFont(countFont);
    }

    CreatureCollectionView(JFrame frame, ClientGUI clientGui)
    {
        this(frame, clientGui, null);
    }

    CreatureCollectionView(JFrame frame, ClientGUI clientGui,
        EditLegion editLegion)
    {
        super(frame, "Caretaker's Stacks", false);
        this.parentFrame = frame;
        setFocusable(false);

        this.gui = clientGui;
        this.editLegion = editLegion;
        this.gone = false;

        getContentPane().setLayout(new BorderLayout());

        this.scrollPane = new JScrollPane(
            javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel panel = makeCreaturePanel(scrollPane);
        scrollPane.setViewportView(panel);
        JLabel infoText = new JLabel("Click on a creature for details!");
        getContentPane().add(infoText, BorderLayout.NORTH);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(legendLabel, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                CreatureCollectionView.this.gui.getOptions().setOption(
                    Options.showCaretaker, false);
            }
        });

        pack();

        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        useSaveWindow(gui.getOptions(), "CreatureCollectionView", null);

        update();
        setVisible(true);
    }

    /** the count for an individual creature */
    class CreatureCount extends JPanel
    {
        private final JLabel label;
        private final JLabel topLabel;
        private final Chit chit;

        CreatureCount(final CreatureType type)
        {
            super(new BorderLayout());

            setBorder(BorderFactory.createLineBorder(Color.black));
            if (type.isTitan())
            {
                chit = Chit.newCreatureChit(CHIT_SIZE, "Titan-0-Black");
            }
            else
            {
                chit = Chit.newCreatureChit(CHIT_SIZE, type);
            }
            chitMap.put(type, chit);
            label = new JLabel(baseString, SwingConstants.CENTER);
            topLabel = new JLabel(htmlizeOnly(htmlColorizeOnly(
                Integer.toString(type.getMaxCount()), "blue")),
                SwingConstants.CENTER);
            label.setFont(countFont);
            topLabel.setFont(countFont);
            countMap.put(type, label);
            topCountMap.put(type, topLabel);

            // clicking the creature icon invokes the details view
            this.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    if (e.getButton() == MouseEvent.BUTTON1)
                    {
                        if (editLegion != null)
                        {
                            editLegion.selectedCreature(type);
                            dispose();
                        }
                        else
                        {
                            new ShowCreatureDetails(
                                CreatureCollectionView.this.parentFrame, type,
                                null, CreatureCollectionView.this.scrollPane,
                                gui.getGame().getVariant(), gui);
                        }
                    }
                }
            });

            // jikes whines because add is defined in both JPanel and JDialog.
            this.add(topLabel, BorderLayout.NORTH);
            this.add(chit, BorderLayout.CENTER);
            this.add(label, BorderLayout.SOUTH);
        }

        @Override
        public Dimension getPreferredSize()
        {
            Dimension labelDim = label.getPreferredSize();
            Rectangle chitDim = chit.getBounds();
            int minX = chitDim.width + 1;
            int minY = chitDim.height + (2 * (int)labelDim.getHeight()) + 1;
            if (minX < (int)labelDim.getWidth() + 2)
            {
                minX = (int)labelDim.getWidth() + 2;
            }
            return new Dimension(minX, minY);
        }
    }

    private JPanel makeCreaturePanel(JScrollPane scrollPane)
    {
        JPanel creaturePanel = new JPanel();
        creaturePanel.setLayout(new CCVFlowLayout(scrollPane, creaturePanel,
            FlowLayout.LEFT, 2, 2));
        for (CreatureType type : gui.getGame().getVariant().getCreatureTypes())
        {
            creaturePanel.add(new CreatureCount(type));
        }

        return creaturePanel;
    }

    public void update()
    {
        try
        {
            for (Entry<CreatureType, JLabel> entry : countMap.entrySet())
            {
                CreatureType type = entry.getKey();
                JLabel label = entry.getValue();
                int count = gui.getGame().getCaretaker()
                    .getAvailableCount(type);
                int maxcount = type.getMaxCount();
                int deadCount = gui.getGame().getCaretaker()
                    .getDeadCount(type);
                int inGameCount = maxcount - (deadCount + count);

                // safety check
                if ((inGameCount < 0) || (inGameCount > maxcount))
                {
                    LOGGER.log(Level.SEVERE, "Something went wrong:"
                        + " discrepancy between total (" + maxcount
                        + "), remaining (" + count + ") and dead ("
                        + deadCount + ") count for creature " + type);
                    return;
                }

                boolean immortal = type.isImmortal();
                String color;
                if (count == 0)
                {
                    color = "yellow";
                    if (!immortal)
                    {
                        Chit chit = chitMap.get(type);
                        chit.setDead(true);
                    }
                }
                else
                {
                    Chit chit = chitMap.get(type);
                    chit.setDead(false);
                    if (count == maxcount)
                    {
                        color = "green";
                    }
                    else
                    {
                        color = "black";
                    }
                }
                String htmlCount = htmlColorizeOnly((count < 10 ? "0" : "")
                    + Integer.toString(count), color);
                String htmlDeadCount = htmlColorizeOnly(immortal
                    && deadCount == 0 ? "--" : (deadCount < 10 ? "0" : "")
                    + Integer.toString(deadCount), "red");
                String htmlInGameCount = htmlColorizeOnly(
                    (inGameCount < 10 ? "0" : "")
                        + Integer.toString(inGameCount), "green");
                String htmlSlash = htmlColorizeOnly("/", "black");
                label.setText(htmlizeOnly(htmlCount + htmlSlash
                    + htmlInGameCount + htmlSlash + htmlDeadCount));
                JLabel topLabel = topCountMap.get(type);
                topLabel.setText(htmlizeOnly(htmlColorizeOnly(
                    Integer.toString(maxcount), "blue")));
            }

            repaint();
        }
        catch (NullPointerException ex)
        {
            // If we try updating this dialog before creatures are loaded,
            // just ignore the exception and let it retry later.
        }
    }

    private static String htmlColorizeOnly(String input, String color)
    {
        StringBuilder sb = new StringBuilder("<font color=");
        sb.append(color);
        sb.append(">");
        sb.append(input);
        sb.append("</font>");
        return sb.toString();
    }

    private static String htmlizeOnly(String input)
    {
        StringBuilder sb = new StringBuilder("<html>");
        sb.append(input);
        sb.append("</html>");
        return sb.toString();
    }

    @Override
    public void dispose()
    {
        // Don't do anything if dispose already done.
        if (gone)
        {
            return;
        }

        gone = true;
        setVisible(false);

        // We MUST remove this. Otherwise the object does not get
        // garbage-collected.
        getContentPane().remove(legendLabel);

        parentFrame = null;
        scrollPane = null;

        countMap.clear();
        countMap = null;
        topCountMap.clear();
        topCountMap = null;
        chitMap.clear();
        chitMap = null;

        super.dispose();
    }

    @Override
    public Dimension getMinimumSize()
    {
        // default : 5 creatures wide

        int minSingleX = CHIT_SIZE + 8;
        if (minSingleX < (int)baseLabel.getPreferredSize().getWidth() + 8)
        {
            minSingleX = (int)baseLabel.getPreferredSize().getWidth() + 8;
        }

        int minX = minSingleX * 5;
        int minY = ((CHIT_SIZE + 8 + (2 * (int)baseLabel.getPreferredSize()
            .getHeight())) * ((gui.getGame().getVariant().getCreatureTypes()
            .size() + 4) / 5))
            + CHIT_SIZE;

        return new Dimension(minX, minY);
    }

    @Override
    public Dimension getPreferredSize()
    {
        return getMinimumSize();
    }
}
