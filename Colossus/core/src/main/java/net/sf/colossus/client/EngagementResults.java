package net.sf.colossus.client;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import net.sf.colossus.server.Constants;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Options;


/**
 * Post-engagement status dialog.
 *
 * It collects the results of all battles that are send by 'addData()`.
 *
 * @todo fix layout so the empty dialog can be shown 
 *
 * @version $Id$
 * @author Towi
 * @author David Ripton
 */

final class EngagementResults extends KDialog
{
    private IOracle oracle;
    private IOptions options;

    private int current = -1;
    private int lastSeen = -1;

    private final ArrayList<Engagement> engagementLog = new ArrayList<Engagement>();
    private final SaveWindow saveWindow;

    private JButton firstButton;
    private JButton prevButton;
    private JButton nextButton;
    private JButton lastButton;
    private JLabel summaryLabel;
    private JLabel resultLabel;
    private JLabel attackerIdLabel;
    private JLabel defenderIdLabel;
    private JPanel panelCenter;
    private boolean moveNext;
    private boolean advanceToLast = false;

    /** 
     * Inits the dialog, not opens it.
     * 
     * @param frame is the parent window
     * @param oracle gives us information
     */
    EngagementResults(final JFrame frame, final IOracle oracle,
        final IOptions options)
    {
        super(frame, "Engagement Status", false);
        setFocusable(false);
        this.oracle = oracle;
        this.options = options;
        setBackground(Color.lightGray);
        setupGUI();

        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        this.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                options.setOption(Options.showEngagementResults, false);
            }
        });

        this.saveWindow = new SaveWindow(options, "EngagementResultView");
        saveWindow.restore(this, new Point(0, 0));

        maybeShow();
    }

    /**
     * Adds a log record to the list of logged engagements.
     * 
     * Now the dialog moves to the new engagement if either
     * - the engagement happens in the attacker's turn
     * - it is the first one after the attacker's turn
     * The idea behind this design is that the dialog content moves along with 
     * the player when the player is in charge of the game tempo, but if the player
     * is only passive the engagements stop moving until the player takes control 
     * by either continuing to play or by clicking the next button.
     *
     * TODO: see if xxxStartingCertainities can somehow get values
     *   of better quality.
     *
     * @param attackerStartingContents - imagew names,
     *   result from oracle.getLegionImageNames
     * @param defenderStartingContents - imagew names,
     *   result from oracle.getLegionImageNames
     * @param attackerStartingCertainities - list of Booleans,
     *   for overlay ?-marks
     * @param defenderStartingCertainities - list of Booleans,
     *   for overlay ?-marks
     * @param attackersTurn should be set to true if the engagement happened
     *   in the attackers master board turn. The engagement dialog will be moved
     *   to this engagement, the same will happen with the next
     */
    void addData(
        String winnerId, // null on mutual elim, flee, concede, negotiate
        String method, int points, int turns,
        List<String> attackerStartingContents,
        List<String> defenderStartingContents,
        List<Boolean> attackerStartingCertainities,
        List<Boolean> defenderStartingCertainities, boolean attackersTurn)
    {
        Engagement result = new Engagement(winnerId, method, points, turns,
            attackerStartingContents, defenderStartingContents,
            attackerStartingCertainities, defenderStartingCertainities, oracle);
        this.engagementLog.add(result);

        if (this.current <= -1)
        {
            this.current = 0;
        }

        if (attackersTurn || this.moveNext || advanceToLast)
        {
            this.current = this.engagementLog.size() - 1;
        }
        // iff we are in the attackers turn, the next engagement
        // should be placed automatically, too
        this.moveNext = attackersTurn;

        showCurrent();
        maybeShow();
    }

    /** like toString into a swing component.
     * the current rough layout is:
     * <pre>
     *  ### Content:BorderLayout ########################
     *  # +--North:GridLayout(n,1)--------------------+ #
     *  # | Label_1                                   | #
     *  # | Label_2                                   | #
     *  # | ...                                       | #
     *  # | Label_n                                   | #
     *  # +-------------------------------------------+ #
     *  #===============================================#
     *  # +West:Grid(4,1)-+  %  +-Center:Grid(4,1)----+ #
     *  # | Label_bef_att |  %  | ImageList_bef_att   | #
     *  # | Label_bef_def |  %  | ImageList_bef_def   | #
     *  # | Label_aft_att |  %  | ImageList_aft_att   | #
     *  # | Label_aft_def |  %  | ImageList_aft_def   | #
     *  # +---------------+  %  +---------------------+ #
     *  #===============================================#
     *  # +-South:FlowLayout(left)--------------------+ #
     *  # |  -buttons-                                | #
     *  # +-------------------------------------------+ #
     *  #################################################
     * </pre>
     */
    private void setupGUI()
    {
        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout());

        //    space for Labels
        JPanel panelNorth = new JPanel();
        panelNorth.setLayout(new GridLayout(2, 1, 0, 2));
        panelNorth.setBackground(Color.GRAY);
        contentPane.add(panelNorth, BorderLayout.NORTH);

        //    space for imagelists
        this.panelCenter = new JPanel();
        panelCenter.setLayout(new GridLayout(3, 1, 0, 2));
        contentPane.add(panelCenter, BorderLayout.CENTER);

        //    space for list labels
        JPanel panelWest = new JPanel();
        panelWest.setLayout(new GridLayout(6, 1, 0, 2));
        contentPane.add(panelWest, BorderLayout.WEST);

        //    space for navigate buttons
        JPanel panelSouth = new JPanel();
        panelSouth.setLayout(new FlowLayout(FlowLayout.LEFT));
        contentPane.add(panelSouth, BorderLayout.SOUTH);
        panelSouth.setBackground(Color.GRAY);

        //
        // add elements
        //
        //  north
        this.summaryLabel = new JLabel();
        this.resultLabel = new JLabel();

        panelNorth.add(this.summaryLabel);
        panelNorth.add(this.resultLabel);

        // west
        this.attackerIdLabel = new JLabel();
        this.attackerIdLabel.setForeground(Color.BLUE);
        this.defenderIdLabel = new JLabel();
        this.defenderIdLabel.setForeground(Color.BLUE);

        panelWest.add(new JLabel("Attacker"));
        panelWest.add(this.attackerIdLabel);
        panelWest.add(new JLabel("Defender"));
        panelWest.add(this.defenderIdLabel);
        panelWest.add(new JLabel("Winner"));

        //  center gets only prepared for the chits

        //  south
        this.firstButton = new JButton("First");
        firstButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                current = 0;
                advanceToLast = false;
                showCurrent();
            }
        });
        panelSouth.add(firstButton);

        this.prevButton = new JButton("Previous");
        prevButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                current--;
                advanceToLast = false;
                showCurrent();
            }
        });
        panelSouth.add(prevButton);

        this.nextButton = new JButton("Next");
        nextButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                current++;
                advanceToLast = false;
                showCurrent();
            }
        });
        panelSouth.add(nextButton);

        this.lastButton = new JButton("Last");
        lastButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                current = engagementLog.size() - 1;
                advanceToLast = true;
                showCurrent();
            }
        });
        panelSouth.add(lastButton);

        JButton hideButton = new JButton("Hide");
        hideButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                setVisible(false);
            }
        });
        panelSouth.add(hideButton);

        this.firstButton.setEnabled(false);
        this.prevButton.setEnabled(false);
        this.nextButton.setEnabled(false);
        this.lastButton.setEnabled(false);
    }

    private Component createLegionComponent(String markerId,
        List<String> imageNames, List<Boolean> certainList, boolean isDefender)
    {
        // prepare my box
        Box panel = Box.createHorizontalBox();
        if (isDefender)
        {
            panel.setBackground(Color.WHITE);
        }
        int scale = 3 * Scale.get(); // or 3 or 4?
        // add marker
        Marker marker = new Marker(scale, markerId);
        panel.add(marker);
        panel.add(Box.createHorizontalStrut(5));
        // towi: you want it upside down or not? then remove "false"
        final boolean inverse = false && isDefender;
        // add chits
        int idx = 0;
        for (Iterator<String> it = imageNames.iterator(); it.hasNext();)
        {
            final String imageName = it.next();
            final Boolean chitCertain = certainList.get(idx);
            final boolean showDubious = !chitCertain.booleanValue();
            Chit chit = new Chit(scale, imageName, inverse, showDubious);
            panel.add(chit);
            idx += 1;
        }
        // make list always 7 wide with invisible chits. not perfect.
        for (int i = idx; i < 7; i++)
        {
            panel.add(Box.createRigidArea(new Dimension(scale, scale)));
        }
        // expand space
        panel.add(Box.createHorizontalGlue());
        return panel;
    }

    private void showCurrent()
    {
        if (engagementLog.size() == 0)
        {
            // TODO: this shrinks the dialog to a tiny size.
            //   for this reason we disallowed dropping 
            //   the last in the action handler. 
            //            Container contentPane = getContentPane();
            //            contentPane.removeAll();
            this.setTitle("no Engagements");
            this.firstButton.setEnabled(false);
            this.prevButton.setEnabled(false);
            this.nextButton.setEnabled(false);
            this.lastButton.setEnabled(false);

        }
        else
        {
            Engagement result = engagementLog.get(current);
            this.setTitle("Engagement " + (current + 1) + " of "
                + engagementLog.size());
            this.summaryLabel.setText(result.getSummary());
            this.resultLabel.setText(result.getResultText());
            this.attackerIdLabel.setText(result.attackerId);
            this.defenderIdLabel.setText(result.defenderId);

            this.firstButton.setEnabled(current != 0);
            this.prevButton.setEnabled(current != 0);
            this.nextButton.setEnabled(current != engagementLog.size() - 1);
            this.lastButton.setEnabled(current != engagementLog.size() - 1);

            this.panelCenter.removeAll();
            this.panelCenter.add(createLegionComponent(result.attackerId,
                result.attackerStartingContents,
                result.attackerStartingCertainities, false));
            this.panelCenter.add(createLegionComponent(result.defenderId,
                result.defenderStartingContents,
                result.defenderStartingCertainities, true));
            if (result.attackerId.equals(result.winnerId))
            {
                this.panelCenter.add(createLegionComponent(result.attackerId,
                    result.attackerEndingContents,
                    result.attackerEndingCertainties, false));
            }
            else if (result.defenderId.equals(result.winnerId))
            {
                this.panelCenter.add(createLegionComponent(result.defenderId,
                    result.defenderEndingContents,
                    result.defenderEndingCertainties, true));
            }
            else
            {
                this.panelCenter.add(new JPanel());
            }

            this.lastSeen = Math.max(this.lastSeen, this.current);
        }
    }

    void maybeShow()
    {
        if (options.getOption(Options.showEngagementResults))
        {
            pack();
            if (!isVisible())
            {
                setVisible(true);
            }
        }
        else
        {
            if (isVisible())
            {
                setVisible(false);
            }
        }
    }

    @Override
    public void dispose()
    {
        saveWindow.save(this);
        super.dispose();
        this.options = null;
        this.oracle = null;
    }

    @Override
    public void setVisible(boolean visible)
    {
        saveWindow.save(this);
        super.setVisible(visible);
    }

    /**
     * Stores information about an engagement.
     * 
     * @todo: it might be useful to have this as a proper toplevel class
     */
    private class Engagement
    {
        String winnerId; // null on mutual elim, flee, concede, negotiate
        String loserId;
        String attackerId;
        String defenderId;
        String method;
        int points;
        int turns;
        List<String> attackerStartingContents;
        List<String> defenderStartingContents;
        List<Boolean> attackerStartingCertainities;
        List<Boolean> defenderStartingCertainities;
        String hexLabel;
        int gameTurn;
        List<String> attackerEndingContents;
        List<String> defenderEndingContents;
        List<Boolean> attackerEndingCertainties;
        List<Boolean> defenderEndingCertainties;

        public Engagement(String winnerId, String method, int points,
            int turns, List<String> attackerStartingContents,
            List<String> defenderStartingContents,
            List<Boolean> attackerStartingCertainities,
            List<Boolean> defenderStartingCertainities, IOracle oracle)
        {
            this.winnerId = winnerId;
            this.method = method;
            this.points = points;
            this.turns = turns;
            this.attackerStartingContents = attackerStartingContents;
            this.defenderStartingContents = defenderStartingContents;
            this.attackerStartingCertainities = attackerStartingCertainities;
            this.defenderStartingCertainities = defenderStartingCertainities;
            this.hexLabel = oracle.getBattleSite();
            this.attackerId = oracle.getAttackerMarkerId();
            this.defenderId = oracle.getDefenderMarkerId();
            this.gameTurn = oracle.getTurnNumber();

            this.attackerEndingContents = oracle
                .getLegionImageNames(this.attackerId);
            this.defenderEndingContents = oracle
                .getLegionImageNames(this.defenderId);
            this.attackerEndingCertainties = oracle
                .getLegionCreatureCertainties(this.attackerId);
            this.defenderEndingCertainties = oracle
                .getLegionCreatureCertainties(this.defenderId);

            this.setWinnerAndLoserId();
        }

        public String getSummary()
        {
            return "On turn " + this.gameTurn + ", " + this.attackerId
                + " attacked " + this.defenderId + " in "
                + MasterBoard.getHexByLabel(this.hexLabel).getDescription();
        }

        private void setWinnerAndLoserId()
        {
            this.loserId = null;
            if (this.winnerId != null)
            {
                if (this.winnerId.equals(this.attackerId))
                {
                    this.loserId = this.defenderId;
                }
                else if (this.winnerId.equals(this.defenderId))
                {
                    this.loserId = this.attackerId;
                }
                else
                {
                    this.winnerId = null; // @todo is this case possible at all? What does it mean?
                }
            }
        }

        public String getResultText()
        {
            String result = "bogus method";
            if (method.equals(Constants.erMethodFlee))
            {
                result = winnerId + " won when " + loserId
                    + " fled and earned " + this.points + " points";
            }
            else if (method.equals(Constants.erMethodConcede))
            {
                result = winnerId + " won when " + loserId
                    + " conceded and earned " + this.points + " points";
            }
            else if (method.equals(Constants.erMethodConcede))
            {
                if (winnerId != null)
                {
                    result = winnerId
                        + " won a negotiated settlement and earned "
                        + this.points + " points";
                }
                else
                {
                    result = "Negotiated mutual elimination";
                }
            }
            else if (method.equals(Constants.erMethodFight))
            {
                if (winnerId != null)
                {
                    if (turns > 7)
                    {
                        result = winnerId + " won the battle by time loss"
                            + " and earned " + this.points + " points";
                    }
                    else
                    {
                        result = winnerId + " won the battle in " + this.turns
                            + " turns and earned " + this.points + " points";
                    }
                }
                else
                {
                    result = "Mutual elimination in " + this.turns + " turns";
                }
            }
            return result;
        }
    }
}
