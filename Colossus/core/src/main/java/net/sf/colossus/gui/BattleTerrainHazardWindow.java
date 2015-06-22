package net.sf.colossus.gui;


import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardConstants;
import net.sf.colossus.variant.HazardHexside;
import net.sf.colossus.variant.HazardTerrain;
import net.sf.colossus.variant.Hazards;
import net.sf.colossus.variant.MasterHex;
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
    private static final int HEX_SIZE = 15;
    private static final int EFFECT_SIZE = 20;
    private static final int CREATURE_SIZE = 30;
    private static final int STRIKE_SIZE = 15;

    private final static GridBagConstraints GBC_DEFAULT = new GridBagConstraints();
    static
    {
        GBC_DEFAULT.anchor = GridBagConstraints.NORTH;
        GBC_DEFAULT.insets = new Insets(2, 2, 5, 2);
        GBC_DEFAULT.weightx = 0.01;
    }
    private final static GridBagConstraints GBC_NORTHWEST = (GridBagConstraints)GBC_DEFAULT
        .clone();
    static
    {
        GBC_NORTHWEST.anchor = GridBagConstraints.NORTHWEST;
    }
    private final static GridBagConstraints GBC_NORTHEAST = (GridBagConstraints)GBC_DEFAULT
        .clone();
    static
    {
        GBC_NORTHEAST.anchor = GridBagConstraints.NORTHEAST;
    }

    private final MasterHex hex;
    private final Variant variant;
    private final SortedSet<CreatureType> creatures;
    private Map<String, HazardTerrain> hazardsDisplayed;
    private Map<String, HazardHexside> hexsidesDisplayed;

    public BattleTerrainHazardWindow(JFrame frame, ClientGUI gui, MasterHex hex)

    {
        super(frame, "Battle Terrain Hazards for "
            + hex.getTerrain().getDisplayName(), false);

        assert SwingUtilities.isEventDispatchThread() : "Constructor should be called only on the EDT";

        this.hex = hex;
        variant = gui.getGame().getVariant();
        creatures = variant.getCreatureTypes();
        getContentPane().setLayout(new GridBagLayout());
        useSaveWindow(gui.getOptions(), "BattleTerrainHazard", null);

        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                dispose();
            }
        });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setupHeader(getContentPane());
        setupChart(getContentPane());

        pack();
        setVisible(true);
    }

    private void setupHeader(Container container)
    {
        // to save extra indirections with subpanels some labels will be across two columns
        GridBagConstraints dblConstraints = (GridBagConstraints)GBC_DEFAULT
            .clone();
        dblConstraints.gridwidth = 2;

        // add headers
        container.add(new JPanel(), GBC_DEFAULT);
        container.add(new JLabel("Hex"), GBC_DEFAULT);
        container.add(new JLabel("Move"), dblConstraints);
        container.add(new JLabel("Natives"), GBC_DEFAULT);
        container.add(new JLabel("Strike"), dblConstraints);
        container.add(new JLabel("Defence"), dblConstraints);
        container.add(new JLabel("Special"), dblConstraints);

        // add an empty cell to finalize the row and eat extra space
        GridBagConstraints constraints = (GridBagConstraints)GBC_DEFAULT
            .clone();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1;
        container.add(new JPanel(), constraints);

    }

    private void setupChart(Container container)
    {
        hazardsDisplayed = new HashMap<String, HazardTerrain>();
        hexsidesDisplayed = new HashMap<String, HazardHexside>();
        for (HazardTerrain hazard : HazardTerrain.getAllHazardTerrains())
        {
            if (hazardsDisplayed.containsKey(hazard.getName())
                || hex.getTerrain().getHazardCount(hazard) == 0)
            {
                // Ignore
            }
            else
            {
                hazardsDisplayed.put(hazard.getName(), hazard);
                addHazard(container, hazard);
            }
        }
        for (HazardHexside hazard : HazardHexside.getAllHazardHexsides())
        {
            if ("nothing".equalsIgnoreCase(hazard.getName())
                || hexsidesDisplayed.containsKey(hazard.getName())
                || hex.getTerrain().getHazardHexsideCount(hazard) == 0)
            {
                // Ignore
            }
            else
            {
                hexsidesDisplayed.put(hazard.getName(), hazard);
                addHazard(container, hazard);
            }
        }

        // add an empty row that can grow with spare vSpace
        GridBagConstraints vFillConstraints = new GridBagConstraints();
        vFillConstraints.gridx = 0;
        vFillConstraints.gridwidth = GridBagConstraints.REMAINDER;
        vFillConstraints.weighty = 1;
        container.add(new JPanel(), vFillConstraints);

        // add a row for info text
        // TODO should be done better, this is just as first aid....
        GridBagConstraints vFillConstraints2 = new GridBagConstraints();
        vFillConstraints2.gridx = 0;
        vFillConstraints2.gridwidth = GridBagConstraints.REMAINDER;
        vFillConstraints2.anchor = GridBagConstraints.SOUTHWEST;
        vFillConstraints2.weighty = 1;
        JPanel textPanel = new JPanel();
        JLabel label = new JLabel(
            "Hold the mouse over a symbol for an explanatory popup text.");
        textPanel.add(label);
        container.add(textPanel, vFillConstraints);
    }

    private void addHazard(Container container, Hazards hazard)
    {
        // hex label is always first in row, aligned vCenter but left
        GridBagConstraints hexLabelConstraints = (GridBagConstraints)GBC_DEFAULT
            .clone();
        hexLabelConstraints.gridx = 0;
        hexLabelConstraints.anchor = GridBagConstraints.NORTHWEST;
        container.add(new JLabel(hazard.getName()), hexLabelConstraints);

        addHexImage(container, hazard);
        addMovementInfo(container, hazard);
        addNativesPanel(container, hazard);
        addStrikeInfo(container, hazard);
        addDefenderInfo(container, hazard);
        addSpecialInfo(container, hazard);

        // add an empty cell to finalize the row and eat extra space
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1;
        container.add(new JPanel(), constraints);

    }

    // Create GUI representation of Terrain
    private void addHexImage(Container container, Hazards hazard)
    {
        GUIBattleHex hex = new GUIBattleHex(HEX_SIZE, 0, HEX_SIZE, container,
            0, 0);
        BattleHex model = hex.getHexModel();
        if (hazard instanceof HazardTerrain)
        {
            model.setTerrain((HazardTerrain)hazard);
        }
        else
        {
            model.setTerrain(HazardTerrain.getDefaultTerrain());
            // to see the hexsides (or at least most of them) we have to configure
            // them on the neighbors
            // TODO top is broken, three are still missing
            // TODO for a full drawn one we would have to draw two hexes at least
            //      it might be easier (and better looking) to just draw the hexside
            GUIBattleHex neighborTop = new GUIBattleHex(HEX_SIZE, -4
                * HEX_SIZE, HEX_SIZE, container, 0, 0);
            configureHexModel((HazardHexside)hazard, neighborTop.getHexModel());
            hex.setNeighbor(0, neighborTop);
            GUIBattleHex neighborTopRight = new GUIBattleHex(4 * HEX_SIZE, -2
                * HEX_SIZE, HEX_SIZE, container, 0, 0);
            configureHexModel((HazardHexside)hazard,
                neighborTopRight.getHexModel());
            hex.setNeighbor(1, neighborTopRight);
            GUIBattleHex neighborBottomRight = new GUIBattleHex(4 * HEX_SIZE,
                2 * HEX_SIZE, HEX_SIZE, container, 0, 0);
            configureHexModel((HazardHexside)hazard,
                neighborBottomRight.getHexModel());
            hex.setNeighbor(2, neighborBottomRight);
        }
        hex.setHexModel(model);

        // we give this one some extra space around it
        GridBagConstraints constraints = (GridBagConstraints)GBC_DEFAULT
            .clone();
        constraints.insets = new Insets(5, 5, 5, 5);
        container.add(hex, constraints);
    }

    private void configureHexModel(HazardHexside hazard, BattleHex model)
    {
        model.setTerrain(HazardTerrain.getDefaultTerrain());
        for (int i = 0; i <= 5; i++)
        {
            model.setHexsideHazard(i, hazard);
        }
    }

    // Show Native critters;
    private void addNativesPanel(Container container, Hazards hazard)
    {
        JPanel nativePanel = new JPanel(new GridLayout(0, 6));
        for (CreatureType creature : creatures)
        {
            if (hazard instanceof HazardTerrain)
            {
                if (creature.isNativeIn((HazardTerrain)hazard))
                {
                    Chit chit = Chit.newCreatureChit(CREATURE_SIZE, creature);
                    chit.setToolTipText(creature.getName());
                    nativePanel.add(chit);
                }
            }
            else
            {
                if ((hazard.equals(HazardHexside.DUNE) && creature
                    .isNativeDune())
                    || (hazard.equals(HazardHexside.SLOPE) && creature
                        .isNativeSlope())
                    || (hazard.equals(HazardHexside.RIVER) && creature
                        .isNativeRiver()))
                {
                    Chit chit = Chit.newCreatureChit(CREATURE_SIZE, creature);
                    chit.setToolTipText(creature.getName());
                    nativePanel.add(chit);
                }
            }
        }
        container.add(nativePanel, GBC_DEFAULT);
    }

    // Effect on Movement
    private void addMovementInfo(Container container, Hazards hazard)
    {
        Chit flySymbol = null;
        if (hazard.effectOnFlyerMovement
            .equals(HazardConstants.EffectOnMovement.BLOCKALL))
        {
            flySymbol = Chit.newSymbolChit(EFFECT_SIZE, "FlyingBlocked");
            flySymbol.setToolTipText("FlyingBlocked");
        }
        else if (hazard.effectOnFlyerMovement
            .equals(HazardConstants.EffectOnMovement.BLOCKFOREIGNER))
        {
            flySymbol = Chit.newSymbolChit(EFFECT_SIZE, "FlyingNativeOnly");
            flySymbol.setToolTipText("Native Flyers Only");
        }
        else if (hazard.effectOnFlyerMovement
            .equals(HazardConstants.EffectOnMovement.SLOWALL))
        {
            flySymbol = Chit.newSymbolChit(EFFECT_SIZE, "FlyingSlow");
            flySymbol.setToolTipText("Slows Flying Creatures");
        }
        else if (hazard.effectOnFlyerMovement
            .equals(HazardConstants.EffectOnMovement.SLOWFOREIGNER))
        {
            flySymbol = Chit.newSymbolChit(EFFECT_SIZE, "FlyingNativeSlow");
            flySymbol.setToolTipText("Slows Non-Native Flying Creatures");
        }
        else
        {
            flySymbol = Chit.newSymbolChit(EFFECT_SIZE, "FlyingAll");
            flySymbol.setToolTipText("No effect on Flying Creatures");
        }
        container.add(flySymbol, GBC_NORTHEAST);

        Chit groundSymbol = null;
        if (hazard.effectOnGroundMovement
            .equals(HazardConstants.EffectOnMovement.BLOCKALL))
        {
            groundSymbol = Chit.newSymbolChit(EFFECT_SIZE, "GroundBlocked");
            groundSymbol.setToolTipText("Blocks Ground Movement");
        }
        else if (hazard.effectOnGroundMovement
            .equals(HazardConstants.EffectOnMovement.BLOCKFOREIGNER))
        {
            groundSymbol = Chit.newSymbolChit(EFFECT_SIZE, "GroundNativeOnly");
            groundSymbol.setToolTipText("Only Natives may Occupy");
        }
        else if (hazard.effectOnGroundMovement
            .equals(HazardConstants.EffectOnMovement.SLOWFOREIGNER))
        {
            groundSymbol = Chit.newSymbolChit(EFFECT_SIZE, "GroundNativeSlow");
            groundSymbol.setToolTipText("NonNatives Slowed");
        }
        else if (hazard.effectOnGroundMovement
            .equals(HazardConstants.EffectOnMovement.SLOWALL))
        {
            groundSymbol = Chit.newSymbolChit(EFFECT_SIZE, "GroundAllSlow");
            groundSymbol.setToolTipText("Slows Ground Movement");
        }
        else
        {
            groundSymbol = Chit.newSymbolChit(EFFECT_SIZE, "GroundAll");
            groundSymbol.setToolTipText("No Effect On Ground Movement");
        }
        container.add(groundSymbol, GBC_NORTHWEST);
    }

    private void addSpecialInfo(Container container, Hazards hazard)
    {
        Chit rangeStrikeSymbol;
        if (hazard.rangeStrikeSpecial
            .equals(HazardConstants.RangeStrikeSpecialEffect.RANGESTRIKEBLOCKED))
        {
            rangeStrikeSymbol = Chit.newSymbolChit(EFFECT_SIZE,
                "RangeStrikeBlocked");
            rangeStrikeSymbol
                .setToolTipText("Blocks normal Rangestrikes-Magic is not blocked");
        }
        else if (hazard.rangeStrikeSpecial
            .equals(HazardConstants.RangeStrikeSpecialEffect.RANGESTRIKEWALL))
        {
            rangeStrikeSymbol = Chit.newSymbolChit(EFFECT_SIZE,
                "RangeStrikeWall");
            rangeStrikeSymbol
                .setToolTipText("Blocks Rangestrikes unless Hazard is"
                    + "occupied by either the Rangestriker or the target.");
        }
        else if (hazard.rangeStrikeSpecial
            .equals(HazardConstants.RangeStrikeSpecialEffect.RANGESTRIKESKILLPENALTY))
        {
            rangeStrikeSymbol = Chit.newSymbolChit(EFFECT_SIZE,
                "RangeStrikeSkill");
            rangeStrikeSymbol
                .setToolTipText("Non Natives to this Hazard sill lose 1 Skill"
                    + "for each hazard of this type being crossed.");
        }
        else
        {
            rangeStrikeSymbol = Chit.newSymbolChit(EFFECT_SIZE,
                "RangeStrikeFree");
            rangeStrikeSymbol.setToolTipText("No effect on Rangestrikes.");
        }
        container.add(rangeStrikeSymbol);

        Chit special = null;
        if (hazard.terrainSpecial
            .equals(HazardConstants.SpecialEffect.HEALTHDRAIN))
        {
            special = Chit.newSymbolChit(EFFECT_SIZE, "HealthDrain");
            special
                .setToolTipText("Non natives suffer 1 damage per strike phase");
        }
        if (hazard.terrainSpecial
            .equals(HazardConstants.SpecialEffect.HEALTHDRAIN_WATERDWELLER))
        {
            special = Chit.newSymbolChit(EFFECT_SIZE, "HealthDrain");
            special.setToolTipText("Water Dweller lose 1 health per turn");
        }
        if (hazard.terrainSpecial
            .equals(HazardConstants.SpecialEffect.HEALTHGAIN))
        {
            special = new Chit(EFFECT_SIZE, "HealthGain");
            special
                .setToolTipText("Heals 1 damage per strike phase. Cures slow and poison");
        }
        if (hazard.terrainSpecial
            .equals(HazardConstants.SpecialEffect.PERMSLOW))
        {
            special = new Chit(EFFECT_SIZE, "PermanentSlow");
            special
                .setToolTipText("Creatures are slowed 1 per turn for rest of the battle");
        }
        if (special != null)
            container.add(special);
    }

    private void addDefenderInfo(Container container, Hazards hazard)
    {
        container.add(makeStrikeEffect("Defending", hazard.defenseEffect),
            GBC_NORTHEAST);
        container.add(
            makeStrikeEffect("Being Rangestruck", hazard.rangedDefenseEffect),
            GBC_NORTHWEST);
    }

    private void addStrikeInfo(Container container, Hazards hazard)
    {
        container.add(makeStrikeEffect("Attacking", hazard.attackEffect),
            GBC_NORTHEAST);
        container.add(
            makeStrikeEffect("Rangestriking", hazard.rangedAttackEffect),
            GBC_NORTHWEST);
    }

    private Chit makeStrikeEffect(String strike, Hazards.CombatEffect e)
    {
        String[] overlay;
        if ("Being Rangestruck".equals(strike)
            || "Rangestriking".equals(strike))
        {
            overlay = new String[1];
            overlay[0] = "RangestrikeBase";
        }
        else
        {
            overlay = null;
        }

        Chit strikeSymbol;
        if (e.effect.equals(HazardConstants.EffectOnStrike.BLOCKED))
        {
            strikeSymbol = new Chit(STRIKE_SIZE, "StrikeBlocked", overlay);
            strikeSymbol.setToolTipText(strike
                + " Across Hazard is not Possible");
        }
        else if (e.effect.equals(HazardConstants.EffectOnStrike.SKILLBONUS)
            || e.effect.equals(HazardConstants.EffectOnStrike.SKILLPENALTY))
        {
            if (e.effect.equals(HazardConstants.EffectOnStrike.SKILLPENALTY))
            {
                strikeSymbol = new StrikeDie(STRIKE_SIZE, e.adjustment,
                    "Miss", overlay);
            }
            else
            {
                strikeSymbol = new StrikeDie(STRIKE_SIZE, e.adjustment, "Hit",
                    overlay);
            }
            StringBuilder tip = new StringBuilder();
            if (e.scope
                .equals(HazardConstants.ScopeOfEffectOnStrike.FOREIGNERS)
                || e.scope
                    .equals(HazardConstants.ScopeOfEffectOnStrike.IMPERIALS))
            {
                tip.append("Non-Natives ");
            }
            else if (e.scope
                .equals(HazardConstants.ScopeOfEffectOnStrike.NATIVES)
                || e.scope
                    .equals(HazardConstants.ScopeOfEffectOnStrike.PATRIOTS))
            {
                tip.append("Natives ");
            }
            else
            {
                tip.append("Everyone ");
            }
            tip.append("have skill ");
            if (e.effect.equals(HazardConstants.EffectOnStrike.SKILLPENALTY))
            {
                tip.append("decreased by ");
            }
            else
            {
                tip.append("increased by ");
            }
            tip.append(e.adjustment);
            tip.append(" when " + strike);
            if (e.scope
                .equals(HazardConstants.ScopeOfEffectOnStrike.IMPERIALS))
            {
                tip.append("Natives");
            }
            else if (e.scope
                .equals(HazardConstants.ScopeOfEffectOnStrike.PATRIOTS))
            {
                tip.append("Non-Natives");
            }
            strikeSymbol.setToolTipText(tip.toString());
        }
        else if (e.effect.equals(HazardConstants.EffectOnStrike.POWERBONUS)
            || e.effect.equals(HazardConstants.EffectOnStrike.POWERPENALTY))
        {
            strikeSymbol = new StrikeDie(STRIKE_SIZE, 1, "RedBlue", overlay);
            StringBuilder tip = new StringBuilder();
            if (e.scope
                .equals(HazardConstants.ScopeOfEffectOnStrike.FOREIGNERS)
                || e.scope
                    .equals(HazardConstants.ScopeOfEffectOnStrike.IMPERIALS))
            {
                tip.append("Non-Natives ");
            }
            else if (e.scope
                .equals(HazardConstants.ScopeOfEffectOnStrike.NATIVES)
                || e.scope
                    .equals(HazardConstants.ScopeOfEffectOnStrike.PATRIOTS))
            {
                tip.append("Natives ");
            }
            else
            {
                tip.append("Everyone ");
            }
            if (e.effect.equals(HazardConstants.EffectOnStrike.POWERPENALTY))
            {
                tip.append("loses ");
            }
            else
            {
                tip.append("gains ");
            }
            tip.append(e.adjustment);
            tip.append(" dice when " + strike);
            if (e.scope
                .equals(HazardConstants.ScopeOfEffectOnStrike.IMPERIALS))
            {
                tip.append("Natives");
            }
            else if (e.scope
                .equals(HazardConstants.ScopeOfEffectOnStrike.PATRIOTS))
            {
                tip.append("Non-Natives");
            }
            strikeSymbol.setToolTipText(tip.toString());
        }
        else
        {
            strikeSymbol = new StrikeDie(STRIKE_SIZE, 0, "RedBlue", overlay);
            strikeSymbol.setToolTipText("Normal Strike");
        }
        return strikeSymbol;
    }
}
