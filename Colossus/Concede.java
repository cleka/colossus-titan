import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/**
 * Class Concede allows a player to flee or concede before starting a Battle.
 * @version $Id$
 * @author David Ripton
 */

public final class Concede extends JDialog implements ActionListener
{
    private JFrame parentFrame;
    private boolean flee;
    private Legion ally;
    private Legion enemy;
    private Chit allyMarker;
    private Chit enemyMarker;
    private static Point location;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();
    private static boolean answer;


    private Concede(Client client, JFrame parentFrame, Legion ally,
        Legion enemy, boolean flee)
    {
        super(parentFrame, (flee ? "Flee" : "Concede") + " with Legion " +
            ally.getLongMarkerName() + " in " +
            ally.getCurrentHex().getDescription() + "?", true);

        Container contentPane = getContentPane();
        contentPane.setLayout(gridbag);

        this.parentFrame = parentFrame;
        this.ally = ally;
        this.enemy = enemy;
        this.flee = flee;

        pack();

        setBackground(Color.lightGray);
        setResizable(false);

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int scale = 4 * Scale.get();

        allyMarker = new Marker(scale, ally.getImageName(), this, client);
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        gridbag.setConstraints(allyMarker, constraints);
        contentPane.add(allyMarker);

        Collection critters = ally.getCritters();
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Chit chit = new Chit(scale, critter.getImageName(), this);
            constraints.gridy = 0;
            constraints.gridwidth = 1;
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
        }

        enemyMarker = new Marker(scale, enemy.getImageName(), this, client);
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        gridbag.setConstraints(enemyMarker, constraints);
        contentPane.add(enemyMarker);

        critters = enemy.getCritters();
        it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Chit chit = new Chit(scale, critter.getImageName(), this);
            constraints.gridy = 1;
            constraints.gridwidth = 1;
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
        }

        JButton button1 = new JButton(flee ? "Flee" : "Concede");
        button1.setMnemonic(flee ? KeyEvent.VK_F : KeyEvent.VK_C);
        JButton button2 = new JButton(flee ? "Don't Flee" : "Don't Concede");
        button2.setMnemonic(KeyEvent.VK_D);

        // Attempt to center the buttons.
        int chitWidth = Math.max(ally.getHeight(), enemy.getHeight()) + 1;
        if (chitWidth < 4)
        {
            constraints.gridwidth = 1;
        }
        else
        {
            constraints.gridwidth = 2;
        }
        int leadSpace = (chitWidth - 2 * constraints.gridwidth) / 2;
        if (leadSpace < 0)
        {
            leadSpace = 0;
        }

        constraints.gridy = 2;
        constraints.gridx = leadSpace;
        gridbag.setConstraints(button1, constraints);
        contentPane.add(button1);
        button1.addActionListener(this);
        constraints.gridx = leadSpace + constraints.gridwidth;
        gridbag.setConstraints(button2, constraints);
        contentPane.add(button2);
        button2.addActionListener(this);

        pack();

        // Initially, center the dialog on the screen.  Save the
        // location for future invocations.
        if (location == null)
        {
            location = new Point(d.width / 2 - getSize().width / 2,
                d.height / 2 - getSize().height / 2);
        }
        setLocation(location);

        setVisible(true);
        repaint();
    }


    /** Return true if the player concedes. */
    public static boolean concede(Client client, JFrame parentFrame,
        Legion ally, Legion enemy)
    {
        answer = false;
        new Concede(client, parentFrame, ally, enemy, false);
        return answer;
    }


    /** Return true if the player flees. */
    public static boolean flee(Client client, JFrame parentFrame, Legion ally,
        Legion enemy)
    {
        answer = false;
        new Concede(client, parentFrame, ally, enemy, true);
        return answer;
    }


    public static void saveLocation(Point point)
    {
        location = point;
    }


    public static Point returnLocation()
    {
        return location;
    }


    private void cleanup()
    {
        location = getLocation();
        dispose();
    }


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Flee") ||
            e.getActionCommand().equals("Concede"))
        {
            answer = true;
        }
        else
        {
            answer = false;
        }
        cleanup();
    }

    /*
    public static void main(String [] args)
    {
        JFrame frame = new JFrame("testing Concede");
        int scale = Scale.get();
        frame.setSize(new Dimension(80 * scale, 80 * scale));
        frame.pack();
        frame.setVisible(true);

        Game game = new Game();
        game.addPlayer("Attacker");
        game.initBoard();
        Client client = game.getServer().getClient(0);
        MasterBoard board = client.getBoard();
        MasterHex hex = board.getHexByLabel("130");

        Player player = game.getPlayer(0);
        player.setScore(1400);
        player.setTower(1);
        player.setColor("Red");
        player.initMarkersAvailable();
        String selectedMarkerId = player.selectMarkerId("Rd01");
        Legion attacker = new Legion(selectedMarkerId, null, hex.getLabel(),
            hex.getLabel(), Creature.titan, Creature.colossus,
            Creature.serpent, Creature.archangel, Creature.hydra,
            Creature.giant, Creature.dragon, null, player.getName(), game);
        player.addLegion(attacker);
        client.addMarker(selectedMarkerId);

        game.addPlayer("Defender");
        player = game.getPlayer(1);
        player.setTower(2);
        player.setColor("Blue");
        player.initMarkersAvailable();
        selectedMarkerId = player.selectMarkerId("Bu01");
        Legion defender = new Legion(selectedMarkerId, null, hex.getLabel(),
            hex.getLabel(), Creature.ogre, Creature.centaur,
            Creature.gargoyle, null, null, null, null, null,
            player.getName(), game);
        player.addLegion(defender);
        client.addMarker(selectedMarkerId);

        boolean answer = Concede.flee(client, frame, defender, attacker);
        System.out.println("Flee? " + answer);
        answer = Concede.concede(client, frame, attacker, defender);
        System.out.println("Concede? " + answer);
        System.exit(0);
    }
    */
}
