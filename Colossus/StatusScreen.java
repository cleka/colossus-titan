import java.awt.*;
import java.awt.event.*;


/**
 * Class StatusScreen displays some information about the current game.
 * @version $Id$
 * @author David Ripton
 */


public class StatusScreen extends Frame implements WindowListener
{
    private Label [] activeLabel;
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

        setLayout(new GridLayout(0, 10));

        // active, player name, tower, color, colors eliminated, legions,
        //     markers, creatures, titan power, score

        add(new Label(""));
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
        activeLabel = new Label[numPlayers];
        elimLabel = new Label[numPlayers];
        legionsLabel = new Label[numPlayers];
        markersLabel = new Label[numPlayers];
        creaturesLabel = new Label[numPlayers];
        titanLabel = new Label[numPlayers];
        scoreLabel = new Label[numPlayers];

        for (int i = 0; i < numPlayers; i++)
        {
            activeLabel[i] = new Label(" ");
            if (game.getActivePlayerNum() == i)
            {
                activeLabel[i] = new Label("*");
            }
            else
            {
                activeLabel[i] = new Label(" ");
            }
            add(activeLabel[i]);
            add(new Label(game.getPlayer(i).getName()));
            add(new Label(
                String.valueOf(100 * game.getPlayer(i).getTower())));
            add(new Label(game.getPlayer(i).getColor()));
            elimLabel[i] = new Label(game.getPlayer(i).getPlayersElim());
            add(elimLabel[i]);
            legionsLabel[i] = new Label(String.valueOf(
                game.getPlayer(i).getNumLegions()));
            add(legionsLabel[i]);
            markersLabel[i] = new Label(String.valueOf(
                game.getPlayer(i).getNumMarkersAvailable()));
            add(markersLabel[i]);
            creaturesLabel[i] = new Label(String.valueOf(
                game.getPlayer(i).getNumCreatures()));
            add(creaturesLabel[i]);
            titanLabel[i] = new Label(String.valueOf(
                game.getPlayer(i).getTitanPower()));
            add(titanLabel[i]);
            scoreLabel[i] = new Label(String.valueOf(
                game.getPlayer(i).getScore()));
            add(scoreLabel[i]);
        }

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


    public void updateStatusScreen()
    {
        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            if (game.getActivePlayerNum() == i)
            {
                activeLabel[i].setText("*");
            }
            else
            {
                activeLabel[i].setText(" ");
            }
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
