import java.awt.*;
import java.awt.event.*;

/**
 * Class BattleTurn gets and holds chronological and sequence data for a battle.
 * @version $Id$
 * @author David Ripton
 */

public class BattleTurn extends Dialog implements ActionListener,
    WindowListener
{
    private BattleMap map;
    private static Point location;
    private Battle battle;


    public BattleTurn(BattleMap map, Battle battle)
    {
        super(map, battle.getActivePlayer().getName() + " Turn " + 
                battle.getTurnNumber());

        this.map = map;
        this.battle = battle;
        
        setBackground(Color.lightGray);
        
        pack();

        // Place this window in the upper left corner, or at the saved
        // location of the last BattleTurn. 
        if (location == null)
        {
            location = new Point(0, 0);
        }
        setLocation(location);
    }


    public void setupRecruitDialog()
    {
        removeAll();
        setTitle(battle.getActivePlayer().getName() + " Turn " + 
            battle.getTurnNumber());
        setLayout(new GridLayout(0, 1));
        add(new Label(battle.getActivePlayer().getName() + " : Recruit"));

        battle.recruitReinforcement();
    }
    
    
    public void setupSummonDialog()
    {
        removeAll();
        setTitle(battle.getActivePlayer().getName() + " Turn " +
            battle.getTurnNumber());
        setLayout(new GridLayout(0, 1));
        add(new Label(battle.getActivePlayer().getName() + " : Summon"));
        pack();
    }


    public void setupMoveDialog()
    {
        // If there are no legal moves, move on.
        if (battle.highlightMovableChits() < 1)
        {
            battle.advancePhase();
        }
        else
        {
            removeAll();
            setTitle(battle.getActivePlayer().getName() + " Turn " + 
                battle.getTurnNumber());
            setLayout(new GridLayout(0, 5));

            add(new Label(battle.getActivePlayer().getName() + " : Move"));

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
            setVisible(true);
        }
    }


    public void setupFightDialog()
    {
        battle.applyDriftDamage();

        // If there are no possible strikes, move on.
        if (battle.highlightChitsWithTargets() < 1)
        {
            battle.advancePhase();
        }
        else
        {
            removeAll();
            setLayout(new GridLayout(0, 3));

            add(new Label(battle.getActivePlayer().getName() + 
                ((battle.getPhase() == Battle.FIGHT) ? 
                " : Strike" : " : Strikeback")));

            Button button1 = new Button("Concede Battle");
            add(button1);
            button1.addActionListener(this);
            
            Button button2 = new Button("Done with Strikes");
            add(button2);
            button2.addActionListener(this);

            pack();
        }
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
            battle.undoLastMove();
        }

        else if (e.getActionCommand().equals("Undo All Moves"))
        {
            battle.undoAllMoves();
        }

        else if (e.getActionCommand().equals("Done with Moves"))
        {
            battle.doneWithMoves();
        }

        else if (e.getActionCommand().equals("Done with Strikes"))
        {
            battle.doneWithStrikes();
        }

        else if (e.getActionCommand().equals("Concede Battle"))
        {
            // XXX: Since the UI is shared between players for the
            // hotseat game, we will assume that the active player
            // is the one conceding.  This will change later.
            battle.tryToConcede();
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
