package net.sf.colossus.client;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Log;


/**
 * Post-engagement status dialog.
 *
 * It collects the results of all battles that are send by 'addData()`.
 * after that it pops open the dialog, presenting the just added entry.
 * In the dialog one can go forward and back, and drop boring results.
 *
 * TODO: hide and show this dialog from master board.
 *
 * @version $Id$
 * @author David Ripton
 * @author Towi
 */

final class EngagementResults
    extends KDialog
    implements ActionListener, WindowListener
{
    private Client client;

    /** 
     * inits the diaolog, not opens it.
     * @param frame is the parent window
     * @param client is the colossus.client to talk to
     */
    EngagementResults(final JFrame frame, final Client aClient)
    {
        super(frame, "Engagement Status", false);
        setFocusable(false);
        this.client = aClient;
        pack(); // TODO: needed here? dunno.
        setBackground(Color.lightGray);
        addWindowListener(this);
    }


    private int current = -1;
    private ArrayList engagementLog = new ArrayList();
    /** adds a log record to the list of logged engagements.
     *
     * TODO: see if xxxStartingCerrainities can somehow get values
     *   of better quality.
     *
     * @param attackerStartingContents - imagew names,
     *   result from client.getLegionImageNames
     * @param defenderStartingContents - imagew names,
     *   result from client.getLegionImageNames
     * @param attackerStartingCertainities - list of Booleans,
     *   for overlay ?-marks
     * @param defenderStartingCertainities - list of Booleans,
     *   for overlay ?-marks
     */
    void addData(
        String winnerId, // null on mutual elim, flee, concede, negotiate
        String method,
        int points,
        List attackerStartingContents,
        List defenderStartingContents,
        List attackerStartingCertainities,
        List defenderStartingCertainities
        )
    {
        EngagementLogEntry elog = new EngagementLogEntry();

        elog.hexLabel = client.getBattleSite();
        elog.attackerId = client.getAttackerMarkerId();
        elog.defenderId = client.getDefenderMarkerId();
        elog.battleTurn = client.getBattleTurnNumber();
        elog.gameTurn = client.getTurnNumber();

        elog.attackerStartingContents = attackerStartingContents;
        elog.defenderStartingContents = defenderStartingContents;
        elog.attackerStartingCertainities = attackerStartingCertainities;
        elog.defenderStartingCertainities = defenderStartingCertainities;

        elog.attackerEndingContents =
            client.getLegionImageNames(elog.attackerId);
        elog.defenderEndingContents =
            client.getLegionImageNames(elog.defenderId);
        elog.attackerEndingCertainities =
            client.getLegionCreatureCertainties(elog.attackerId);
        elog.defenderEndingCertainities =
            client.getLegionCreatureCertainties(elog.defenderId);

        elog.setWinnerId(winnerId);
        elog.setMethod(method);

        engagementLog.add(elog);
        current = engagementLog.size() - 1;

        showCurrent();
    }


    /** one log entry. can show itself into a swing container.
     * TODO: make fields private and write a constructor with many args.
     */
    private class EngagementLogEntry
    {
        int gameTurn;
        String attackerId;
        String defenderId;
        String hexLabel;
        int battleTurn;
        String winnerId;
        String loserId;
        String method;
        String _result; // long string set with method
        int points;
        // image names        
        List attackerStartingContents; 
        List defenderStartingContents;
        List attackerEndingContents;
        List defenderEndingContents;
        // certainities
        List attackerStartingCertainities; 
        List defenderStartingCertainities;
        List attackerEndingCertainities;
        List defenderEndingCertainities;

        /** by setting the winnerId the loserId is also set. */
        void setWinnerId(String aWinnerId)
        {
            loserId = null;
            winnerId = aWinnerId;
            if (winnerId != null)
            {
                if (winnerId.equals(attackerId))
                {
                    loserId = defenderId;
                }
                else if (winnerId.equals(defenderId))
                {
                    loserId = attackerId;
                }
                else
                {
                    Log.warn("EngagementResults: Bogus winnerId:" + winnerId);
                }
            }
        }

        /** also calculates the '_result_ string for display. */
        void setMethod(String aMethod)
        {
            this.method = aMethod;
            this._result = "bogus method";
            if (method.equals("flee"))
            {
                _result = winnerId + " won when " + loserId + " fled";
            }
            else if (method.equals("concede"))
            {
                _result = winnerId + " won when " + loserId + " conceded";
            }
            else if (method.equals("negotiate"))
            {
                if (winnerId != null)
                {
                    _result = winnerId + " won a negotiated settlement";
                }
                else
                {
                    _result = "Negotiated mutual elimination";
                }
            }
            else if (method.equals("fight"))
            {
                if (winnerId != null)
                {
                    _result = winnerId + " won the battle in "
                        + battleTurn +  " turns";
                }
                else
                {
                    _result = "Mutual elimination in "
                        + battleTurn + " turns";
                }
            }
        }

        /** make some of the background colors more distiguishable */
        private final static boolean COLORFUL = true;
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
         *  # |  -prev-  -next-  -dismiss-                | #
         *  # +-------------------------------------------+ #
         *  #################################################
         * </pre>
         */
        void show(Container cnt, ActionListener al)
        {
            // setup the containers
            cnt.removeAll();
            cnt.setLayout(new BorderLayout());

            //    space for Labels
            JPanel panelNorth  = new JPanel();
            panelNorth.setLayout(new GridLayout(4,1, 0,2));
            cnt.add(panelNorth,  BorderLayout.NORTH);
            if(COLORFUL)
            {
                panelNorth.setBackground(Color.GRAY);
            }

            //    space for imagelists
            JPanel panelCenter = new JPanel();
            panelCenter.setLayout(new GridLayout(4,1, 0,2));
            cnt.add(panelCenter, BorderLayout.CENTER);

            //    space for list labels
            JPanel panelWest = new JPanel();
            panelWest.setLayout(new GridLayout(8,1, 0,2));
            cnt.add(panelWest, BorderLayout.WEST);

            //    space for navigate buttons
            JPanel panelSouth  = new JPanel();
            panelSouth.setLayout(new FlowLayout(FlowLayout.LEFT));
            cnt.add(panelSouth, BorderLayout.SOUTH);
            if(COLORFUL)
            {
                panelSouth.setBackground(Color.GRAY);
            }

            //
            // add elements
            //
            //  north
            panelNorth.add(new JLabel("Turn " + gameTurn));
            panelNorth.add(new JLabel(attackerId + " attacked " + defenderId
                + " in "
                + MasterBoard.getHexByLabel(hexLabel).getDescription()));
            panelNorth.add(new JLabel(_result));
            panelNorth.add(new JLabel(winnerId + " earned "
                + points + " points"));

            // west
            panelWest.add(new JLabel("Starting"));
            JLabel l = new JLabel(attackerId);
            l.setForeground(Color.BLUE);
            panelWest.add(l);
            panelWest.add(new JLabel("Final"));
            panelWest.add(new JLabel(""));

            panelWest.add(new JLabel(""));
            panelWest.add(new JLabel("Starting"));
            l = new JLabel(defenderId);
            l.setForeground(Color.BLUE);
            panelWest.add(l);
            panelWest.add(new JLabel("Final"));

            //  center
            _legion(attackerId,
                attackerStartingContents, attackerStartingCertainities,
                panelCenter, true, false, false);
            if (winnerId == null || winnerId.equals(defenderId))
            {
                _legion(attackerId,
                    attackerEndingContents, attackerEndingCertainities,
                    panelCenter, false, false, true);
            }
            else
            {
                _legion(attackerId,
                    attackerEndingContents, attackerEndingCertainities,
                    panelCenter, false, false, false);
            }

            _legion(defenderId,
                defenderStartingContents, defenderStartingCertainities,
                panelCenter, true, true, false);
            if (winnerId == null || winnerId.equals(attackerId))
            {
                _legion(defenderId,
                defenderEndingContents, defenderEndingCertainities,
                panelCenter, false, true, true);
            }
            else
            {
                _legion(defenderId,
                defenderEndingContents, defenderEndingCertainities,
                panelCenter, false, true, false);
            }

            //  south
            JButton prevButton = new JButton(PREV);
            prevButton.setActionCommand(PREV);
            prevButton.addActionListener(al);
            panelSouth.add(prevButton);

            JButton nextButton = new JButton(NEXT);
            nextButton.addActionListener(al);
            nextButton.setActionCommand(NEXT);
            panelSouth.add(nextButton);

            JButton dropButton = new JButton(DROP);
            dropButton.addActionListener(al);
            dropButton.setActionCommand(DROP);
            panelSouth.add(dropButton);

            // finish
        }

        /** helper for show */
        void _legion(String markerId,
                List imageNames,
                List certainList,
                Container cnt,
                boolean isStarting,
                boolean isDefender,
                boolean isDead)
        {
            // prepare my box
            Box panel = Box.createHorizontalBox();
            if (COLORFUL && isDefender)
            {
                panel.setBackground(Color.WHITE);
            }
            cnt.add(panel);
            int scale = 3 * Scale.get(); // or 3 or 4?
            // add marker
            Marker marker = new Marker(scale, markerId, cnt, null);
            if (isDead)
            {
                marker.setDead(true);
            }
            panel.add(marker);
            panel.add(Box.createHorizontalStrut(5));
            // towi: you want it upside down or not? then remove "false"
            final boolean inverse = false && isDefender;
            // add chits
            int idx = 0;
            Iterator it = imageNames.iterator();
            while (it.hasNext())
            {
                final String imageName = (String) it.next();
                final Boolean chitCertain = (Boolean) certainList.get(idx);
                final boolean showDubious = !chitCertain.booleanValue();
                Chit chit = new Chit(scale, imageName, cnt,
                    inverse, showDubious);
                if (isDead)
                {
                    chit.setDead(true);
                }
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
        }
    }

    private void showCurrent() 
    {
        if (engagementLog.size() == 0)
        {
            // TODO: this shrinks the dialog to a tiny size.
            //   for this reason we disallowed dropping 
            //   the last in the action handler. 
            Container contentPane = getContentPane();
            contentPane.removeAll();
            this.setTitle("no Engagements");
        }
        else
        {
            EngagementLogEntry elog =
                (EngagementLogEntry) engagementLog.get(current);
            Container contentPane = getContentPane();
            elog.show(contentPane, this);
            this.setTitle("Engagement " + (current + 1)
                + " of " + engagementLog.size());
        }
        // pop up the window
        if (!isVisible())
        {
            pack();
        }
        setVisible(true);
    }

    // button actions and labels
    private final static String NEXT = "Next";
    private final static String PREV = "Previous";
    private final static String DROP = "Drop";
    private final static String DISMISS = "Dismiss";

    /** handler for buttons */
    public void actionPerformed(ActionEvent e)
    {
        String cmd = e.getActionCommand();
        if (cmd.equals(NEXT))
        {
               current += 1;
        }
        else if (cmd.equals(PREV))
        {
            current -= 1;
        }
        else if (cmd.equals(DROP))
        {
            // TODO: currenty we disallow drop last.
            //   does not look good in dialog.
            if (engagementLog.size() > 1)
            {
                engagementLog.remove(current);
            }
        }
        // show
        current = between(0, current, engagementLog.size() - 1); // sanity
        showCurrent();
    }

    /** min max cascade */
    private final int between(int min, int val, int max)
    {
        return Math.min(Math.max(min, val), max);
    }

    /** just close it */
    public void windowClosing(WindowEvent e)
    {
        dispose();
    }
}
