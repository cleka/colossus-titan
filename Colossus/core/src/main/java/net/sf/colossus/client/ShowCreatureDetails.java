/*
 * Created on 03.01.2004
 */
package net.sf.colossus.client;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Critter;
import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.RecruitGraph;
import net.sf.colossus.variant.HazardTerrain;
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
public final class ShowCreatureDetails extends KDialog implements
    MouseListener, WindowListener
{

    /** pops up the non-modal dialog. info can be updated if needed.
     * @param parentFrame parent frame, i.e. the master board
     * @param creature creature to show detailed info for.
     * @param point coordinate on screen to display windows, or null.
     * @param pane if 'point' is not null it is relative to this.
     */
    public ShowCreatureDetails(final JFrame parentFrame,
        final Creature creature, final Point point, final JScrollPane pane)
    {
        super(parentFrame, "Creature Info: " + creature.getName(), false);

        setBackground(Color.lightGray);
        addWindowListener(this);
        Container cnt = getContentPane();

        showCreatureDetails(cnt, creature);

        pack();
        addMouseListener(this);
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
        final Creature creature)
    {
        // claear all the elements
        cnt.removeAll();
        cnt.setLayout(new BorderLayout());
        // prepare main pane
        JEditorPane pane = new JEditorPane();
        pane.setEditable(false);
        // define the content
        StringBuffer s = new StringBuffer();
        _head(s, creature);
        //
        // general
        //
        _section(s, "General Characteristics");
        _trSpan(s, "Name", creature.getName() + " <i>("
            + creature.getPluralName() + ")</i>");
        _trSpan(s, "Power..Skill", creature.getPower() + ".."
            + creature.getSkill());
        _trSpan(s, "Rangestrike", (creature.isRangestriker() ? "yes" : "no")
            + (creature.useMagicMissile() ? " <b>(magic missiles)</b>" : ""));
        _trSpan(s, "Flier", creature.isFlier() ? "yes" : "no");
        _trSpan(s, _low("Lord"), creature.isLordOrDemiLord() ? (creature
            .isLord() ? "<u><b>Lord</b></u>" : "<b>Demi-Lord</b>")
            : _low("no"));
        StringBuffer buf = new StringBuffer();
        for (Iterator iterator = HazardTerrain.getAllHazardTerrains()
            .iterator(); iterator.hasNext();)
        {
            HazardTerrain terrain = (HazardTerrain)iterator.next();
            if (creature.isNativeTerrain(terrain))
            {
                buf.append(terrain.getName());
                buf.append(", ");
            }
        }
        for (int idx = 0; idx < HEXSIDES.length; idx++)
        {
            if (creature.isNativeHexside(HEXSIDES[idx]))
            {
                buf.append(HEXSIDE_NAMES[idx]);
                buf.append(", ");
            }
        }
        _trSpan(s, "Native Hazards", buf.toString());

        //
        // recruit
        //
        _section(s, "Recruit");
        final String[] terrains = TerrainRecruitLoader.getTerrains();
        //   in
        for (int ti = 0; ti < terrains.length; ti++)
        {
            buf = new StringBuffer();
            List recruiters = Creature.getCreatures();
            for (int ri = 0; ri < recruiters.size(); ri++)
            {
                final Creature recruiter = (Creature)recruiters.get(ri);
                int num = TerrainRecruitLoader.numberOfRecruiterNeeded(
                    recruiter, creature, terrains[ti], null);
                if (num == 1)
                {
                    buf.append("by 1 " + recruiter.getName() + ", ");
                }
                else if ((num > 0) && (num < RecruitGraph.BIGNUM))
                {
                    buf.append("by " + num + " " + recruiter.getPluralName()
                        + ", ");
                }
            }
            if (buf.length() > 0)
            {
                Color color = TerrainRecruitLoader.getTerrainColor(
                    terrains[ti]).brighter();
                s
                    .append(MessageFormat
                        .format(
                            "<tr><td bgcolor={0}>in {1}</td>"
                                + "<td colspan={2} nowrap><font color=blue>{3}</font></td>"
                                + "</tr>", new Object[] {
                                HTMLColor.colorToCode(color),
                                terrains[ti],
                                ""
                                    + (HazardTerrain.getAllHazardTerrains()
                                        .size() + 1), buf.toString(), }));
            }
        }
        //   out
        for (int ti = 0; ti < terrains.length; ti++)
        {
            buf = new StringBuffer();
            List recruits = Creature.getCreatures();
            for (int ri = 0; ri < recruits.size(); ri++)
            {
                final Creature recruit = (Creature)recruits.get(ri);
                int num = TerrainRecruitLoader.numberOfRecruiterNeeded(
                    creature, recruit, terrains[ti], null);
                if ((num > 0) && (num < RecruitGraph.BIGNUM))
                {
                    buf.append(num + " recruit a " + recruit.getName() + ", ");
                }
            }
            if (buf.length() > 0)
            {
                Color color = TerrainRecruitLoader.getTerrainColor(
                    terrains[ti]).brighter();
                s
                    .append(MessageFormat
                        .format(
                            "<tr><td bgcolor={0}>in {1}</td>"
                                + "<td colspan={2} nowrap><font color=green>{3}</font></td>"
                                + "</tr>", new Object[] {
                                HTMLColor.colorToCode(color),
                                terrains[ti],
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
            HazardTerrain.PLAINS);
        SimulatedCritter other = new SimulatedCritter(creature,
            HazardTerrain.PLAINS);
        //   hazards row 1
        s.append("<tr><td ROWSPAN=2 align=right>" + creature.getName()
            + " in</td><td></td>");
        for (Iterator iterator = HazardTerrain.getAllHazardTerrains()
            .iterator(); iterator.hasNext();)
        {
            iterator.next(); // skip one
            if (!iterator.hasNext())
            {
                break;
            }
            HazardTerrain terrain = (HazardTerrain)iterator.next();
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
        for (Iterator iterator = HazardTerrain.getAllHazardTerrains()
            .iterator(); iterator.hasNext();)
        {
            HazardTerrain terrain = (HazardTerrain)iterator.next();
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
        for (Iterator iterator = HazardTerrain.getAllHazardTerrains()
            .iterator(); iterator.hasNext();)
        {
            HazardTerrain terrain = (HazardTerrain)iterator.next();
            critter.setNewHazardHex(terrain);
            Color color = critter.getHazardColor().brighter();
            s.append(MessageFormat.format("<td bgcolor={0}>{1}</td>",
                new Object[] { HTMLColor.colorToCode(color),
                    "" + critter.getSimulatedPower(other), }));
        }
        s.append("<td bgcolor=#dddddd></td></tr>");
        //   ... my strike skill
        s.append("<tr><th nowrap>my Strike Skill</th>");
        for (Iterator iterator = HazardTerrain.getAllHazardTerrains()
            .iterator(); iterator.hasNext();)
        {
            HazardTerrain terrain = (HazardTerrain)iterator.next();
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
            .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
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
        "cliff", "slope", "tower", "river" };

    /** html header and start of page. */
    private static void _head(StringBuffer s, final Creature cr)
    {
        s.append("<html><head></head><body bgcolor="
            + HTMLColor.colorToCode(Color.LIGHT_GRAY) + ">");
        s.append("<h2>Creature Details: <b>" + cr.getName() + "</b></h2>");
        s.append("<table width=100%>");
    }

    /** start of a named section.
     * @param s in/out
     */
    private static void _section(StringBuffer s, final String name)
    {
        s.append("<tr bgcolor=gray><td colspan="
            + (HazardTerrain.getAllHazardTerrains().size() + 1) + ">");
        s.append("<b>" + name + "</b>");
        s.append("</td></tr>");
    }

    /** a headered table row, the data column spans.
     * @param s in/out
     */
    private static void _trSpan(StringBuffer s, final String name,
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
        public char getOppositeHexside(final int i)
        {
            return ' '; // plain hex side for now
        }
    }

    /** helper class to simulate a battle of the creature in question against
     * an other creature. especially distance and hazard must be simulated.
     * very fragile class, i suppose. but it might be worth it.
     * @author Towi
     */
    final class SimulatedCritter extends Critter
    {

        /** catch calls to "underlying" battle hex and proxy it to this. */
        private SimulatedBattleHex hex;

        /** @param creature to create a critter for
         * @param hazard that stands in this hazard */
        SimulatedCritter(final Creature creature, final HazardTerrain hazard)
        {
            super(creature, "markerId", null);
            setNewHazardHex(hazard);
        }

        /** in hazard Plains. */
        SimulatedCritter(final Creature creature)
        {
            this(creature, HazardTerrain.PLAINS);
        }

        /** create the simulated hex. */
        public void setNewHazardHex(final HazardTerrain hazard)
        {
            hex = new SimulatedBattleHex(hazard);
        }

        /** power of this creature hitting target. */
        public int getSimulatedPower(final Critter target)
        {
            return getDice(target);
        }

        /** skill of this creature hitting target. */
        public int getSimulatedSkill(final Critter target)
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
        protected BattleHex getCurrentHex()
        {
            return hex;
        }

        /** fake -- true or false as wished. TODO */
        protected boolean isInContact(final boolean countDead)
        {
            return true;
        }

        /** not needed here -- fake returns 0. */
        protected int countBrambleHexes(final BattleHex targetHex)
        {
            return 0;
        }
    }

    //
    // mouse and window events
    //
    /** disposes. */
    public void mouseClicked(MouseEvent e)
    {
        dispose();
    }

    /** disposes. */
    public void mousePressed(MouseEvent e)
    {
        dispose();
    }

    /** disposes. */
    public void mouseReleased(MouseEvent e)
    {
        dispose();
    }

    /** disposes. */
    public void windowClosing(WindowEvent e)
    {
        dispose();
    }

}
