import java.awt.*;
import java.awt.event.*;


/**
 * Class GetPlayers is a dialog used to enter players' names.
 * @version $Id$ 
 * @author David Ripton
 */


public class GetPlayers extends Dialog implements WindowListener, ActionListener
{
    private TextField [] tf = new TextField[6];
    private Game game;


    public GetPlayers(Frame parentFrame, Game game)
    {
        super(parentFrame, "Player Setup", true);

        this.game = game;

        setBackground(Color.lightGray);
        pack();

        setLayout(new GridLayout(0, 2));

        for (int i = 0; i < 6; i++)
        {
            String s = "Player " + (i + 1) + " Name";
            add(new Label(s));
            tf[i] = new TextField(20);
            add(tf[i]);
        }

        Button button1 = new Button("OK");
        add(button1);
        button1.addActionListener(this);
        Button button2 = new Button("Quit");
        add(button2);
        button2.addActionListener(this);

        pack();
        
        // Center dialog on screen.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, d.height / 2
                     - getSize().height / 2));

        addWindowListener(this);
        setVisible(true);
    }
    
    
    private void validateInputs()
    {
        String [] playerNames = new String[6];
        boolean error = false;
        String [] s = new String[6];

        for (int i = 0; i < 6; i++)
        {
            s[i] = tf[i].getText();
        }

        // Sort in reverse order so that empties go last.
        sortStrings(s);

        // Make sure each player has a unique name.
        int numPlayers = 0;
        for (int i = 0; i < 6; i++)
        {
            if (s[i].length() > 0)
            {
                if (i > 0 && s[i].equals(s[i - 1]))
                {
                    error = true;
                }
                else
                {
                    playerNames[numPlayers] = s[i];
                    numPlayers++;
                }
            }
        }

        if (error || numPlayers == 0)
        {
            for (int i = 0; i < 6; i++)
            {
                tf[i].setText("");
            }
            return;
        }

        // Data is good; send to game.
        if (game != null)
        {
            game.setNumPlayers(numPlayers);
            for (int i = 0; i < numPlayers; i++)
            {
                // Display player numbers as if they started at 1 not 0.
                Game.logEvent("Player " + (i + 1) + " is " + playerNames[i]);
                game.setPlayerName(i, playerNames[i]);
            }
        }

        dispose();
    }


    // Sort string array in reverse order
    private void sortStrings(String [] s)
    {
        for (int i = 0; i < s.length - 1; i++)
        {
            for (int j = i + 1; j < s.length; j++)
            {
                if (s[i].compareTo(s[j]) > 0)
                {
                    String temp = s[i];
                    s[i] = s[j];
                    s[j] = temp;
                }
            }
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
        if (game != null)
        {
            game.dispose();
        }
        dispose();
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

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Quit"))
        {
            if (game != null)
            {
                game.dispose();
            }
            dispose();
        }
        else if (e.getActionCommand().equals("OK"))
        {
            validateInputs();
        }
    }


    public void dispose()
    {
        super.dispose();
        if (game == null)
        {
            System.exit(0);
        }
    }


    public static void main(String [] args)
    {
        new GetPlayers(new Frame(), null);
    }
}
