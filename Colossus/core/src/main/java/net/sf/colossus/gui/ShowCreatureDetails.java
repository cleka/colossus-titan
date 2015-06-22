/*
 * Created on 03.01.2004
 */
package net.sf.colossus.gui;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import net.sf.colossus.game.Creature;
import net.sf.colossus.game.RecruitGraph;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.server.BattleStrikeServerSide;
import net.sf.colossus.server.LegionServerSide;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardHexside;
import net.sf.colossus.variant.HazardTerrain;
import net.sf.colossus.variant.Hazards;
import net.sf.colossus.variant.IVariant;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.Variant;


/**
 * A dialog frame that displays lots of (almost static) information
 * about one specific creature type. i.e the power/skill, the musterings
 * and the abilities in the different hazards.
 *
 * Non-static information that might be shown is the number of creatures
 * left in the caretaker's stack.
 *
 * The dialog is thought to be popped up and closed if needed, displaying
 * information about one selected creature type. The info in the dialog is
 * updated if needed.
 *
 * Implementation details:
 *   The dialog mainly contains one JTextEdit control which itself
 *   display HTML text! It is thinkable, that the HTML text comes from
 *   an URL (easy to implement). Currently the whole HTML is built
 *   line by line like a servlet would do it.
 *
 *   To figure out some of the more difficult properties of the creature
 *   I "simulate" a tiny battlefield, where the creature in question
 *   engages other creatures in different hazard terrains. This is
 *   of course highly dependent on the battle implementation. It can easily
 *   break. I tried to be very generic -- things that might break, should
 *   break on compilation time, and not display wrong information.
 *
 *
 * TODO this dialog should have a SaveWindow attached to it.
 *
 * TODO hexside Hazards
 * Clemens: I started adding the hexside hazards, but that is not completed;
 * for one, the simulatedXXX setup cannot easily be extended calculate that
 * right, and there it is dependent on "atop XXX" or "below XXX" .
 * So, I leave the extended table creation there, but do not add the hexside
 * hazards into the hazards Collection so that it just shows same as before.
 * There is a lot of things that need improvement, see
 * 2136671      Show creature detail window...
 *
 * @author Towi, copied from ShowRecruitTree
 */
public final class ShowCreatureDetails extends KDialog
{

    // Client acting as placeholder for Variant
    // TODO ivariant can be removed when Variant itself
    //      is able to provide that information

    private final IVariant ivariant;

    private final Collection<Hazards> hazards;

    private final BattleStrikeServerSide battleStrikeSS;

