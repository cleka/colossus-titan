import java.awt.*;
import java.awt.event.*;

/**
 * Class BattleTurn gets and holds chronological and sequence data for a battle.
 * @version $Id$
 * author David Ripton
 */

class BattleTurn extends Dialog implements ActionListener
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


    BattleTurn(Frame parentFrame, BattleMap map, Legion attacker, Legion
        defender)
    {
        super(parentFrame, defender.getPlayer().getName() + " Turn 1");

        this.parentFrame = parentFrame;
        this.map = map;
        this.attacker = attacker;
        this.defender = defender;
        activeLegion = defender;
        
        setBackground(java.awt.Color.lightGray);

        setupMoveDialog();

        // This is necessary to prevent a visible resize.
        pack();

        setVisible(true);

        // Make sure that this window is in front of the Turn window.
        toFront();
    }


    void setupRecruitDialog()
    {
System.out.println("setupRecruitDialog");
        removeAll();
        setTitle(getActivePlayer().getName() + " Turn " + turnNumber);
        setLayout(new GridLayout(0, 1));
        add(new Label(getActivePlayer().getName() + " : Recruit"));

        if (turnNumber == 4 && defender.canRecruit())
        {
System.out.println("recruiting time");
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
System.out.println("setupSummonDialog");
        removeAll();
        setTitle(getActivePlayer().getName() + " Turn " + turnNumber);
        setLayout(new GridLayout(0, 1));
        add(new Label(getActivePlayer().getName() + " : Summon"));

        int summonState = map.getSummonState();
System.out.println("summonState = " + summonState);

        if (summonState == BattleMap.FIRST_BLOOD)
        {
            if (attacker.getHeight() < 7 &&
                attacker.getPlayer().canSummonAngel())
            {
System.out.println("SummonAngel");
                summoningAngel = true;

                // Make sure the MasterBoard is visible.
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
System.out.println("summoningAngel is false  phase = " + phase);
            if (phase == SUMMON)
            {
                advancePhase();
            }
        }
        else
        {
System.out.println("summoningAngel is true");
        }
    }


    // This is called from MasterBoard after the SummonAngel finishes.
    void finishSummoningAngel()
    {
System.out.println("BattleTurn.finishSummoningAngel");
        if (attacker.summoned())
        {
System.out.println("placeNewChit");
            map.placeNewChit(attacker);
        }

        summoningAngel = false;

        if (phase == SUMMON)
        {
            advancePhase();
        }
    }


    void setupMoveDialog()
    {
System.out.println("setupMoveDialog");
        // If there are no legal moves, move on.
        if (map.highlightMovableChits() < 1)
        {
System.out.println("No legal moves; advancing to strike phase");
            advancePhase();
        }
        else
        {
            removeAll();
            setTitle(getActivePlayer().getName() + " Turn " + turnNumber);
            setLayout(new GridLayout(0, 4));

            add(new Label(getActivePlayer().getName() + " : Move"));

            Button button1 = new Button("Undo Last Move");
            add(button1);
            button1.addActionListener(this);

            Button button2 = new Button("Undo All Moves");
            add(button2);
            button2.addActionListener(this);

            Button button3 = new Button("Done with Moves");
            add(button3);
            button3.addActionListener(this);

            pack();

            // Place this window in the upper right corner.
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(new Point(d.width - getSize().width, 0));
        }
    }


    void setupFightDialog()
    {
System.out.println("setupFightDialog");
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
            setLayout(new GridLayout(0, 2));

            add(new Label(getActivePlayer().getName() +
                ((phase == FIGHT) ? " : Strike" : " : Strikeback")));
            Button button1 = new Button("Done with Strikes");
            add(button1);
            button1.addActionListener(this);

            pack();

            // Place this window in the upper right corner.
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(new Point(d.width - getSize().width, 0));
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
System.out.println("entering advancePhase() with phase " + phase);
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
System.out.println("Calling removeDeadChits");
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
System.out.println("Now turn " + turnNumber);
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
System.out.println("leaving advancePhase() with phase " + phase);
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
            if (map.forcedStrikesRemain() == false)
            {
                map.commitStrikes();
                advancePhase();
            }
        }
    }
}
