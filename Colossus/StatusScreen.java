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
    
    
    public StatusScreen(Game game)
    {
        super("Game Status");

        setVisible(false);
        this.game = game;

        setupIcon();

        setLayout(new GridLayout(0, 9));

        // active, player name, tower, color, colors eliminated, legions,
        //     markers, creatures, titan power, score

        add(new Label("Player "));
        add(new Label("Tower "));
        add(new Label("Color "));
        add(new Label("Elim "));
        add(new Label("Legions "));
        add(new Label("Markers "));
        add(new Label("Creatures "));
        add(new Label("Titan Power "));
        add(new Label("Score"));

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

        for (int i = 0; i < numPlayers; i++)
        {
            nameLabel[i] = new Label();
            add(nameLabel[i]);

            towerLabel[i] = new Label();
            add(towerLabel[i]);

            colorLabel[i] = new Label();
            add(colorLabel[i]);

            elimLabel[i] = new Label();
            add(elimLabel[i]);

            legionsLabel[i] = new Label();
            add(legionsLabel[i]);

            markersLabel[i] = new Label();
            add(markersLabel[i]);

            creaturesLabel[i] = new Label();
            add(creaturesLabel[i]);

            titanLabel[i] = new Label();
            add(titanLabel[i]);

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
}
