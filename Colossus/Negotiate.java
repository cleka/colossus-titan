import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/**
 * Class Negotiate allows two players to settle an engagement.
 * @version $Id$
 * @author David Ripton
 */

public final class Negotiate extends JDialog implements MouseListener,
    ActionListener
{
    private Legion attacker;
    private Legion defender;
    private ArrayList attackerChits = new ArrayList();
    private ArrayList defenderChits = new ArrayList();
    private Marker attackerMarker;
    private Marker defenderMarker;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();
    private Client client;
    private static NegotiationResults results;


    private Negotiate(Client client, Legion attacker, Legion defender)
    {
        super(client.getBoard().getFrame(), attacker.getLongMarkerName() +
            " Negotiates with " + defender.getLongMarkerName(), true);

        Container contentPane = getContentPane();

        contentPane.setLayout(gridbag);

        this.client = client;
        this.attacker = attacker;
        this.defender = defender;

        pack();
        setBackground(Color.lightGray);
        setResizable(false);

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();

        addMouseListener(this);

        Game game = attacker.getGame();
        int scale = 4 * Scale.get();

        attackerMarker = new Marker(scale, attacker.getImageName(),
            this, client);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        gridbag.setConstraints(attackerMarker, constraints);
        contentPane.add(attackerMarker);

        Collection critters = attacker.getCritters();
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Chit chit = new Chit(scale, critter.getImageName(), this);
            attackerChits.add(chit);
            constraints.gridx = GridBagConstraints.RELATIVE;
            constraints.gridy = 0;
            constraints.gridwidth = 1;
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
            chit.addMouseListener(this);
        }

        defenderMarker = new Marker(scale, defender.getImageName(),
            this, client);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        gridbag.setConstraints(defenderMarker, constraints);
        contentPane.add(defenderMarker);

        critters = defender.getCritters();
        it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Chit chit = new Chit(scale, critter.getImageName(), this);
            defenderChits.add(chit);
            constraints.gridx = GridBagConstraints.RELATIVE;
            constraints.gridy = 1;
            constraints.gridwidth = 1;
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
            chit.addMouseListener(this);
        }

        JButton button1 = new JButton("Offer");
        button1.setMnemonic(KeyEvent.VK_O);
        JButton button2 = new JButton("Fight");
        button2.setMnemonic(KeyEvent.VK_F);

        // Attempt to center the buttons.
        int chitWidth = Math.max(attacker.getHeight(),
            defender.getHeight()) + 1;
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

        // Use the same location as the preceding Concede dialog.
        Point location = Concede.returnLocation();
        if (location == null)
        {
            location = new Point(d.width / 2 - getSize().width / 2,
                d.height / 2 - getSize().height / 2);
        }
        setLocation(location);

        setVisible(true);
        repaint();
    }


    /** Display a dialog allowing one player to offer a settlement to
     *  an engagement.  Return a NegotiationResults.
     */
    public static NegotiationResults negotiate(Client client, Legion attacker,
        Legion defender)
    {
        new Negotiate(client, attacker, defender);
        return results;
    }


    public void cleanup()
    {
        Concede.saveLocation(getLocation());
        dispose();
    }


    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        if (attackerChits.contains(source) || defenderChits.contains(source))
        {
            Chit chit = (Chit)source;
            chit.toggleDead();
            chit.repaint();
        }
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mouseReleased(MouseEvent e)
    {
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Offer"))
        {
            // Count remaining chits.
            boolean attackersLeft = false;
            Iterator it = attackerChits.iterator();
            while (it.hasNext())
            {
                Chit chit = (Chit)it.next();
                if (!chit.isDead())
                {
                    attackersLeft = true;
                    break;
                }
            }

            boolean defendersLeft = false;
            it = defenderChits.iterator();
            while (it.hasNext())
            {
                Chit chit = (Chit)it.next();
                if (!chit.isDead())
                {
                    defendersLeft = true;
                    break;
                }
            }

            // Ensure that at least one legion is completely eliminated.
            if (attackersLeft && defendersLeft)
            {
                client.showMessageDialog(
                    "At least one legion must be eliminated.");
                return;
            }

            if (!attackersLeft && !defendersLeft)
            {
                // Mutual destruction.
                results = new NegotiationResults(attacker.getMarkerId(),
                    defender.getMarkerId(), false, true, null, null);
            }

            // If this is not a mutual elimination, figure out how many
            // points the victor receives.
            else
            {
                Legion winner;
                Legion loser;
                ArrayList winnerChits;

                if (!defendersLeft)
                {
                    winner = attacker;
                    loser = defender;
                    winnerChits = attackerChits;
                }
                else
                {
                    winner = defender;
                    loser = attacker;
                    winnerChits = defenderChits;
                }

                // Ensure that the winning legion doesn't contain a dead
                // Titan.
                it = winnerChits.iterator();
                while (it.hasNext())
                {
                    Chit chit = (Chit)it.next();
                    if (chit.isDead() && chit.getId().startsWith("Titan"))
                    {
                        client.showMessageDialog(
                            "Titan cannot die unless his whole stack dies.");
                        return;
                    }
                }

                // Remove all dead creatures from the winning legion.
                Set winnerLosses = new HashSet();
                it = winnerChits.iterator();
                while (it.hasNext())
                {
                    Chit chit = (Chit)it.next();
                    if (chit.isDead())
                    {
                        String name = chit.getId();
                        if (name.startsWith("Titan"))
                        {
                            name = "Titan";
                        }
                        winnerLosses.add(Creature.getCreatureByName(name));
                    }
                }
                results = new NegotiationResults(attacker.getMarkerId(),
                    defender.getMarkerId(), false, false,
                    winner.getMarkerId(), winnerLosses);
            }

            // Exit this dialog.
            cleanup();
        }

        else if (e.getActionCommand().equals("Fight"))
        {
            results = new NegotiationResults(attacker.getMarkerId(),
                defender.getMarkerId(), true, false, null, null);

            // Exit this dialog.
            cleanup();
        }
    }

    /*
    public static void main(String [] args)
    {
        JFrame frame = new JFrame("testing Negotiate");
        int scale = Scale.get();
        frame.setSize(new Dimension(80 * scale, 80 * scale));
        frame.pack();
        frame.setVisible(true);

        Game game = new Game();
        game.addPlayer("Attacker");
        game.addPlayer("Defender");
        game.initBoard();
        Client client = game.getServer().getClient(0);

        MasterHex hex = MasterBoard.getHexByLabel("130");

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

        Negotiate.negotiate(client, attacker, defender);
        System.exit(0);
    }
    */
}
