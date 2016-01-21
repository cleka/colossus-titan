/*
 * Created on 03.01.2004
 */
package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.ScrollPane;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import net.sf.colossus.client.GameClientSide;
import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.client.PlayerClientSide;
import net.sf.colossus.client.PredictSplitNode;
import net.sf.colossus.client.PredictSplits;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.Creature;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.util.TimeFormats;
import net.sf.colossus.variant.CreatureType;


/**
 * A dialog that displays information about a single player and his legions.
 */
@SuppressWarnings("serial")
public final class PlayerDetailsDialog extends KDialog
{
    private final PlayerClientSide player;
    private final ClientGUI gui;

    private final JPanel mainPane;
    private final JButton refreshButton;

    private static final int MAX_CREATURE_COLUMNS = 6;

    private final static Insets INSETS = new Insets(5, 5, 5, 5);
    private final static GridBagConstraints LABEL_CONSTRAINT = new GridBagConstraints();
    static
    {
        LABEL_CONSTRAINT.gridx = GridBagConstraints.RELATIVE;
        LABEL_CONSTRAINT.gridy = GridBagConstraints.RELATIVE;
        LABEL_CONSTRAINT.anchor = GridBagConstraints.WEST;
        LABEL_CONSTRAINT.insets = INSETS;
    }
    private final static GridBagConstraints VALUE_CONSTRAINT = new GridBagConstraints();
    static
    {
        VALUE_CONSTRAINT.gridx = GridBagConstraints.RELATIVE;
        VALUE_CONSTRAINT.gridy = GridBagConstraints.RELATIVE;
        VALUE_CONSTRAINT.anchor = GridBagConstraints.WEST;
        VALUE_CONSTRAINT.insets = INSETS;
    }
    private final static GridBagConstraints FIRST_LABEL_CONSTRAINT = new GridBagConstraints();
    static
    {
        FIRST_LABEL_CONSTRAINT.gridx = 0;
        FIRST_LABEL_CONSTRAINT.gridy = GridBagConstraints.RELATIVE;
        FIRST_LABEL_CONSTRAINT.anchor = GridBagConstraints.WEST;
        FIRST_LABEL_CONSTRAINT.insets = INSETS;

    }
    private final static GridBagConstraints SECTION_TITLE_CONSTRAINT = new GridBagConstraints();
    static
    {
        SECTION_TITLE_CONSTRAINT.gridx = GridBagConstraints.RELATIVE;
        SECTION_TITLE_CONSTRAINT.gridwidth = GridBagConstraints.REMAINDER;
        SECTION_TITLE_CONSTRAINT.anchor = GridBagConstraints.WEST;
        SECTION_TITLE_CONSTRAINT.weightx = 1;
        SECTION_TITLE_CONSTRAINT.insets = INSETS;
    }
    private final static GridBagConstraints HORIZONTAL_FILL_CONSTRAINT = new GridBagConstraints();
    static
    {
        HORIZONTAL_FILL_CONSTRAINT.gridx = GridBagConstraints.RELATIVE;
        HORIZONTAL_FILL_CONSTRAINT.gridwidth = GridBagConstraints.REMAINDER;
        HORIZONTAL_FILL_CONSTRAINT.weightx = 1;
    }

