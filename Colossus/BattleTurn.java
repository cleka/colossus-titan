import java.awt.*;
import java.awt.event.*;

/**
 * Class BattleTurn gets and holds chronological and sequence data for a battle.
 * @version $Id$
 * author David Ripton
 */

class BattleTurn extends Dialog implements ActionListener, WindowListener
{
    // phases of a turn
    public static final int SUMMON = 0;
    public static final int RECRUIT = 1;
    public static final int MOVE = 2;
    public static final int FIGHT = 3;
    public static final int STRIKEBACK = 4;

    private Frame parentFrame;
    private BattleMap map;
    private Legion attacker;
    private Legion defender;
    private Legion activeLegion;
    private int turnNumber = 1;
    private int phase = MOVE;
    private SummonAngel summonAngel;
    private boolean summoningAngel = false;
    private static Point location;


    BattleTurn(Frame parentFrame, BattleMap map, Legion attacker, Legion
        defender)
    {
        super(parentFrame, defender.getPlayer().getName() + " Turn 1");

        this.parentFrame = parentFrame;
        this.map = map;
        this.attacker = attacker;
        this.defender = defender;
        activeLegion = defender;
        
        setBackground(Color.lightGray);

        setupMoveDialog();

        pack();

        // Place this window in the upper left corner, or at the saved
        // location of the last BattleTurn. 
        if (location == null)
        {
            location = new Point(0, 0);
        }
        setLocation(location);

        setVisible(true);
    }


    void setupRecruitDialog()
    {
        removeAll();
        setTitle(getActivePlayer().getName() + " Turn " + turnNumber);
        setLayout(new GridLayout(0, 1));
        add(new Label(getActivePlayer().getName() + " : Recruit"));

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
        removeAll();
        setTitle(getActivePlayer().getName() + " Turn " + turnNumber);
        setLayout(new GridLayout(0, 1));
        add(new Label(getActivePlayer().getName() + " : Summon"));

        int summonState = map.getSummonState();

        if (summonState == BattleMap.FIRST_BLOOD)
        {
            if (attacker.canSummonAngel())
            {
                summoningAngel = true;

                // Make sure the MasterBoard is visible.
                map.getBoard().deiconify();
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
            removeAll();
            setTitle(getActivePlayer().getName() + " Turn " + turnNumber);
            setLayout(new GridLayout(0, 5));

            add(new Label(getActivePlayer().getName() + " : Move"));

            Button button1 = new Button("Undo Last Move");
            add(button1);
            button1.addActionListener(this);

            Button button2 = new Button("Undo All Moves");
            add(button2);
            button2.addActionListener(this);

            Button button3 = new Button("Concede Battle");
            add(button3);
            button3.addActionListener(this);

            Button button4 = new Button("Done with Moves");
            add(button4);
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
            removeAll();
            setLayout(new GridLayout(0, 3));

            add(new Label(getActivePlayer().getName() + 
                ((phase == FIGHT) ? " : Strike" : " : Strikeback")));

            Button button1 = new Button("Concede Battle");
            add(button1);
            button1.addActionListener(this);
            
            Button button2 = new Button("Done with Strikes");
            add(button2);
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
                            player.die(defender.getPlayer(), true);
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


    public void cleanup()
    {
        location = getLocation();
        dispose();
    }


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Undo Last Move"))
        {
            map.undoLastMove();
        }

        else if (e.getActionCommand().equals("Undo All Moves"))
        {
            map.undoAllMoves();
        }

        else if (e.getActionCommand().equals("Done with Moves"))
        {
            map.removeOffboardChits();
            map.commitMoves();
            advancePhase();
        }

        else if (e.getActionCommand().equals("Done with Strikes"))
        {
            // Advance only if there are no unresolved strikes.
            if (!map.forcedStrikesRemain())
            {
                map.commitStrikes();
                advancePhase();
            }
        }

        else if (e.getActionCommand().equals("Concede Battle"))
        {
            // XXX: Concession timing is tricky.
            new OptionDialog(parentFrame, "Confirm Concession",
                "Are you sure you want to concede the battle?",
                "Yes", "No");
            if (OptionDialog.getLastAnswer() == OptionDialog.YES_OPTION)
            {
                map.concede(getActivePlayer());
            }
            advancePhase();
        }
    }


    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
    }

    public void windowDeactivated(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowOpened(WindowEvent e)
    {
    }
}
