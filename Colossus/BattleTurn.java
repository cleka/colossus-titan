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


    BattleTurn(Frame parentFrame, BattleMap map, Legion attacker, Legion
        defender)
    {
        super(parentFrame, defender.getPlayer().getName() + " Turn 1");

        this.parentFrame = parentFrame;
        this.map = map;
        this.attacker = attacker;
        this.defender = defender;
        activeLegion = defender;

        setupMoveDialog();

        // This is necessary to prevent a visible resize.
        pack();

        setVisible(true);
    }


    void setupRecruitDialog()
    {
        setTitle(getActivePlayer().getName() + " Turn " + turnNumber);

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
        setTitle(getActivePlayer().getName() + " Turn " + turnNumber);

        if (map.getSummonState() == BattleMap.FIRST_BLOOD)
        {
            if (attacker.getHeight() < 7 &&
                attacker.getPlayer().canSummonAngel())
            {
                summonAngel = new SummonAngel(map.getBoard(), attacker);
            }
            else
            {
System.out.println("Couldn't summon angel  height is " + attacker.getHeight());
            }
        }
        // XXX: Need a way to placeNewChit() that triggers when the summon
        // finishes.

        advancePhase();
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
            setLayout(new GridLayout(0, 4));
            setTitle(getActivePlayer().getName() + " Turn " + turnNumber);

            add(new Label(activeLegion.getPlayer().getName() + " : Move"));

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
            setTitle(getActivePlayer().getName() + " Turn " + turnNumber);

            add(new Label(activeLegion.getPlayer().getName() +
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
            if (attacker.getHeight() < 1 || defender.getHeight() < 1)
            {
                return;
            }

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
                        // This is the attacker's titan stack, so the defender 
                        // gets his markers plus half points for his unengaged 
                        // legions.
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