    public PlayerDetailsDialog(final JFrame parentFrame,
        PlayerClientSide player, ClientGUI clientGui)
    {
        // TODO currently modal since we don't listen for updates yet --> fix
        super(parentFrame, "Details for player " + player.getName(), false);
        this.player = player;
        this.gui = clientGui;

        useSaveWindow(gui.getOptions(), "PlayerDetails", null);
        // int randomOffset = (int)(System.currentTimeMillis() % 5 - 10) * 10;
        // centerOnScreen(randomOffset, randomOffset);

        mainPane = new JPanel();
        JScrollPane scrollPane = new JScrollPane(mainPane);
        scrollPane.setAlignmentX(ScrollPane.LEFT_ALIGNMENT);
        getContentPane().add(scrollPane);
        mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.Y_AXIS));
        mainPane.setAlignmentX(LEFT_ALIGNMENT);

        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                refreshContent();
            }
        });

        addContent(mainPane);

        mainPane.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                dispose();
            }
        });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                pack();
                setVisible(true);
            }
        });
    }

    private void refreshContent()
    {
        mainPane.removeAll();
        addContent(mainPane);
        pack();
    }

    private void addContent(JPanel mainPane)
    {
        mainPane.add(createHeader());
        mainPane.add(createSummaryTable());
        mainPane.add(createLegionsTable());
        mainPane.add(createCreaturesTable());
    }

    private String getInfoText()
    {
        GameClientSide g = gui.getGameClientSide();
        return "turn " + g.getTurnNumber() + ", " + g.getActivePlayer() + " "
            + g.getPhase().getDoesWhat();
    }

    private Box createHeader()
    {
        Box labelBox = new Box(BoxLayout.X_AXIS);
        labelBox.setAlignmentX(LEFT_ALIGNMENT);
        labelBox.setBorder(new EmptyBorder(5, 5, 5, 5));

        JLabel refreshedLabel = new JLabel("Generated: "
            + TimeFormats.getCurrentTimeLocalized() + " (" + getInfoText()
            + ")  ", JLabel.LEFT);
        refreshedLabel.setHorizontalAlignment(JLabel.LEFT);
        refreshedLabel.setAlignmentX(LEFT_ALIGNMENT);

        labelBox.add(refreshedLabel);
        labelBox.add(Box.createHorizontalGlue());
        labelBox.add(refreshButton);
        return labelBox;
    }

    private JPanel createSummaryTable()
    {
        JPanel result = new JPanel(new GridBagLayout());
        result.setAlignmentX(LEFT_ALIGNMENT);

        result.add(new JLabel("Player:"), FIRST_LABEL_CONSTRAINT);
        result.add(new JLabel(player.getName()), VALUE_CONSTRAINT);
        result.add(new JLabel("Type:"), LABEL_CONSTRAINT);
        String type = player.getType();
        type = type.substring(type.lastIndexOf('.') + 1);
        result.add(new JLabel(type), VALUE_CONSTRAINT);
        result.add(new JPanel(), HORIZONTAL_FILL_CONSTRAINT);

        result.add(new JLabel("Score:"), FIRST_LABEL_CONSTRAINT);
        result.add(new JLabel(String.valueOf(player.getScore())),
            VALUE_CONSTRAINT);
        result.add(new JLabel("Titan power:"), LABEL_CONSTRAINT);
        result.add(new JLabel(String.valueOf(player.getTitanPower())),
            VALUE_CONSTRAINT);
        result.add(new JPanel(), HORIZONTAL_FILL_CONSTRAINT);

        return result;
    }

    private JPanel createLegionsTable()
    {
        JPanel result = new JPanel(new GridBagLayout());
        result.setAlignmentX(LEFT_ALIGNMENT);

        JLabel sectionLabel = new JLabel(
            String.valueOf(player.getNumLegions()) + " legions:");
        result.add(sectionLabel, SECTION_TITLE_CONSTRAINT);

        result.add(new JLabel("Legion"), FIRST_LABEL_CONSTRAINT);
        result.add(new JLabel(), LABEL_CONSTRAINT);
        result.add(new JLabel("Hex"), LABEL_CONSTRAINT);
        result.add(new JLabel("Content"), LABEL_CONSTRAINT);
        result.add(new JPanel(), HORIZONTAL_FILL_CONSTRAINT);

        boolean dubiousAsBlanks = gui.getOptions().getOption(
            Options.dubiousAsBlanks);

        List<LegionClientSide> legions = new ArrayList<LegionClientSide>();
        if (gui.getOptions().getOption(Options.legionListByMarkerId))
        {
            // TODO: this could probably been done much more elegant
            // but for now I just want it to work so that I can use it...
            List<String> markers = new ArrayList<String>();
            for (LegionClientSide legion : player.getLegions())
            {
                markers.add(legion.getMarkerId());
            }
            Collections.sort(markers);
            for (String markerId : markers)
            {
                legions.add(player.getLegionByMarkerId(markerId));
            }
        }
        else
        {
            legions.addAll(player.getLegions());
        }
        for (LegionClientSide legion : legions)
        {
            result.add(new JLabel(legion.getMarkerId()),
                FIRST_LABEL_CONSTRAINT);
            result.add(
                new Marker(legion, 2 * Scale.get(), legion.getLongMarkerId()),
                LABEL_CONSTRAINT);
            result.add(new JLabel(legion.getCurrentHex().getLabel()),
                LABEL_CONSTRAINT);
            result.add(new LegionInfoPanel(legion, 2 * Scale.get(), 0, 0,
                true, gui.getEffectiveViewMode(), gui.getClient().isMyLegion(legion),
                dubiousAsBlanks, true, false), LABEL_CONSTRAINT);
            result.add(new JPanel(), HORIZONTAL_FILL_CONSTRAINT);
        }

        result.add(new JLabel("Split history:"), SECTION_TITLE_CONSTRAINT);
        if (gui.getGame().getTurnNumber() < 512)
        {
            JScrollPane splitNodesPanel = new JScrollPane(
                createSplitNodesPanel());
            result.add(splitNodesPanel, SECTION_TITLE_CONSTRAINT);
        }
        else
        {
            result.add(new JLabel(
                " [ Too many turns! Table cannot be created. ] "),
                SECTION_TITLE_CONSTRAINT);
        }

        return result;
    }

    private JPanel createSplitNodesPanel()
    {
        JPanel result = new JPanel(new GridBagLayout());
        result.setAlignmentX(LEFT_ALIGNMENT);

        PredictSplits ps = player.getPredictSplits();
        PredictSplitNode root = ps.getRoot();
        Map<PredictSplitNode, GridBagConstraints> layouts = new HashMap<PredictSplitNode, GridBagConstraints>();
        GridBagConstraints rootConstraints = calculateSplitNodeLayout(root, 0,
            layouts);
        int totalHeight = rootConstraints.gridheight;

        for (Map.Entry<PredictSplitNode, GridBagConstraints> entry : layouts
            .entrySet())
        {
            PredictSplitNode node = entry.getKey();
            GridBagConstraints constraints = entry.getValue();
            // Currently we just place a simple label with the name, colored in a way
            // that the length on the timeline gets clear.
            // TODO: replace with some graphical representation of the node
            JLabel label = new JLabel(node.getFullName());
            label.setOpaque(true);
            label.setBackground(getIntermediateColor(SystemColor.control,
                Color.WHITE, constraints.gridy / (totalHeight - 1D)));
            result.add(label, constraints);
        }
        return result;
    }

    private Color getIntermediateColor(Color from, Color to, double pos)
    {
        assert (pos >= 0) && (pos <= 1) : "Position out of range [0,1]";
        int red = (int)(from.getRed() * (1 - pos) + to.getRed() * pos);
        int green = (int)(from.getGreen() * (1 - pos) + to.getGreen() * pos);
        int blue = (int)(from.getBlue() * (1 - pos) + to.getBlue() * pos);
        int alpha = (int)(from.getAlpha() * (1 - pos) + to.getAlpha() * pos);
        return new Color(red, green, blue, alpha);
    }

    // TODO it might be possible to just place the nodes directly instead of using
    //      the map to store the constraints first
    private GridBagConstraints calculateSplitNodeLayout(PredictSplitNode node,
        int y, Map<PredictSplitNode, GridBagConstraints> layouts)
    {
        // Layout concept: each parent spans the height of the two children, the
        // child that keeps the marker stays at the same level, the splitoff gets
        // placed just below the other.
        // Horizontally the legions are placed according to a timeline concept: from
        // the turn they were created to the turn they were split again.
        // TODO: what about killed legions?
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = node.getTurnCreated();
        constraints.gridy = y;
        constraints.fill = GridBagConstraints.BOTH;
        if (node.getChild1() != null)
        {
            GridBagConstraints child1Layout = calculateSplitNodeLayout(
                node.getChild1(), y, layouts);
            GridBagConstraints child2Layout = calculateSplitNodeLayout(
                node.getChild2(), y + child1Layout.gridheight, layouts);
            constraints.gridheight = child1Layout.gridheight
                + child2Layout.gridheight;
            constraints.gridwidth = node.getChild1().getTurnCreated()
                - node.getTurnCreated();
        }
        else
        {
            constraints.gridheight = 1;
            constraints.gridwidth = gui.getGame().getTurnNumber()
                - node.getTurnCreated();
        }
        layouts.put(node, constraints);
        return constraints;
    }

    private JPanel createCreaturesTable()
    {
        JPanel result = new JPanel(new GridBagLayout());
        result.setAlignmentX(LEFT_ALIGNMENT);

        result.add(new JLabel(String.valueOf(player.getNumCreatures())
            + " creatures (" + player.getTotalPointValue() + " points):"),
            SECTION_TITLE_CONSTRAINT);

        Map<CreatureType, Integer> creatureMap = new TreeMap<CreatureType, Integer>(
            CreatureType.NAME_ORDER);
        for (LegionClientSide legion : player.getLegions())
        {
            for (Creature creature : legion.getCreatures())
            {
                Integer count = creatureMap.get(creature.getType());
                if (count == null)
                {
                    count = Integer.valueOf(1);
                }
                else
                {
                    count = Integer.valueOf(count.intValue() + 1);
                }
                creatureMap.put(creature.getType(), count);
            }
        }

        int i = 0;
        int total = creatureMap.size();

        // one row plus another every time we get passed N times the maximum
        // per row -- this is effectively a rounding up on ints
        int rows = (total - 1) / MAX_CREATURE_COLUMNS + 1;

        // same rounding up trick
        int cols = (total - 1) / rows + 1;

        for (Map.Entry<CreatureType, Integer> entry : creatureMap.entrySet())
        {
            CreatureType type = entry.getKey();
            Integer count = entry.getValue();
            GridBagConstraints chitConstraint;
            if (i == 0)
            {
                chitConstraint = FIRST_LABEL_CONSTRAINT;
            }
            else
            {
                chitConstraint = LABEL_CONSTRAINT;
            }
            result.add(Chit.newCreatureChit(2 * Scale.get(), type),
                chitConstraint);
            result.add(new JLabel(count.toString()), VALUE_CONSTRAINT);
            i = (i + 1) % cols;
            if (i == 0)
            {
                result.add(new JPanel(), HORIZONTAL_FILL_CONSTRAINT);
            }
        }

        return result;
    }
}
