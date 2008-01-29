/*
 * Created on 03.01.2004
 */
package net.sf.colossus.client;


import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowListener;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.sf.colossus.game.Creature;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Options;
import net.sf.colossus.variant.CreatureType;


/**
 * A dialog that displays information about a single player and his legions.
 */
public final class PlayerDetailsDialog extends KDialog implements
    MouseListener, WindowListener
{
    private final PlayerClientSide player;
    private final Client client;

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
        PlayerClientSide player, Client client)
    {
        // TODO currently modal since we don't listen for updates yet --> fix
        super(parentFrame, "Details for player " + player.getName(), true);
        this.player = player;
        this.client = client;

        useSaveWindow(client.getOptions(), "PlayerDetails", null);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.add(createSummaryTable());
        contentPane.add(createLegionsTable());
        contentPane.add(createCreaturesTable());

        addMouseListener(new MouseAdapter()
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

    private JPanel createSummaryTable()
    {
        JPanel result = new JPanel(new GridBagLayout());

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

        JLabel sectionLabel = new JLabel(String
            .valueOf(player.getNumLegions())
            + " legions:");
        result.add(sectionLabel, SECTION_TITLE_CONSTRAINT);

        result.add(new JPanel(), FIRST_LABEL_CONSTRAINT);
        result.add(new JLabel("Hex"), LABEL_CONSTRAINT);
        result.add(new JLabel("Content"), LABEL_CONSTRAINT);
        result.add(new JPanel(), HORIZONTAL_FILL_CONSTRAINT);

        for (LegionClientSide legion : player.getLegions())
        {
            result.add(new Marker(2 * Scale.get(), legion.getMarkerId()),
                FIRST_LABEL_CONSTRAINT);
            result.add(new JLabel(legion.getCurrentHex().getLabel()),
                LABEL_CONSTRAINT);
            result.add(new LegionInfoPanel(legion, 2 * Scale.get(), 0, 0,
                true, client.getViewMode(), client.getActivePlayer(), client
                    .getOptions().getOption(Options.dubiousAsBlanks), true),
                LABEL_CONSTRAINT);
            result.add(new JPanel(), HORIZONTAL_FILL_CONSTRAINT);
        }
        return result;
    }

    private JPanel createCreaturesTable()
    {
        JPanel result = new JPanel(new GridBagLayout());

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
            result.add(new Chit(2 * Scale.get(), type.getName()),
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