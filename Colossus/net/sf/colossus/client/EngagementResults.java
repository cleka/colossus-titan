package net.sf.colossus.client;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Options;


/**
 * Post-engagement status dialog.
 *
 * It collects the results of all battles that are send by 'addData()`.
 * after that it pops open the dialog, presenting the just added entry.
 * In the dialog one can go forward and back, and drop boring results.
 *
 * @version $Id$
 * @author Towi
 * @author David Ripton
 */

final class EngagementResults
    extends KDialog
{
    private IOracle oracle;
    private IOptions options;
    private int current = -1;
    private int lastSeen = -1;
    private ArrayList engagementLog = new ArrayList();
	private SaveWindow saveWindow;

    /** 
     * inits the diaolog, not opens it.
     * @param frame is the parent window
     * @param oracle gives us information
     */
    EngagementResults(final JFrame frame, final IOracle oracle, 
            IOptions options)
    {
        super(frame, "Engagement Status", false);
        setFocusable(false);
        this.oracle = oracle;
        this.options = options;
        setBackground(Color.lightGray);
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        
        this.saveWindow = new SaveWindow(options, "EngagementResultView");
        saveWindow.restore(this, new Point(0,0));
    }

    /** adds a log record to the list of logged engagements.
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
     */
    void addData(
        String winnerId, // null on mutual elim, flee, concede, negotiate
        String method,
        int points,
        int turns,
        List attackerStartingContents,
        List defenderStartingContents,
        List attackerStartingCertainities,
        List defenderStartingCertainities
        )
    {
        Engagement result = new Engagement(winnerId,
                method,
                points,
                turns,
                attackerStartingContents,
                defenderStartingContents,
                attackerStartingCertainities,
                defenderStartingCertainities,
                oracle
        );       
        EngagementLogEntry elog = new EngagementLogEntry(result);
        engagementLog.add(elog);
        
        if(this.current == -1)
        {
        	this.current = 0;
        }

        showCurrent();
    }


    /** one log entry. can show itself into a swing container.
     * @todo the model part has been extracted into ths Engagement class,
     * this class should disappear now in favour of doing the GUI layout only once
     * in the outer class.
     */
    private class EngagementLogEntry
    {
        private Engagement result;

		public EngagementLogEntry(Engagement result) {
			this.result = result;
		}

        /** make some of the background colors more distiguishable */
        private final static boolean COLORFUL = true;
		private JButton firstButton;
		private JButton prevButton;
		private JButton nextButton;
		private JButton lastButton;
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
        void show(Container cnt)
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
            panelCenter.setLayout(new GridLayout(3,1, 0,2));
            cnt.add(panelCenter, BorderLayout.CENTER);

            //    space for list labels
            JPanel panelWest = new JPanel();
            panelWest.setLayout(new GridLayout(6,1, 0,2));
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
            panelNorth.add(new JLabel("Turn " + result.gameTurn));
            panelNorth.add(new JLabel(result.attackerId + " attacked " + result.defenderId
                + " in "
                + MasterBoard.getHexByLabel(result.hexLabel).getDescription()));
            panelNorth.add(new JLabel(result.getResultText()));
            if(result.winnerId!=null)
            {
                panelNorth.add(new JLabel(result.winnerId + " earned "
                    + result.points + " points"));
            }

            // west
            panelWest.add(new JLabel("Atacker"));
            JLabel l = new JLabel(result.attackerId);
            l.setForeground(Color.BLUE);
            panelWest.add(l);
            panelWest.add(new JLabel("Defender"));
            l = new JLabel(result.defenderId);
            l.setForeground(Color.BLUE);
            panelWest.add(l);
            panelWest.add(new JLabel("Winner"));


            //  center
            _legion(result.attackerId,
                result.attackerStartingContents, result.attackerStartingCertainities,
                panelCenter, true, false, false);

            _legion(result.defenderId,
                result.defenderStartingContents, result.defenderStartingCertainities,
                panelCenter, true, true, false);
            if (result.attackerId.equals(result.winnerId))
            {
                _legion(result.attackerId,
                    result.attackerEndingContents, result.attackerEndingCertainties,
                    panelCenter, false, false, false);
            }
            else if (result.defenderId.equals(result.winnerId))
            {
                _legion(result.defenderId,
                result.defenderEndingContents, result.defenderEndingCertainties,
                panelCenter, false, true, false);
            }
 
            //  south
            this.firstButton = new JButton("First");
            firstButton.addActionListener(new ActionListener(){
            	public void actionPerformed(ActionEvent e){
                    current = 0;
                    showCurrent();
            	}    
            });
            panelSouth.add(firstButton);
            this.firstButton.setEnabled(current != 0);

            this.prevButton = new JButton("Previous");
            prevButton.addActionListener(new ActionListener() {
            	public void actionPerformed(ActionEvent e) {
                    current--;
                    showCurrent();
            	}   
            });
            panelSouth.add(prevButton);
            this.prevButton.setEnabled(current != 0);

            if(current != engagementLog.size()-1)
            {
                this.nextButton = new JButton("Next");
                nextButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        current++;
                        showCurrent();
        			}   
                });
            }
            else
            {
                this.nextButton = new JButton("Hide");
                nextButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        current++;
                        hide();
                    }   
                });
            }   
            panelSouth.add(nextButton);

            this.lastButton = new JButton("Last");
            lastButton.addActionListener(new ActionListener() {
            	public void actionPerformed(ActionEvent e) {
                    current = engagementLog.size() - 1;
                    showCurrent();
				}
            });
            panelSouth.add(lastButton);
            this.lastButton.setEnabled(current != engagementLog.size()-1);

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
            for (Iterator it = imageNames.iterator(); it.hasNext(); )
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
            elog.show(contentPane);
            this.setTitle("Engagement " + (current + 1)
                + " of " + engagementLog.size());
            this.lastSeen = Math.max(this.lastSeen, this.current);
        }
        maybeShow();
    }

    void maybeShow()
    {
        if (options.getOption(Options.showEngagementResults) &&
                engagementLog.size() != 0)
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

    public void dispose()
    {
        saveWindow.save(this);
        super.dispose();
    }

    public void hide()
    {
        saveWindow.save(this);
        super.hide();
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
        List attackerStartingContents;
        List defenderStartingContents;
        List attackerStartingCertainities;
        List defenderStartingCertainities;
		String hexLabel;
		int gameTurn;
		List attackerEndingContents;
		List defenderEndingContents;
		List attackerEndingCertainties;
		List defenderEndingCertainties;
        
		public Engagement(
		    String winnerId, 
            String method, 
            int points,
			int turns, 
            List attackerStartingContents,
			List defenderStartingContents,
			List attackerStartingCertainities,
			List defenderStartingCertainities,
            IOracle oracle
            )
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

            this.attackerEndingContents =
                oracle.getLegionImageNames(this.attackerId);
            this.defenderEndingContents =
                oracle.getLegionImageNames(this.defenderId);
            this.attackerEndingCertainties =
                oracle.getLegionCreatureCertainties(this.attackerId);
            this.defenderEndingCertainties =
                oracle.getLegionCreatureCertainties(this.defenderId);

            this.setWinnerAndLoserId();
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
                    this.winnerId=null; // @todo is this case possible at all? What does it mean?
                }
            }
        }

        public String getResultText()
        {
            String result = "bogus method";
            if (method.equals("flee"))
            {
                result = winnerId + " won when " + loserId + " fled";
            }
            else if (method.equals("concede"))
            {
                result = winnerId + " won when " + loserId + " conceded";
            }
            else if (method.equals("negotiate"))
            {
                if (winnerId != null)
                {
                    result = winnerId + " won a negotiated settlement";
                }
                else
                {
                    result = "Negotiated mutual elimination";
                }
            }
            else if (method.equals("fight"))
            {
                if (winnerId != null)
                {
                    result = winnerId + " won the battle in "
                        + this.turns +  " turns";
                }
                else
                {
                    result = "Mutual elimination in "
                        + this.turns + " turns";
                }
            }
            return result;
        }
    }
}
