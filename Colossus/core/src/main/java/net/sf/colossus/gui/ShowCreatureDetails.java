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
import java.util.Iterator;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import net.sf.colossus.game.RecruitGraph;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.server.CreatureServerSide;
import net.sf.colossus.server.LegionServerSide;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardTerrain;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


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
 * @author Towi, copied from ShowRecruitTree
 * @version $Id$
 */
public final class ShowCreatureDetails extends KDialog
{

    /** pops up the non-modal dialog. info can be updated if needed.
     * @param parentFrame parent frame, i.e. the master board
     * @param creature creature to show detailed info for.
     * @param point coordinate on screen to display windows, or null.
     * @param pane if 'point' is not null it is relative to this.
     */
    public ShowCreatureDetails(final JFrame parentFrame,
        final CreatureType creature, final Point point, final JScrollPane pane)
    {
        super(parentFrame, "Creature Info: " + creature.getName(), false);

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

        showCreatureDetails(cnt, creature);

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
        final CreatureType creature)
    {
        // claear all the elements
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
        _trSpan(s, "Name", creature.getName() + " <i>("
            + creature.getPluralName() + ")</i>");
        _trSpan(s, "Power..Skill", creature.getPower() + ".."
            + creature.getSkill());
        _trSpan(s, "Total count", "" + creature.getMaxCount());
        _trSpan(s, "Rangestrike", (creature.isRangestriker() ? "yes" : "no")
            + (creature.useMagicMissile() ? " <b>(magic missiles)</b>" : ""));
        _trSpan(s, "Flier", creature.isFlier() ? "yes" : "no");
        _trSpan(s, "Summonable", creature.isSummonable() ? "yes" : "no");
        // TODO Instead show full list of "where and for each multiple of X
        _trSpan(s, "Acquirable",
            TerrainRecruitLoader.isAcquirable(creature) ? "yes" : "no");
        _trSpan(s, _low("Lord"), creature.isLordOrDemiLord() ? (creature
            .isLord() ? "<u><b>Lord</b></u>" : "<b>Demi-Lord</b>")
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
            if (creature.isNativeHexside(HEXSIDES[idx]))
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
        for (MasterBoardTerrain terrain : TerrainRecruitLoader.getTerrains())
        {
            buf = new StringBuilder();
            List<CreatureType> recruiters = VariantSupport.getCurrentVariant()
                .getCreatureTypes();
            separator = "";
            for (int ri = 0; ri < recruiters.size(); ri++)
            {
                final CreatureType recruiter = recruiters.get(ri);
                int num = TerrainRecruitLoader.numberOfRecruiterNeeded(
                    recruiter, creature, terrain, null);
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
                s
                    .append(MessageFormat
                        .format(
                            "<tr><td bgcolor={0}>in {1}</td>"
                                + "<td colspan={2}><font color=blue>{3}</font></td>"
                                + "</tr>", new Object[] {
                                HTMLColor.colorToCode(color),
                                terrain.getId(),
                                ""
                                    + (HazardTerrain.getAllHazardTerrains()
                                        .size() + 1), buf.toString(), }));
            }
        }
        //   out
        for (MasterBoardTerrain terrain : TerrainRecruitLoader.getTerrains())
        {
            buf = new StringBuilder();
            List<CreatureType> recruits = VariantSupport.getCurrentVariant()
                .getCreatureTypes();
            separator = "";
            for (int ri = 0; ri < recruits.size(); ri++)
            {
                final CreatureType recruit = recruits.get(ri);
                int num = TerrainRecruitLoader.numberOfRecruiterNeeded(
                    creature, recruit, terrain, null);
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
                s
                    .append(MessageFormat
                        .format(
                            "<tr><td bgcolor={0}>in {1}</td>"
                                + "<td colspan={2}><font color=green>{3}</font></td>"
                                + "</tr>", new Object[] {
                                HTMLColor.colorToCode(color),
                                terrain.getId(),
                                ""
                                    + (HazardTerrain.getAllHazardTerrains()
                                        .size() + 1), buf.toString(), }));
            }
        }

        //
        // Battle
        //
        _section(s, "Battle");
        //   subtable title
        s.append(MessageFormat.format(
            "<tr><td bgcolor=#dddddd colspan={0}>{1}</td></tr>", new Object[] {
                "" + (HazardTerrain.getAllHazardTerrains().size() + 2),
                "Target in Plains", }));
        SimulatedCritter critter = new SimulatedCritter(creature,
            HazardTerrain.getDefaultTerrain());
        SimulatedCritter other = new SimulatedCritter(creature, HazardTerrain
            .getDefaultTerrain());
        //   hazards row 1
        s.append("<tr><td ROWSPAN=2 align=right>" + creature.getName()
            + " in</td><td></td>");
        for (Iterator<HazardTerrain> iterator = HazardTerrain
            .getAllHazardTerrains().iterator(); iterator.hasNext();)
        {
            iterator.next(); // skip one
            if (!iterator.hasNext())
            {
                break;
            }
            HazardTerrain terrain = iterator.next();
            critter.setNewHazardHex(terrain);
            Color color = critter.getHazardColor().brighter();
            String colspan = "2";
            s
                .append(MessageFormat.format(
                    "<td bgcolor={0} colspan={2}>{1}</td>", new Object[] {
                        HTMLColor.colorToCode(color), terrain.getName(),
                        colspan }));
        }
        s.append("</tr>");
        //   hazards row 2
        s.append("<tr>");
        for (Iterator<HazardTerrain> iterator = HazardTerrain
            .getAllHazardTerrains().iterator(); iterator.hasNext();)
        {
            HazardTerrain terrain = iterator.next();
            critter.setNewHazardHex(terrain);
            Color color = critter.getHazardColor().brighter();
            String colspan = "2";
            s.append(MessageFormat.format(
                "<td bgcolor={0} colspan={2}>{1}</td>",
                new Object[] { HTMLColor.colorToCode(color),
                    terrain.getName(), colspan, }));
            if (iterator.hasNext())
            {
                iterator.next(); // skip one
            }
        }
        s.append("</tr>");
        //   the info: the table content
        //   ... my strike power
        s.append("<tr><th nowrap>my Strike Power</th>");
        for (HazardTerrain terrain : HazardTerrain.getAllHazardTerrains())
        {
            critter.setNewHazardHex(terrain);
            Color color = critter.getHazardColor().brighter();
            s.append(MessageFormat.format("<td bgcolor={0}>{1}</td>",
                new Object[] { HTMLColor.colorToCode(color),
                    "" + critter.getSimulatedPower(other), }));
        }
        s.append("<td bgcolor=#dddddd></td></tr>");
        //   ... my strike skill
        s.append("<tr><th nowrap>my Strike Skill</th>");
        for (HazardTerrain terrain : HazardTerrain.getAllHazardTerrains())
        {
            critter.setNewHazardHex(terrain);
            Color color = critter.getHazardColor().brighter();
            s.append(MessageFormat.format("<td bgcolor={0}>{1}</td>",
                new Object[] { HTMLColor.colorToCode(color),
                    "" + critter.getSimulatedSkill(other), }));
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
    private static void _section(StringBuilder s, final String name)
    {
        s.append("<tr bgcolor=gray><td colspan="
            + (HazardTerrain.getAllHazardTerrains().size() + 1) + ">");
        s.append("<b>" + name + "</b>");
        s.append("</td></tr>");
    }

    /** a headered table row, the data column spans.
     * @param s in/out
     */
    private static void _trSpan(StringBuilder s, final String name,
        final String value)
    {
        s.append(MessageFormat.format(
            "<tr><th>{0}</th><td colspan={1}>{2}</td></tr>", new Object[] {
                name, // 0
                "" + (HazardTerrain.getAllHazardTerrains().size() + 1), // 1
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
     * TODO this is the only reference to {@link CreatureServerSide} left in the client
     * code and it probably doesn't need the specific model. If this should stay, it
     * should probably changed to use {@link net.sf.colossus.game.Creature} instead.
     *
     * @author Towi
     */
    final class SimulatedCritter extends CreatureServerSide
    {

        /** catch calls to "underlying" battle hex and proxy it to this. */
        private SimulatedBattleHex hex;

        /** @param creature to create a critter for
         * @param hazard that stands in this hazard */
        SimulatedCritter(final CreatureType creature,
            final HazardTerrain hazard)
        {
            super(creature, new LegionServerSide("dummy", null, null, null,
                null, null), null);
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

        /** power of this creature hitting target. */
        public int getSimulatedPower(final CreatureServerSide target)
        {
            return getDice(target);
        }

        /** skill of this creature hitting target. */
        public int getSimulatedSkill(final CreatureServerSide target)
        {
            return getStrikeNumber(target);
        }

        /** color of hex i stand on. */
        public Color getHazardColor()
        {
            return hex.getTerrainColor();
        }

        //
        // to help simulating
        //
        /** prox to simulated hex. */
        @Override
        protected BattleHex getCurrentHex()
        {
            return hex;
        }

        /** fake -- true or false as wished. TODO */
        @Override
        protected boolean isInContact(final boolean countDead)
        {
            return true;
        }

        /** not needed here -- fake returns 0. */
        /** @deprecated another function with explicit reference to Bramble
         * that should be fixed.
         */
        @Deprecated
        @Override
        protected int countBrambleHexes(final BattleHex targetHex)
        {
            return 0;
        }
    }
}
