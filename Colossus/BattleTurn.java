import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class BattleTurn gets and holds chronological and sequence data for a battle.
 * @version $Id$
 * author David Ripton
 */

class BattleTurn extends JDialog implements ActionListener
{
    // phases of a turn
    public static final int SUMMON = 0;
    public static final int RECRUIT = 1;
    public static final int MOVE = 2;
    public static final int FIGHT = 3;
    public static final int STRIKEBACK = 4;

    private JFrame parentFrame;
    private BattleMap map;
    private Legion attacker;
    private Legion defender;
    private Legion activeLegion;
    private int turnNumber = 1;
    private int phase = MOVE;
    private SummonAngel summonAngel;
    private boolean summoningAngel = false;
    private Container contentPane;


    BattleTurn(JFrame parentFrame, BattleMap map, Legion attacker, Legion
        defender)
    {
        super(parentFrame, defender.getPlayer().getName() + " Turn 1");

        this.parentFrame = parentFrame;
        this.map = map;
        this.attacker = attacker;
        this.defender = defender;
        activeLegion = defender;
        
        setBackground(java.awt.Color.lightGray);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        setupMoveDialog();

        pack();

        // Place this window in the upper left corner.
        setLocation(new Point(0, 0));

        setVisible(true);

        // Make sure that this window is in front of the Turn window.
System.out.println("BattleTurn.toFront()");
        toFront();
    }


    void setupRecruitDialog()
    {
        contentPane = getContentPane();
        contentPane.removeAll();
        setTitle(getActivePlayer().getName() + " Turn " + turnNumber);
        contentPane.setLayout(new GridLayout(0, 1));
        contentPane.add(new JLabel(getActivePlayer().getName() + " : Recruit"));

        if (turnNumber == 4 && defender.canRecruit())
        {
            // Allow recruiting a reinforcement.
            new PickRecruit(map, defender);

            if (defender.recruited())
            {
                map.placeNewChit(defender);
            }
        }
            
        advancePhase();
    }
    
    
    void setupSummonDialog()
    {
        contentPane = getContentPane();
        contentPane.removeAll();
        setTitle(getActivePlayer().getName() + " Turn " + turnNumber);
        contentPane.setLayout(new GridLayout(0, 1));
        contentPane.add(new JLabel(getActivePlayer().getName() + " : Summon"));

        int summonState = map.getSummonState();

        if (summonState == BattleMap.FIRST_BLOOD)
        {
            if (attacker.getHeight() < 7 &&
                attacker.getPlayer().canSummonAngel())
            {
                summoningAngel = true;

                // Make sure the MasterBoard is visible.
                map.getBoard().deiconify();
System.out.println("map.getBoard().show()");
                map.getBoard().show();

                summonAngel = new SummonAngel(map.getBoard(), attacker);
                map.getBoard().setSummonAngel(summonAngel);
            }

            // This is the last chance to summon an angel until the
            // battle is over.
            map.setSummonState(map.TOO_LATE);
        }

        if (!summoningAngel)
        {
            if (phase == SUMMON)
            {
                advancePhase();
            }
        }
    }


    // This is called from MasterBoard after the SummonAngel finishes.
    void finishSummoningAngel()
    {
        if (attacker.summoned())
        {
            map.placeNewChit(attacker);
        }

        summoningAngel = false;

        if (phase == SUMMON)
        {
            advancePhase();
        }

        // Bring the BattleMap back to the front.
System.out.println("map.show()");
        map.show();
    }


    void setupMoveDialog()
    {
        // If there are no legal moves, move on.
        if (map.highlightMovableChits() < 1)
        {
            advancePhase();
        }
        else
        {
            contentPane = getContentPane();
            contentPane.removeAll();
            setTitle(getActivePlayer().getName() + " Turn " + turnNumber);
            contentPane.setLayout(new GridLayout(0, 5));

            contentPane.add(new JLabel(getActivePlayer().getName() + 
                " : Move"));

            JButton button1 = new JButton("Undo Last Move");
            contentPane.add(button1);
            button1.addActionListener(this);

            JButton button2 = new JButton("Undo All Moves");
            contentPane.add(button2);
            button2.addActionListener(this);

            JButton button3 = new JButton("Concede Battle");
            contentPane.add(button3);
            button3.addActionListener(this);

            JButton button4 = new JButton("Done with Moves");
            contentPane.add(button4);
            button4.addActionListener(this);

            pack();

        }
    }


