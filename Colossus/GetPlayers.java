import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;


/**
 * Class GetPlayers is a dialog used to enter players' names.
 * @version $Id$ 
 * @author David Ripton
 */


public class GetPlayers extends JDialog implements WindowListener,
    ActionListener
{
    private ArrayList textFields = new ArrayList();
    private Game game;


    public GetPlayers(JFrame parentFrame, Game game)
    {
        super(parentFrame, "Player Setup", true);

        this.game = game;

        setBackground(Color.lightGray);
        pack();

        Container contentPane = getContentPane();

        contentPane.setLayout(new GridLayout(0, 2));

        for (int i = 0; i < 6; i++)
        {
            String s = "Player " + (i + 1) + " Name";
            contentPane.add(new JLabel(s));
            TextField tf = new TextField(20);
            contentPane.add(tf);
            textFields.add(tf);
        }

        JButton button1 = new JButton("OK");
        contentPane.add(button1);
        button1.addActionListener(this);
        JButton button2 = new JButton("Quit");
        contentPane.add(button2);
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
        boolean error = false;
        Set playerNames = new TreeSet();
        int numPlayers = 0;

        Iterator it = textFields.iterator();
        while (it.hasNext())
        {
            TextField tf = (TextField)it.next();
            String text = tf.getText();
            if (text.length() > 0)
            {
                numPlayers++;
                playerNames.add(text);
            }
        }

        // Make sure that there is at least one player, and
        // that each player has a unique name.
        if (numPlayers < 1 || playerNames.size() != numPlayers)
        {
            it = textFields.iterator();
            while (it.hasNext())
            {
                TextField tf = (TextField)it.next();
                tf.setText("");
            }
            return;
        }

        // Data is good; send to game.
        if (game != null)
        {
            it = playerNames.iterator();
            while (it.hasNext())
            {
                String name = (String)it.next();
                game.addPlayer(name);
                Game.logEvent("Added player " + name);
            }
        }

        dispose();
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
        if (game == null)
        {
            System.exit(0);
        }
        super.dispose();
    }


    public Dimension getMinimumSize()
    {
        return new Dimension(300, 300);
    }
    
    public Dimension getPreferredSize()
    {
        return getMinimumSize();
    }


    public static void main(String [] args)
    {
        new GetPlayers(new JFrame(), null);
    }
}
