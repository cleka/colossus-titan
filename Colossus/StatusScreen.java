import java.awt.*;
import java.awt.event.*;


/**
 * Class StatusScreen displays some information about the current game.
 * @version $Id$
 * @author David Ripton
 */


public class StatusScreen extends Frame implements WindowListener
{
    private Label [] nameLabel;
    private Label [] towerLabel;
    private Label [] colorLabel;
    private Label [] elimLabel;
    private Label [] legionsLabel;
    private Label [] markersLabel;
    private Label [] creaturesLabel;
    private Label [] titanLabel;
    private Label [] scoreLabel;

    private Game game;
    
    // XXX Need to make labels narrower. 

    public StatusScreen(Game game)
    {
        super("Game Status");

        setVisible(false);
        this.game = game;

        setupIcon();

        addWindowListener(this);

        setLayout(new GridLayout(9, 0));

        int numPlayers = game.getNumPlayers();

        nameLabel = new Label[numPlayers];
        towerLabel = new Label[numPlayers];
        colorLabel = new Label[numPlayers];
        elimLabel = new Label[numPlayers];
        legionsLabel = new Label[numPlayers];
        markersLabel = new Label[numPlayers];
        creaturesLabel = new Label[numPlayers];
        titanLabel = new Label[numPlayers];
        scoreLabel = new Label[numPlayers];


        add(new Label("Player"));
        for (int i = 0; i < numPlayers; i++)
        {
            nameLabel[i] = new Label();
            add(nameLabel[i]);
        }

        add(new Label("Tower"));
        for (int i = 0; i < numPlayers; i++)
        {
            towerLabel[i] = new Label();
            add(towerLabel[i]);
        }

        add(new Label("Color"));
        for (int i = 0; i < numPlayers; i++)
        {
            colorLabel[i] = new Label();
            add(colorLabel[i]);
        }

        add(new Label("Elim"));
        for (int i = 0; i < numPlayers; i++)
        {
            elimLabel[i] = new Label();
            add(elimLabel[i]);
        }

        add(new Label("Legions"));
        for (int i = 0; i < numPlayers; i++)
        {
            legionsLabel[i] = new Label();
            add(legionsLabel[i]);
        }

        add(new Label("Markers"));
        for (int i = 0; i < numPlayers; i++)
        {
            markersLabel[i] = new Label();
            add(markersLabel[i]);
        }

        add(new Label("Creatures"));
        for (int i = 0; i < numPlayers; i++)
        {
            creaturesLabel[i] = new Label();
            add(creaturesLabel[i]);
        }

        add(new Label("Titan Size"));
        for (int i = 0; i < numPlayers; i++)
        {
            titanLabel[i] = new Label();
            add(titanLabel[i]);
        }

        add(new Label("Score"));
        for (int i = 0; i < numPlayers; i++)
        {
            scoreLabel[i] = new Label();
            add(scoreLabel[i]);
        }

        updateStatusScreen();

        pack();

        // Move dialog to bottom right of screen.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width - getSize().width,
            d.height - getSize().height));

        setVisible(true);
    }
    
    
    private void setupIcon()
    {
        if (!game.isApplet())
        {
            try
            {
                setIconImage(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource(Creature.colossus.getImageName())));
            }
            catch (NullPointerException e)
            {
                System.out.println(e.toString() + " Could not find " + 
                    Creature.colossus.getImageName());
                dispose();
            }
        }
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
            Color color;
            if (game.getActivePlayerNum() == i)
            {
                color = Color.yellow;
            }
            else if (game.getPlayer(i).isDead())
            {
                color = Color.red;
            }
            else
            {
                color = Color.lightGray;
            }
            setPlayerLabelBackground(i, color);

            nameLabel[i].setText(game.getPlayer(i).getName());
            towerLabel[i].setText(String.valueOf(100 * 
                game.getPlayer(i).getTower()));
            colorLabel[i].setText(game.getPlayer(i).getColor());
            elimLabel[i].setText(game.getPlayer(i).getPlayersElim());
            legionsLabel[i].setText(String.valueOf(
                game.getPlayer(i).getNumLegions()));
            markersLabel[i].setText(String.valueOf(
                game.getPlayer(i).getNumMarkersAvailable()));
            creaturesLabel[i].setText(String.valueOf(
                game.getPlayer(i).getNumCreatures()));
            titanLabel[i].setText(String.valueOf(
                game.getPlayer(i).getTitanPower()));
            scoreLabel[i].setText(String.valueOf(
                game.getPlayer(i).getScore()));
        }

        repaint();
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
        return new Dimension(25 * scale, 15 * scale);
    }

    public Dimension getPreferredSize()
    {
        return getMinimumSize();
    }

}