    void setupFightDialog()
    {
        // Apply drift damage only once per player turn.
        if (phase == FIGHT)
        {
            map.applyDriftDamage();
        }

        // If there are no possible strikes, move on.
        if (map.highlightChitsWithTargets() < 1)
        {
            advancePhase();
        }
        else
        {
            contentPane = getContentPane();
            contentPane.removeAll();
            contentPane.setLayout(new GridLayout(0, 3));

            contentPane.add(new JLabel(getActivePlayer().getName() +
                ((phase == FIGHT) ? " : Strike" : " : Strikeback")));

            JButton button1 = new JButton("Concede Battle");
            contentPane.add(button1);
            button1.addActionListener(this);
            
            JButton button2 = new JButton("Done with Strikes");
            contentPane.add(button2);
            button2.addActionListener(this);

            pack();
        }
    }


    Legion getActiveLegion()
    {
        return activeLegion;
    }


    Player getActivePlayer()
    {
        return activeLegion.getPlayer();
    }


    int getPhase()
    {
        return phase;
    }


    int getTurnNumber()
    {
        return turnNumber;
    }


    void advancePhase()
    {
        if (phase == SUMMON)
        {
            phase = MOVE;
            setupMoveDialog();
        }
        
        else if (phase == RECRUIT)
        {
            phase = MOVE;
            setupMoveDialog();
        }

        else if (phase == MOVE)
        {
            phase = FIGHT;
            setupFightDialog();
        }

        else if (phase == FIGHT)
        {
            if (activeLegion == defender)
            {
                activeLegion = attacker;
            }
            else
            {
                activeLegion = defender;
            }

            phase = STRIKEBACK;
            setupFightDialog();
        }

        else if (phase == STRIKEBACK)
        {
            map.removeDeadChits();

            // Make sure the battle isn't over before continuing.
            if (attacker.getHeight() >= 1 && defender.getHeight() >= 1)
            {
                if (activeLegion == attacker)
                {
                    phase = SUMMON;
                    setupSummonDialog();
                }
                else
                {
                    turnNumber++;
                    if (turnNumber > 7)
                    {
                        // Time loss.  Attacker is eliminated but defender
                        //    gets no points.
                        if (attacker.numCreature(Creature.titan) != 0)
                        {
                            // This is the attacker's titan stack, so the 
                            // defender gets his markers plus half points for 
                            // his unengaged legions.
                            Player player = attacker.getPlayer();
                            attacker.removeLegion();
                            player.die(defender.getPlayer());
                        }
                        else
                        {
                            attacker.removeLegion();
                        }
                        map.cleanup();
                    }
                    else
                    {
                        phase = RECRUIT;
                        setupRecruitDialog();
                    }
                }
            }
        }
    }


    SummonAngel getSummonAngel()
    {
        return summonAngel;
    }


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand() == "Undo Last Move")
        {
            map.undoLastMove();
        }

        else if (e.getActionCommand() == "Undo All Moves")
        {
            map.undoAllMoves();
        }

        else if (e.getActionCommand() == "Done with Moves")
        {
            map.removeOffboardChits();
            map.commitMoves();
            advancePhase();
        }

        else if (e.getActionCommand() == "Done with Strikes")
        {
            // Advance only if there are no unresolved strikes.
            if (!map.forcedStrikesRemain())
            {
                map.commitStrikes();
                advancePhase();
            }
        }

        else if (e.getActionCommand() == "Concede Battle")
        {
            // XXX: Concession timing is tricky.
            int answer = JOptionPane.showConfirmDialog(parentFrame, 
                "Are you sure you want to concede the battle?");
            if (answer == JOptionPane.YES_OPTION)
            {
                map.concede(getActivePlayer());
            }
            advancePhase();
        }
    }
}
