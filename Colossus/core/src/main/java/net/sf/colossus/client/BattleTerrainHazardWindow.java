package net.sf.colossus.client;


import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.sf.colossus.util.KDialog;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardConstants;
import net.sf.colossus.variant.HazardHexside;
import net.sf.colossus.variant.HazardTerrain;
import net.sf.colossus.variant.Hazards;
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
    private BattleMap map;
    private final JPanel chart;
    private final Variant variant;
    private final List<CreatureType> creatures;
    private Map<String, HazardTerrain> hazardsDisplayed;
    private Map<String, HazardHexside> hexsidesDisplayed;

    public BattleTerrainHazardWindow(JFrame frame, Client client, BattleMap map)

    {
        super(frame, "Battle Terrain Hazards for "
            + map.getMasterHex().getTerrain().getDisplayName(), false);

        this.map = map;
        variant = client.getGame().getVariant();
        creatures = variant.getCreatureTypes();
        chart = new JPanel();
        chart.setLayout(new GridLayout(0, 1));
        useSaveWindow(client.getOptions(), "PlayerDetails", null);

        getContentPane().add(chart);
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                dispose();
            }
        });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setupChart();

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                pack();
                setVisible(true);
            }
        });
    }

    private void setupChart()
    {
        hazardsDisplayed = new HashMap<String, HazardTerrain>();
        hexsidesDisplayed = new HashMap<String, HazardHexside>();
        for (HazardTerrain hazard : HazardTerrain.getAllHazardTerrains())
        {
            if (hazardsDisplayed.containsKey(hazard.getName())
                || map.getMasterHex().getTerrain().getHazardCount(hazard) == 0)
            {
                // Ignore
            }
            else
            {
                hazardsDisplayed.put(hazard.getName(), hazard);
                chart.add(addHazard(hazard));
            }
        }
        for (HazardHexside hazard : HazardHexside.getAllHazardHexsides())
        {
            if (hexsidesDisplayed.containsKey(hazard.getName())
                || map.getMasterHex().getTerrain().getHazardSideCount(
                    hazard.getCode()) == 0)
            {
                // Ignore
            }
            else
            {
                hexsidesDisplayed.put(hazard.getName(), hazard);
                chart.add(addHazard(hazard));
            }
        }
    }

    private JPanel addHazard(Hazards hazard)
    {
        JPanel hazardPanel = new JPanel(); // Row in Chart
        hazardPanel.setBorder(BorderFactory.createEtchedBorder());
        int scale = Scale.get();
        hazardPanel.add(new JLabel(hazard.getName()));

        hazardPanel.add(makeHexPanel(hazard, scale));
        hazardPanel.add(makeMovementPanel(hazard, scale));
        hazardPanel.add(makeNativesPanel(hazard, scale));
        hazardPanel.add(makeStrikePanel(hazard, scale));
        hazardPanel.add(makeDefenderPanel(scale));
        return hazardPanel;
    }

    // Create GUI representation of Terrain 
    private JPanel makeHexPanel(Hazards hazard, int scale)
    {
        JPanel hexPanel = new JPanel();
        hexPanel.setPreferredSize(new Dimension(scale * 8, scale * 8));
        hexPanel.setBorder(BorderFactory.createTitledBorder("Hex"));
        GUIBattleHex hex = new GUIBattleHex(hexPanel.getX() + scale, hexPanel
            .getY(), scale * 2, hexPanel, 0, 1);
        BattleHex model = new BattleHex(1, 1);
        if (hazard instanceof HazardTerrain)
        {
            model.setTerrain((HazardTerrain)hazard);
        }
        else
        {
            model.setTerrain(HazardTerrain.PLAINS);
            model.setHexside(0, hazard.getCode());
            model.setHexside(1, hazard.getCode());
            model.setHexside(2, hazard.getCode());
            model.setHexside(3, hazard.getCode());
            model.setHexside(4, hazard.getCode());
            model.setHexside(5, hazard.getCode());
        }
        hex.setHexModel(model);
        hexPanel.add(hex);
        return hexPanel;
    }

    // Show Native critters;
    private JPanel makeNativesPanel(Hazards hazard, int scale)
    {
        JPanel nativePanel = new JPanel(new GridLayout(0, 6));
        nativePanel.setBorder(BorderFactory.createTitledBorder("Natives"));
        nativePanel.setMinimumSize(new Dimension(scale, scale));
        Iterator<CreatureType> it = creatures.iterator();
        while (it.hasNext())
        {
            CreatureType creature = it.next();
            if (hazard instanceof HazardTerrain)
            {
                if (creature.isNativeIn((HazardTerrain)hazard))
                {
                    Chit chit = new Chit(60, creature.getName());
                    nativePanel.add(chit);
                }
            }
            else
            {
                if ((hazard.equals(HazardHexside.DUNE) && creature
                    .isNativeIn(HazardTerrain.SAND))
                    || (hazard.equals(HazardHexside.SLOPE) && creature
                        .isNativeSlope())
                    || (hazard.equals(HazardHexside.RIVER) && creature
                        .isNativeRiver()))
                {
                    Chit chit = new Chit(scale, creature.getName());
                    nativePanel.add(chit);
                }
            }
        }
        return nativePanel;
    }

    // Effect on Movement
    private JPanel makeMovementPanel(Hazards hazard, int scale)
    {
        JPanel movementPanel = new JPanel();
        movementPanel.setMinimumSize(new Dimension(scale, scale));
        movementPanel.setBorder(BorderFactory
            .createTitledBorder("Effect on Movement"));
        Chit flySymbol = null;
        if (hazard.effectOnFlyerMovement
            .equals(HazardConstants.EffectOnMovement.BLOCKALL))
        {
            flySymbol = new Chit(30, "FlyingBlocked");
            flySymbol.setToolTipText("FlyingBlocked");
        }
        else if (hazard.effectOnFlyerMovement
            .equals(HazardConstants.EffectOnMovement.BLOCKFOREIGNER))
        {
            flySymbol = new Chit(30, "FlyingNative");
            flySymbol.setToolTipText("Native Flyers Only");
        }
        else if (hazard.effectOnFlyerMovement
            .equals(HazardConstants.EffectOnMovement.SLOWALL))
        {
            flySymbol = new Chit(30, "FlyingSlow");
            flySymbol.setToolTipText("Slows Flying Creatures");
        }
        else if (hazard.effectOnFlyerMovement
            .equals(HazardConstants.EffectOnMovement.SLOWFOREIGNER))
        {
            flySymbol = new Chit(30, "FlyingNativeSlow");
            flySymbol.setToolTipText("Slows Non-Native Flying Creatures");
        }
        else
        {
            flySymbol = new Chit(30, "FlyingAll");
            flySymbol.setToolTipText("No effect on Flying Creatures");
        }
        movementPanel.add(flySymbol);
        Chit groundSymbol = null;
        if (hazard.effectOnGroundMovement
            .equals(HazardConstants.EffectOnMovement.BLOCKALL))
        {
            groundSymbol = new Chit(30, "GroundBlocked");
            groundSymbol.setToolTipText("Blocks Ground Movement");
        }
        else if (hazard.effectOnGroundMovement
            .equals(HazardConstants.EffectOnMovement.BLOCKFOREIGNER))
        {
            groundSymbol = new Chit(30, "GroundNativeOnly");
            groundSymbol.setToolTipText("Only Natives may Occupy");
        }
        else if (hazard.effectOnGroundMovement
            .equals(HazardConstants.EffectOnMovement.SLOWFOREIGNER))
        {
            groundSymbol = new Chit(30, "GroundNativeSlow");
            groundSymbol.setToolTipText("NonNatives Slowed");
        }
        else if (hazard.effectOnGroundMovement
            .equals(HazardConstants.EffectOnMovement.SLOWALL))
        {
            groundSymbol = new Chit(30, "GroundSlow");
            groundSymbol.setToolTipText("Slows Ground Movement");
        }
        else
        {
            groundSymbol = new Chit(30, "GroundAll");
            groundSymbol.setToolTipText("No Effect On Ground Movement");
        }
        movementPanel.add(groundSymbol);
        //        if (hazard instanceof HazardTerrain)
        //        {
        //            HazardTerrain terrain = (HazardTerrain)hazard;
        //            if (terrain.isNativeBonusTerrain())
        //            {
        //                Chit bonusSymbol = new Chit(30, "NativeSlow");
        //                movementPanel.add(bonusSymbol);
        //                bonusSymbol.setToolTipText("Natives get Bonuses");
        //            }
        //            if (terrain.isNonNativePenaltyTerrain())
        //            {
        //                Chit penaltySumbol = new Chit(30, "NativePenalty");
        //                penaltySumbol.setToolTipText("NonNatives get Penalty");
        //                movementPanel.add(penaltySumbol);
        //            }
        //        }
        return movementPanel;
    }

    private JPanel makeDefenderPanel(int scale)
    {
        JPanel defenderPanel = new JPanel();
        defenderPanel.setBorder(BorderFactory
            .createTitledBorder("Defence Bonus"));
        defenderPanel.setMinimumSize(new Dimension(scale, scale));
        // TODO ED - add Strike data when moved from strike to Hazard Terrain.
        return defenderPanel;
    }

    private JPanel makeStrikePanel(Hazards hazard, int scale)
    {
        JPanel strikePanel = new JPanel();
        strikePanel
            .setBorder(BorderFactory.createTitledBorder("Strike Bonus"));
        strikePanel.setMinimumSize(new Dimension(scale, scale));
        strikePanel.add(makeStrikeEffect("Attacking",
            hazard.effectforAttackingFromTerrain, hazard.scopeForAttackEffect,
            hazard.AttackEffectAdjustment));
        strikePanel.add(makeStrikeEffect("Rangestriking",
            hazard.effectforRangeStrikeFromTerrain,
            hazard.scopeForRangeStrikeEffect,
            hazard.RangeStrikeEffectAdjustment));
        return strikePanel;
    }

    private Chit makeStrikeEffect(String strike,
        HazardConstants.EffectOnStrike strikeEffect,
        HazardConstants.ScopeOfEffectOnStrike scope, int effectAdjustment)
    {
        Chit strikeSymbol;
        if (strikeEffect.equals(HazardConstants.EffectOnStrike.BLOCKED))
        {
            strikeSymbol = new Chit(30, "StrikeBlocked");
            strikeSymbol.setToolTipText(strike
                + " Across Hazard is not Possible");
        }
        else if (strikeEffect
            .equals(HazardConstants.EffectOnStrike.SKILLBONUS)
            || strikeEffect
                .equals(HazardConstants.EffectOnStrike.SKILLPENALTY))
        {
            strikeSymbol = new StrikeDie(30, 4 + effectAdjustment, "RedBlue");
            StringBuilder tip = new StringBuilder();
            if (scope.equals(HazardConstants.ScopeOfEffectOnStrike.FOREIGNERS)
                || scope
                    .equals(HazardConstants.ScopeOfEffectOnStrike.IMPERIALS))
            {
                tip.append("Non-Natives ");
            }
            else if (scope
                .equals(HazardConstants.ScopeOfEffectOnStrike.NATIVES)
                || scope
                    .equals(HazardConstants.ScopeOfEffectOnStrike.PATRIOTS))
            {
                tip.append("Natives ");
            }
            else
            {
                tip.append("Everyone ");
            }
            tip.append("have skill ");
            if (strikeEffect
                .equals(HazardConstants.EffectOnStrike.SKILLPENALTY))
            {
                tip.append("decreased by ");
            }
            else
            {
                tip.append("increased by ");
            }
            tip.append(effectAdjustment);
            tip.append(" when " + strike);
            if (scope.equals(HazardConstants.ScopeOfEffectOnStrike.IMPERIALS))
            {
                tip.append("Natives");
            }
            else if (scope
                .equals(HazardConstants.ScopeOfEffectOnStrike.PATRIOTS))
            {
                tip.append("Non-Natives");
            }
            strikeSymbol.setToolTipText(tip.toString());
        }
        else if (strikeEffect
            .equals(HazardConstants.EffectOnStrike.POWERBONUS)
            || strikeEffect
                .equals(HazardConstants.EffectOnStrike.POWERPENALTY))
        {
            strikeSymbol = new StrikeDie(30, 1, "RedBlue");
            StringBuilder tip = new StringBuilder();
            if (scope.equals(HazardConstants.ScopeOfEffectOnStrike.FOREIGNERS)
                || scope
                    .equals(HazardConstants.ScopeOfEffectOnStrike.IMPERIALS))
            {
                tip.append("Non-Natives ");
            }
            else if (scope
                .equals(HazardConstants.ScopeOfEffectOnStrike.NATIVES)
                || scope
                    .equals(HazardConstants.ScopeOfEffectOnStrike.PATRIOTS))
            {
                tip.append("Natives ");
            }
            else
            {
                tip.append("Everyone ");
            }
            if (strikeEffect
                .equals(HazardConstants.EffectOnStrike.POWERPENALTY))
            {
                tip.append("loses ");
            }
            else
            {
                tip.append("gains ");
            }
            tip.append(effectAdjustment);
            tip.append(" dice when " + strike);
            if (scope.equals(HazardConstants.ScopeOfEffectOnStrike.IMPERIALS))
            {
                tip.append("Natives");
            }
            else if (scope
                .equals(HazardConstants.ScopeOfEffectOnStrike.PATRIOTS))
            {
                tip.append("Non-Natives");
            }
            strikeSymbol.setToolTipText(tip.toString());
        }
        else
        {
            strikeSymbol = new StrikeDie(30, 4, "RedBlue");
            strikeSymbol.setToolTipText("Normal Strike");
        }
        return strikeSymbol;
    }

    @Override
    public void dispose()
    {
        // cleanPrefCBListeners();
        super.dispose();
        map = null;
    }

    public void actionPerformed(@SuppressWarnings("unused")
    ActionEvent e)
    {
        setVisible(false);
    }

}
