import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class BattleTurn gets and holds chronological and sequence data for a battle.
 * @version $Id$
 * @author David Ripton
 */

public class BattleTurn extends JDialog implements ActionListener,
    WindowListener
{
    private BattleMap map;
    private Point location;
    private Battle battle;
    private Container contentPane;


    public BattleTurn(BattleMap map, Battle battle)
    {
        super(map.getFrame(), battle.getActivePlayer().getName() + " Turn " +
                battle.getTurnNumber());

        this.map = map;
        this.battle = battle;
        
        setBackground(Color.lightGray);
        addWindowListener(this);
        
        pack();

        // Place this dialog on top of Turn dialog, or at the saved
        // location of the last BattleTurn.
        if (location == null)
        {
            location = Turn.getSavedLocation();
        }
        setLocation(location);

        setResizable(false);
    }


    public void setupRecruitDialog()
    {
        contentPane = getContentPane();
        contentPane.removeAll();
        setTitle(battle.getActivePlayer().getName() + " Turn " + 
            battle.getTurnNumber());
        contentPane.setLayout(new GridLayout(1, 0));
        contentPane.add(new JLabel(battle.getActivePlayer().getName() + 
            " : Recruit"));

        battle.recruitReinforcement();
    }
    
    
    public void setupSummonDialog()
    {
        contentPane = getContentPane();
        contentPane.removeAll();
        setTitle(battle.getActivePlayer().getName() + " Turn " +
            battle.getTurnNumber());
        contentPane.setLayout(new GridLayout(1, 0));
        contentPane.add(new JLabel(battle.getActivePlayer().getName() + 
            " : Summon"));
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
            contentPane = getContentPane();
            contentPane.removeAll();
            setTitle(battle.getActivePlayer().getName() + " Turn " + 
                battle.getTurnNumber());
            contentPane.setLayout(new GridLayout(5, 0));

            contentPane.add(new JLabel(battle.getActivePlayer().getName() +
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
            contentPane = getContentPane();
            contentPane.removeAll();
            contentPane.setLayout(new GridLayout(3, 0));

            contentPane.add(new JLabel(battle.getActivePlayer().getName() +
                ((battle.getPhase() == Battle.FIGHT) ? 
                " : Strike" : " : Strikeback")));

            JButton button1 = new JButton("Concede Battle");
            contentPane.add(button1);
            button1.addActionListener(this);
            
            JButton button2 = new JButton("Done with Strikes");
            contentPane.add(button2);
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


    public Dimension getMinimumSize()
    {
        int scale = MasterBoard.getScale();
        return new Dimension(12 * scale, 12 * scale);
    }

    public Dimension getPreferredSize()
    {
        return getMinimumSize();
    }

}
