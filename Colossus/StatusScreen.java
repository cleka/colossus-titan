import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
 * Class StatusScreen displays some information about the current game.
 * @version $Id$
 * @author David Ripton
 */


public final class StatusScreen extends JDialog implements WindowListener
{
    private JLabel [] nameLabel;
    private JLabel [] towerLabel;
    private JLabel [] colorLabel;
    private JLabel [] elimLabel;
    private JLabel [] legionsLabel;
    private JLabel [] markersLabel;
    private JLabel [] creaturesLabel;
    private JLabel [] titanLabel;
    private JLabel [] scoreLabel;

    private Client client;
    private Game game;

    private static Point location;
    private static Dimension size;


    // TODO Should not take a direct Game reference, since that may contain
    // privileged information.
    public StatusScreen(JFrame frame, Client client, Game game)
    {
        super(frame, "Game Status");

        setVisible(false);
        this.client = client;
        this.game = game;

        addWindowListener(this);

        Container contentPane = getContentPane();
        contentPane.setLayout(new GridLayout(9, 0));

        int numPlayers = game.getNumPlayers();

        nameLabel = new JLabel[numPlayers];
        towerLabel = new JLabel[numPlayers];
        colorLabel = new JLabel[numPlayers];
        elimLabel = new JLabel[numPlayers];
        legionsLabel = new JLabel[numPlayers];
        markersLabel = new JLabel[numPlayers];
        creaturesLabel = new JLabel[numPlayers];
        titanLabel = new JLabel[numPlayers];
        scoreLabel = new JLabel[numPlayers];

        contentPane.add(new JLabel("Player"));
        for (int i = 0; i < numPlayers; i++)
        {
            nameLabel[i] = new JLabel();
            nameLabel[i].setOpaque(true);
            contentPane.add(nameLabel[i]);
        }

        contentPane.add(new JLabel("Tower"));
        for (int i = 0; i < numPlayers; i++)
        {
            towerLabel[i] = new JLabel();
            towerLabel[i].setOpaque(true);
            contentPane.add(towerLabel[i]);
        }

        contentPane.add(new JLabel("Color"));
        for (int i = 0; i < numPlayers; i++)
        {
            colorLabel[i] = new JLabel();
            colorLabel[i].setOpaque(true);
            contentPane.add(colorLabel[i]);
        }

        contentPane.add(new JLabel("Elim"));
        for (int i = 0; i < numPlayers; i++)
        {
            elimLabel[i] = new JLabel();
            elimLabel[i].setOpaque(true);
            contentPane.add(elimLabel[i]);
        }

        contentPane.add(new JLabel("Legions"));
        for (int i = 0; i < numPlayers; i++)
        {
            legionsLabel[i] = new JLabel();
            legionsLabel[i].setOpaque(true);
            contentPane.add(legionsLabel[i]);
        }

        contentPane.add(new JLabel("Markers"));
        for (int i = 0; i < numPlayers; i++)
        {
            markersLabel[i] = new JLabel();
            markersLabel[i].setOpaque(true);
            contentPane.add(markersLabel[i]);
        }

        contentPane.add(new JLabel("Creatures"));
        for (int i = 0; i < numPlayers; i++)
        {
            creaturesLabel[i] = new JLabel();
            creaturesLabel[i].setOpaque(true);
            contentPane.add(creaturesLabel[i]);
        }

        contentPane.add(new JLabel("Titan Size"));
        for (int i = 0; i < numPlayers; i++)
        {
            titanLabel[i] = new JLabel();
            titanLabel[i].setOpaque(true);
            contentPane.add(titanLabel[i]);
        }

        contentPane.add(new JLabel("Score"));
        for (int i = 0; i < numPlayers; i++)
        {
            scoreLabel[i] = new JLabel();
            scoreLabel[i].setOpaque(true);
            contentPane.add(scoreLabel[i]);
        }

        updateStatusScreen();

        pack();

        if (size != null)
        {
            setSize(size);
        }

        if (location == null)
        {
            // Place dialog at bottom right of screen.
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            location = new Point(d.width - getSize().width,
                d.height - getSize().height);
        }
        setLocation(location);

        setVisible(true);
    }


    private void setPlayerLabelBackground(int i, Color color)
    {
        if (nameLabel[i].getBackground() != color)
        {
            nameLabel[i].setBackground(color);
            towerLabel[i].setBackground(color);
            colorLabel[i].setBackground(color);
            elimLabel[i].setBackground(color);
            legionsLabel[i].setBackground(color);
            markersLabel[i].setBackground(color);
            creaturesLabel[i].setBackground(color);
            titanLabel[i].setBackground(color);
            scoreLabel[i].setBackground(color);
        }
    }


    public void updateStatusScreen()
    {
        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            Player player = game.getPlayer(i);
            Color color;
            if (player.isDead())
            {
                color = Color.red;
            }
            else if (game.getActivePlayerNum() == i)
            {
                color = Color.yellow;
            }
            else
            {
                color = Color.lightGray;
            }
            setPlayerLabelBackground(i, color);

            nameLabel[i].setText(player.getName());
            towerLabel[i].setText(String.valueOf(100 * player.getTower()));
            colorLabel[i].setText(player.getColor());
            elimLabel[i].setText(player.getPlayersElim());
            legionsLabel[i].setText(String.valueOf(player.getNumLegions()));
            markersLabel[i].setText(String.valueOf(
                player.getNumMarkersAvailable()));
            creaturesLabel[i].setText(String.valueOf(
                player.getNumCreatures()));
            titanLabel[i].setText(String.valueOf(player.getTitanPower()));
            scoreLabel[i].setText(String.valueOf(player.getScore()));
        }

        repaint();
    }


    public void dispose()
    {
        location = getLocation();
        size = getSize();
        super.dispose();
    }


    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
        client.setOption(Options.showStatusScreen, false);
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
        int scale = Scale.get();
        return new Dimension(25 * scale, 15 * scale);
    }

    public Dimension getPreferredSize()
    {
        return getMinimumSize();
    }

    public void rescale()
    {
        int scale = Scale.get();
        setSize(25 * scale, 15 * scale);
        pack();
    }
}
