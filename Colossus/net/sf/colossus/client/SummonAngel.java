package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import net.sf.colossus.server.Creature;

/**
 * Allows a player to summon an angel or archangel.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */


final class SummonAngel extends JDialog implements MouseListener,
    ActionListener, WindowListener
{
    private String markerId;
    private java.util.List sumChitList;
    private JButton cancelButton;
    private static boolean active;
    private Client client;


    private SummonAngel(Client client, String markerId, String longMarkerName)
    {
        super(client.getBoard().getFrame(), client.getPlayerName() +
            ": Summon Angel into Legion " + longMarkerName, false);

        this.client = client;
        this.markerId = markerId;

        // Count and highlight legions with summonable angels, and put
        // board into a state where those legions can be selected.
        if (client.getBoard().highlightSummonableAngels(markerId) < 1)
        {
            cleanup(null, null);
            return;
        }

        addMouseListener(this);
        addWindowListener(this);

        Container contentPane = getContentPane();
        contentPane.setLayout(new FlowLayout());

        pack();

        setBackground(Color.lightGray);

        int scale = 4 * Scale.get();

        java.util.List summonableList = Creature.getSummonableCreatures();
        java.util.Iterator it = summonableList.iterator();
        sumChitList = new java.util.ArrayList();
        while (it.hasNext())
        {
            Chit tempChit;
            Creature c = (Creature)it.next();
            tempChit = new Chit(scale, c.getName(), this);
            contentPane.add(tempChit);
            tempChit.addMouseListener(this);
            
            // X out chits since no legion is selected.
            tempChit.setDead(true);

            sumChitList.add(tempChit);
        }

        cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic(KeyEvent.VK_C);
        contentPane.add(cancelButton);
        cancelButton.addActionListener(this);

        pack();

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

        setVisible(true);
        repaint();
    }

    static SummonAngel summonAngel(Client client, String markerId,
        String longMarkerName)
    {
        if (!active)
        {
            active = true;
            return new SummonAngel(client, markerId, longMarkerName);
        }
        return null;
    }


    String getMarkerId()
    {
        return markerId;
    }

    private void cleanup(String donorId, String angel)
    {
        client.doSummon(markerId, donorId, angel);
        dispose();
        active = false;
    }


    public void mousePressed(MouseEvent e)
    {
        String donorId = client.getDonorId();
        if (donorId == null)
        {
            return;
        }
        Object source = e.getSource();
        java.util.Iterator it = sumChitList.iterator();
        boolean done = false;
        while (it.hasNext() && !done)
        {
            Chit c = (Chit)it.next();
            if ((source == c) && !(c.isDead()))
            {
                cleanup(donorId, c.getId());
                done = true;
            }
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

    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
        cleanup(null, null);
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

    /** Upstate state of angel and archangel chits to reflect donor */
    void updateChits()
    {
        String donorId = client.getDonorId();
        if (donorId == null)
        {
            return;
        }
        java.util.Iterator it = sumChitList.iterator();
        boolean done = false;
        while (it.hasNext())
        {
            Chit c = (Chit)it.next();
            c.setDead(!client.donorHas(c.getId()));
        }
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Cancel"))
        {
            cleanup(null, null);
        }
    }
}
