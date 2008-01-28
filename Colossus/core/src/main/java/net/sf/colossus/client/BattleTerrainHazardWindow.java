package net.sf.colossus.client;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Options;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardTerrain;
import net.sf.colossus.variant.Variant;


/**
 * Class BattleTerrainHazardWindow shows a GUI representation of the 
 * Hazard Chart 
 * This is still ALPHA.
 * @version $Id: BattleTerrainHazardWindow.java 2975 2008-01-06 10:34:55Z peterbecker $
 * @author Dranathi
 */
public class BattleTerrainHazardWindow extends KDialog
{
    private IOptions options;
    private BattleMap map;
    private final JPanel chart;
    private final JButton closeButton;
    private final SaveWindow saveWindow;
    private final Variant variant;
    private final List<CreatureType> creatures;
    private Map<String, HazardTerrain> hazardsDisplayed;

    public BattleTerrainHazardWindow(JFrame frame, Client client, BattleMap map)

    {
        super(frame, "Battle Terrain Hazards for " + map.terrain, false);

        this.options = client.getOptions();
        this.map = map;
        variant = client.getGame().getVariant();
        creatures = variant.getCreatureTypes();
        chart = new JPanel();
        chart.setLayout(new GridLayout(0, 1));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(chart, BorderLayout.CENTER);
        closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                BattleTerrainHazardWindow.this.setVisible(false);
            }
        });
        getContentPane().add(closeButton, BorderLayout.SOUTH);

        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                BattleTerrainHazardWindow.this.options.setOption(
                    Options.BattleTerrainHazardWindow, false);
            }
        });
        saveWindow = new SaveWindow(options, "BattleTerrainHazardWindow");
        Point location = getUpperRightCorner(550);
        saveWindow.restore(this, location);

        setupChart();

        pack();
        setVisible(true);
    }

    private void setupChart()
    {
        hazardsDisplayed = new HashMap<String, HazardTerrain>();
        for (Iterator<HazardTerrain> iterator = HazardTerrain
            .getAllHazardTerrains().iterator(); iterator.hasNext();)
        {
            HazardTerrain hazard = iterator.next();
            if (hazardsDisplayed.containsKey(hazard.getName())
                || HexMap.getHazardCountInTerrain(hazard, map.terrain) == 0)
            {
                // Ignore
            }
            else
            {
                hazardsDisplayed.put(hazard.getName(), hazard);
                chart.add(addHazard(hazard));
            }
        }
        // TODO - Add Hex sides.
    }

    private JPanel addHazard(HazardTerrain terrain)
    {
        JPanel hazardPanel = new JPanel(); // Row in Chart
        hazardPanel.setBorder(BorderFactory.createEtchedBorder());
        int scale = Scale.get();
        hazardPanel.add(new JLabel(terrain.getName()));

        hazardPanel.add(makeHexPanel(terrain, scale));
        hazardPanel.add(makeMovementPanel(terrain, scale));
        hazardPanel.add(makeNativesPanel(terrain, scale));
        hazardPanel.add(makeStrikePanel(terrain, scale));
        hazardPanel.add(makeDefenderPanel(terrain, scale));
        return hazardPanel;
    }

    // Create GUI representation of Terrain 
    private JPanel makeHexPanel(HazardTerrain terrain, int scale)
    {
        JPanel hexPanel = new JPanel();
        hexPanel.setPreferredSize(new Dimension(scale * 7, scale * 7));
        hexPanel.setBorder(BorderFactory.createTitledBorder("Hex"));
        GUIBattleHex hex = new GUIBattleHex(hexPanel.getX() + scale, hexPanel
            .getY(), scale, hexPanel, 0, 1);
        BattleHex model = new BattleHex(1, 1);
        model.setTerrain(terrain);
        hex.setHexModel(model);
        hexPanel.add(hex);
        return hexPanel;
    }

    // Show Native critters;
    private JPanel makeNativesPanel(HazardTerrain terrain, int scale)
    {
        JPanel nativePanel = new JPanel();
        nativePanel.setBorder(BorderFactory.createTitledBorder("Natives"));
        nativePanel.setMinimumSize(new Dimension(scale, scale));
        Iterator<CreatureType> it = creatures.iterator();
        while (it.hasNext())
        {
            CreatureType creature = it.next();
            if (creature.isNativeIn(terrain))
            {
                Chit chit = new Chit(60, creature.getName());
                nativePanel.add(chit);
            }
        }
        return nativePanel;
    }

    // Effect on Movement
    private JPanel makeMovementPanel(HazardTerrain terrain, int scale)
    {
        JPanel movementPanel = new JPanel();
        movementPanel.setMinimumSize(new Dimension(scale, scale));
        movementPanel.setBorder(BorderFactory
            .createTitledBorder("Effect on Movement"));
        Chit flySymbol = null;
        if (terrain.blocksFlying())
        {
            flySymbol = new Chit(30, "FlyingBlocked");
            flySymbol.setToolTipText("FlyingBlocked");
        }
        else if (terrain.isNativeFlyersOnly())
        {
            flySymbol = new Chit(30, "FlyingNative");
            flySymbol.setToolTipText("Native Flyers Only");
        }
        else if (terrain.isFlyersOnly())
        {
            flySymbol = new Chit(30, "Flying");
            flySymbol.setToolTipText("Only Flying Creatures");
        }
        if (flySymbol != null)
        {
            movementPanel.add(flySymbol);
        }
        Chit moveSymbol = null;
        if (terrain.isNativeOnly())
        {
            moveSymbol = new Chit(30, "NativeOnly");
            moveSymbol.setToolTipText("Only Natives may Occupy");
        }
        else if (terrain.slowsNonNative())
        {
            moveSymbol = new Chit(30, "NativeSlow");
            moveSymbol.setToolTipText("NonNatives Slowed");
        }
        if (moveSymbol != null)
        {
            movementPanel.add(moveSymbol);
        }
        if (terrain.isNativeBonusTerrain())
        {
            Chit bonusSymbol = new Chit(30, "NativeBonus");
            movementPanel.add(bonusSymbol);
            bonusSymbol.setToolTipText("Natives get Bonuses");
        }
        if (terrain.isNonNativePenaltyTerrain())
        {
            Chit penaltySumbol = new Chit(30, "NativePenalty");
            penaltySumbol.setToolTipText("NonNatives get Penalty");
            movementPanel.add(penaltySumbol);
        }
        return movementPanel;
    }

    private JPanel makeDefenderPanel(HazardTerrain terrain, int scale)
    {
        JPanel defenderPanel = new JPanel();
        defenderPanel.setBorder(BorderFactory
            .createTitledBorder("Defence Bonus"));
        defenderPanel.setMinimumSize(new Dimension(scale, scale));
        // TODO add Strike data when moved from strike to Hazard Terrain.
        return defenderPanel;
    }

    private JPanel makeStrikePanel(HazardTerrain terrain, int scale)
    {
        JPanel strikePanel = new JPanel();
        strikePanel
            .setBorder(BorderFactory.createTitledBorder("Strike Bonus"));
        strikePanel.setMinimumSize(new Dimension(scale, scale));
        // TODO add Strike data when moved from strike to Hazard Terrain.
        return strikePanel;
    }

    @Override
    public void dispose()
    {
        // cleanPrefCBListeners();
        super.dispose();
        options = null;
        map = null;
    }

    public void actionPerformed(@SuppressWarnings("unused")
    ActionEvent e)
    {
        setVisible(false);
    }

}
