package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;


/**
 * Class AcquireAngel allows a player to acquire an angel or archangel.
 * @version $Id$
 * @author David Ripton
 */


final class AcquireAngel extends JDialog implements MouseListener,
    WindowListener
{
    private java.util.List chits = new ArrayList();
    private java.util.List recruits;
    private static boolean active;
    private Client client;
    private String markerId;


    AcquireAngel(JFrame parentFrame, Client client, String markerId, 
        java.util.List recruits)
    {
        super(parentFrame, client.getPlayerName() + 
            ": Acquire Angel in legion " + markerId, false);

        this.client = client;
        this.markerId = markerId;
        this.recruits = recruits;

        addMouseListener(this);
        addWindowListener(this);

        Container contentPane = getContentPane();
        contentPane.setLayout(new FlowLayout());

        pack();
        setBackground(Color.lightGray);

        Iterator it = recruits.iterator();
        while (it.hasNext())
        {
            String creatureName = (String)it.next();
            Chit chit = new Chit(4 * Scale.get(), creatureName, this);
            chits.add(chit);
            contentPane.add(chit);
            chit.addMouseListener(this);
        }

        pack();

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

        setVisible(true);
        repaint();
    }


    void cleanup(String angelType)
    {
        client.acquireAngelCallback(markerId, angelType);
    }


    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        int i = chits.indexOf(source);
        if (i != -1)
        {
            cleanup((String)recruits.get(i));

            // Then exit.
            dispose();
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
        cleanup(null);
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