    /** pops up the non-modal dialog. info can be updated if needed.
     * @param parentFrame parent frame, i.e. the master board
     * @param creature creature to show detailed info for.
     * @param point coordinate on screen to display windows, or null.
     * @param pane if 'point' is not null it is relative to this.
     * @param variant the current Variant
     * @param clientGui for now, the Client acting as deputy to answer Variant
     * questions variant cannot answer yet, and we get iVariant from clientGui
     */
    public ShowCreatureDetails(final JFrame parentFrame,
        final CreatureType creature, final Point point,
        final JScrollPane pane, Variant variant, ClientGUI clientGui)
    {
        super(parentFrame, "Creature Info: " + creature.getName(), false);

        this.ivariant = clientGui.getClient();
        this.battleStrikeSS = new BattleStrikeServerSide(clientGui.getClient()
            .getGame());

        Collection<HazardTerrain> terrainHazards = HazardTerrain
            .getAllHazardTerrains();
        // Collection<HazardHexside> hexsideHazards = HazardHexside
        //    .getAllHazardHexsides();

        this.hazards = new ArrayList<Hazards>();
        hazards.addAll(terrainHazards);

        // Not fully implemented
        // hazards.addAll(hexsideHazards);

        setBackground(Color.lightGray);
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                dispose();
            }

        });
        Container cnt = getContentPane();

        showCreatureDetails(cnt, creature, variant);

        pack();
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                dispose();
            }

        });
        if (point != null)
        {
            placeRelative(parentFrame, point, pane);
        }
        setVisible(true);
        repaint();
    }

    /**
     * @param cnt the awt container where the info wil be shown in.
     *   it will be emptied.
     * @param creature the creature that details you want to show
     */
    public void showCreatureDetails(final Container cnt,
        final CreatureType creature, Variant variant)
    {
        // clear all the elements
        cnt.removeAll();
        cnt.setLayout(new BorderLayout());
        // prepare main pane
        JEditorPane pane = new JEditorPane();
        pane.setEditable(false);
        // define the content
        StringBuilder s = new StringBuilder();
        _head(s, creature);
        //
        // general
        //
        _section(s, "General Characteristics");
        _trSpan(s, "Name",
            creature.getName() + " <i>(" + creature.getPluralName() + ")</i>");
        _trSpan(s, "Power..Skill",
            creature.getPower() + ".." + creature.getSkill());
        _trSpan(s, "Total count", "" + creature.getMaxCount());
        _trSpan(s, "Rangestrike", (creature.isRangestriker() ? "yes" : "no")
            + (creature.useMagicMissile() ? " <b>(magic missiles)</b>" : ""));
        _trSpan(s, "Flier", creature.isFlier() ? "yes" : "no");
        if (creature.isPoison())
            _trSpan(s, "Poison", "yes: " + creature.getPoison());
        else
            _trSpan(s, "Poison", "no");
        _trSpan(s, "Summonable", creature.isSummonable() ? "yes" : "no");
        // TODO Instead show full list of "where and for each multiple of X
        _trSpan(s, "Acquirable", variant.isAcquirable(creature) ? "yes" : "no");
        _trSpan(
            s,
            _low("Lord"),
            creature.isLordOrDemiLord() ? (creature.isLord() ? "<u><b>Lord</b></u>"
                : "<b>Demi-Lord</b>")
                : _low("no"));
        StringBuilder buf = new StringBuilder();
        String sp = "&nbsp;";
        String separator = "";
        for (HazardTerrain terrain : HazardTerrain.getAllHazardTerrains())
        {
            if (creature.isNativeIn(terrain))
            {
                buf.append(separator);
                separator = ", ";
                buf.append(terrain.getName());
            }
        }
        for (int idx = 0; idx < HEXSIDES.length; idx++)
        {
            if (creature.isNativeAt(HEXSIDES[idx]))
            {
                buf.append(separator);
                separator = ", ";
                buf.append(HEXSIDE_NAMES[idx]);
            }
        }
        _trSpan(s, "Native Hazards", buf.toString());

        //
        // recruit
        //
        _section(s, "Recruit");
        //   in
        for (MasterBoardTerrain terrain : variant.getTerrains())
        {
            buf = new StringBuilder();
            // TODO use variant instead?
            List<CreatureType> recruiters = VariantSupport.getCurrentVariant()
                .getCreatureTypesAsList();
            separator = "";
            for (int ri = 0; ri < recruiters.size(); ri++)
            {
                final CreatureType recruiter = recruiters.get(ri);
                int num = ivariant.numberOfRecruiterNeeded(recruiter,
                    creature, terrain, null);
                if (num == 1 && creature.getMaxCount() == 1
                    && recruiter.getName().equals(creature.getName()))
                {
                    // skip self-recruiting if there is only one of them
                    // TODO skip already during load
                }
                else if (num == 1)
                {
                    buf.append(separator + "by" + sp + "1" + sp
                        + recruiter.getName());
                    separator = ", ";
                }
                // TODO skip the > getMaxCount() already during Variant load
                else if ((num > 0) && num < recruiter.getMaxCount()
                    && num < RecruitGraph.BIGNUM
                    && !recruiter.getName().equals("Titan"))
                {
                    buf.append(separator + "by" + sp + num + sp
                        + recruiter.getPluralName());
                    separator = ", ";
                }
            }
            if (buf.length() > 0)
            {
                Color color = terrain.getColor().brighter();
                s.append(MessageFormat.format(
                    "<tr><td bgcolor={0}>in {1}</td>"
                        + "<td colspan={2}><font color=blue>{3}</font></td>"
                        + "</tr>", new Object[] {
                        HTMLColor.colorToCode(color), terrain.getId(),
                        "" + (hazards.size() + 1), buf.toString(), }));
            }
        }
        //   out
        for (MasterBoardTerrain terrain : variant.getTerrains())
        {
            buf = new StringBuilder();
            // TODO use variant instead?
            List<CreatureType> recruits = VariantSupport.getCurrentVariant()
                .getCreatureTypesAsList();
            separator = "";
            for (int ri = 0; ri < recruits.size(); ri++)
            {
                final CreatureType recruit = recruits.get(ri);
                int num = ivariant.numberOfRecruiterNeeded(creature, recruit,
                    terrain, null);
                if (num == 1 && creature.getMaxCount() == 1
                    && recruit.getName().equals(creature.getName()))
                {
                    // skip self-recruiting if there is only one of them
                    // TODO skip already during load
                }
                // TODO skip the > getMaxCount() already during Variant load
                else if (num > 0 && num < creature.getMaxCount()
                    && num < RecruitGraph.BIGNUM)
                {
                    buf.append(separator);
                    separator = ", ";
                    buf.append(num + sp + "recruit" + sp + "a" + sp
                        + recruit.getName());
                }
            }
            if (buf.length() > 0)
            {
                Color color = terrain.getColor().brighter();
                s.append(MessageFormat.format(
                    "<tr><td bgcolor={0}>in {1}</td>"
                        + "<td colspan={2}><font color=green>{3}</font></td>"
                        + "</tr>", new Object[] {
                        HTMLColor.colorToCode(color), terrain.getId(),
                        "" + (hazards.size() + 1), buf.toString(), }));
            }
        }

        //
        // Battle
        //
        _section(s, "Battle");
        //   subtable title
        String explanation = "For a target in Plains (with same skill as "
            + "attacking " + creature.getName() + "), the <br> table below "
            + "shows nr of dice to roll and which rolled numbers are hits.";
        s.append(MessageFormat.format(
            "<tr><td bgcolor=#dddddd colspan={0}>{1}</td></tr>", new Object[] {
                "" + (hazards.size() + 2), explanation, }));
        SimulatedCritter critter = new SimulatedCritter(creature,
            HazardTerrain.getDefaultTerrain());
        SimulatedCritter other = new SimulatedCritter(creature,
            HazardTerrain.getDefaultTerrain());

        // =============================================================
        // hazards row 1
        s.append("<tr><td ROWSPAN=2 align=right>" + creature.getName()
            + " in</td><td></td>");
        for (Iterator<Hazards> iterator = hazards.iterator(); iterator
            .hasNext();)
        {
            iterator.next(); // skip one
            if (!iterator.hasNext())
            {
                break;
            }
            Hazards hazard = iterator.next();
            Color color = Color.lightGray;
            String hazardName = hazard.getName();
            if (hazard instanceof HazardTerrain)
            {
                critter.setNewHazardHex((HazardTerrain)hazard);
                color = critter.getHazardColor().brighter();
            }
            else
            {
                other.setHexsideHazard((HazardHexside)hazard);
                color = other.getHexsideColor().brighter();
                // TODO fix Tower/Wall in data files
                if (hazard == HazardHexside.TOWER)
                {
                    hazardName = "Wall";
                }
            }
            String colspan = "2";
            s.append(MessageFormat.format(
                "<td bgcolor={0} colspan={2}>{1}</td>", new Object[] {
                    HTMLColor.colorToCode(color), hazardName, colspan }));
        }
        s.append("</tr>");

        // =============================================================
        // hazards row 2
        s.append("<tr>");
        for (Iterator<Hazards> iterator = hazards.iterator(); iterator
            .hasNext();)
        {
            Hazards hazard = iterator.next();
            Color color = Color.lightGray;
            String hazardName = hazard.getName();
            if (hazard instanceof HazardTerrain)
            {
                critter.setNewHazardHex((HazardTerrain)hazard);
                color = critter.getHazardColor().brighter();
            }
            else
            {
                other.setHexsideHazard((HazardHexside)hazard);
                color = other.getHexsideColor().brighter();
                // TODO fix Tower/Wall in data files
                if (hazard == HazardHexside.TOWER)
                {
                    hazardName = "Wall";
                }
            }
            String colspan = "2";
            s.append(MessageFormat.format(
                "<td bgcolor={0} colspan={2}>{1}</td>", new Object[] {
                    HTMLColor.colorToCode(color), hazardName, colspan, }));
            if (iterator.hasNext())
            {
                iterator.next(); // skip one
            }
        }
        s.append("</tr>");

        // =============================================================
        //   the info: the table content
        //   ... how many dice to roll
        s.append("<tr><th nowrap>Dice count</th>");
        for (Hazards hazard : hazards)
        {
            Color color;
            String text;
            if (hazard instanceof HazardTerrain)
            {
                HazardTerrain terrain = (HazardTerrain)hazard;
                critter.setNewHazardHex(terrain);
                color = critter.getHazardColor().brighter();
                text = "" + critter.getSimulatedDiceCount(other);
            }
            else
            {
                other.setHexsideHazard((HazardHexside)hazard);
                color = other.getHexsideColor().brighter();
                text = "?";
            }
            s.append(MessageFormat.format("<td bgcolor={0}>{1}</td>",
                new Object[] { HTMLColor.colorToCode(color), text, }));
        }
        s.append("<td bgcolor=#dddddd></td></tr>");

        // =============================================================
        //   ... hit treshold
        s.append("<tr><th nowrap>Hit if >=</th>");
        for (Hazards hazard : hazards)
        {
            Color color;
            String text;
            if (hazard instanceof HazardTerrain)
            {
                HazardTerrain terrain = (HazardTerrain)hazard;
                critter.setNewHazardHex(terrain);
                color = critter.getHazardColor().brighter();
                text = "" + critter.getSimulatedStrikeNr(other);
            }
            else
            {
                other.setHexsideHazard((HazardHexside)hazard);
                color = other.getHexsideColor().brighter();
                text = "?";
            }
            s.append(MessageFormat.format("<td bgcolor={0}>{1}</td>",
                new Object[] { HTMLColor.colorToCode(color), text, }));
        }
        s.append("<td bgcolor=#dddddd></td></tr>");

        // XCV ...work in progress...
        // towi ...work in progress...
        //
        //   add stuff here if you like
        //

        // close
        s.append("</table>");
        s.append("</body></html>");

        // put in the content
        pane.setContentType("text/html");
        pane.setText(s.toString());
        // layout everything
        JScrollPane scrollPane = new JScrollPane(pane);
        scrollPane
            .setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(500, 800));
        scrollPane.setMinimumSize(new Dimension(200, 400));
        cnt.add(scrollPane, BorderLayout.CENTER);
    }

    //
    // helpers
    //

    /** easy access to hex side identifiers. */
    private static final char[] HEXSIDES = { ' ', 'd', 'c', 's', 'w', 'r' };

    /** define hex side names for table column headers. */
    private static final String[] HEXSIDE_NAMES = { "nothing", "dune",
        "cliff", "slope", "wall", "river" };

    /** html header and start of page. */
    private static void _head(StringBuilder s, final CreatureType cr)
    {
        s.append("<html><head></head><body bgcolor="
            + HTMLColor.colorToCode(Color.LIGHT_GRAY) + ">");
        s.append("<h2>Creature Details: <b>" + cr.getName() + "</b></h2>");
        s.append("<table width=100%>");
    }

    /** start of a named section.
     * @param s in/out
     */
    private void _section(StringBuilder s, final String name)
    {
        s.append("<tr bgcolor=gray><td colspan=" + (hazards.size() + 1) + ">");
        s.append("<b>" + name + "</b>");
        s.append("</td></tr>");
    }

    /** a headered table row, the data column spans.
     * @param s in/out
     */
    private void _trSpan(StringBuilder s, final String name, final String value)
    {
        s.append(MessageFormat.format(
            "<tr><th>{0}</th><td colspan={1}>{2}</td></tr>", new Object[] {
                name, // 0
                "" + (hazards.size() + 1), // 1
                value, // 2
            }));
    }

    /** wrap HTML code around s to make it dark, or gray. */
    private static String _low(final String s)
    {
        return "<font color=gray>" + s + "</font>";
    }

    //
    // simulate a battle
    //
    /** helper class that catches some calls for the simulated critter. */
    final class SimulatedBattleHex extends BattleHex
    {
        SimulatedBattleHex(final HazardTerrain hazard)
        {
            super(4, 4); // 4,4: something in the middle
            setTerrain(hazard);
        }

        /** fake, return ' ' nor now. TODO. */
        @Override
        public char getOppositeHexside(final int i)
        {
            return ' '; // plain hex side for now
        }
    }

    /** helper class to simulate a battle of the creature in question against
     * an other creature. especially distance and hazard must be simulated.
     * very fragile class, i suppose. but it might be worth it.
     *
     * TODO this gets harder and harder to maintain the more typesafe the model gets.
     * Figure out what it is really good for and solve the actual problem. Currently
     * it even causes assertion errors since it passes nulls where nulls aren't allowed.
     *
     * @author Towi
     */
    final class SimulatedCritter extends Creature
    {

        /** catch calls to "underlying" battle hex and proxy it to this. */
        private SimulatedBattleHex hex;

        /** @param creature to create a critter for
         * @param hazard that stands in this hazard */
        SimulatedCritter(final CreatureType creature,
            final HazardTerrain hazard)
        {
            super(creature, new LegionServerSide("dummy", null, null, null,
                null, null));
            setNewHazardHex(hazard);
        }

        /** in hazard Plains. */
        SimulatedCritter(final CreatureType creature)
        {
            this(creature, HazardTerrain.getDefaultTerrain());
        }

        /** create the simulated hex. */
        public void setNewHazardHex(final HazardTerrain hazard)
        {
            hex = new SimulatedBattleHex(hazard);
        }

        // typically called on *OTHER*
        public void setHexsideHazard(final HazardHexside hexside)
        {
            hex.setHexsideHazard(0, hexside);
        }

        /** power of this creature hitting target. */
        public int getSimulatedDiceCount(final Creature target)
        {
            return battleStrikeSS.getDice(this, target, false);
        }

        /** skill of this creature hitting target. */
        public int getSimulatedStrikeNr(final Creature target)
        {
            return battleStrikeSS.getStrikeNumber(this, target, false);
        }

        /** color of hex i stand on. */
        public Color getHazardColor()
        {
            return hex.getTerrainColor();
        }

        public Color getHexsideColor()
        {
            Color color = Color.gray;

            // See BattleHext.getTerrainColor()

            HazardHexside hexsideHazard = hex.getHexsideHazard(0);
            if (hexsideHazard == HazardHexside.RIVER)
            {
                // River is water like a lake
                color = HTMLColor.skyBlue;
            }
            else if (hexsideHazard == HazardHexside.DUNE)
            {
                // Dunes are in the desert
                color = Color.orange;
            }
            else if (hexsideHazard == HazardHexside.SLOPE)
            {
                // Slope are in the hills
                color = HTMLColor.brown;
            }
            else if (hexsideHazard == HazardHexside.TOWER)
            {
                // Slope are in the hills
                color = HTMLColor.dimGray;
            }
            else if (hexsideHazard == HazardHexside.CLIFF)
            {
                // Slope are in the hills
                color = HTMLColor.darkRed;
            }

            return color;
        }

        //
        // to help simulating
        //
        /** prox to simulated hex. */
        @Override
        public BattleHex getCurrentHex()
        {
            return hex;
        }
    }
}
