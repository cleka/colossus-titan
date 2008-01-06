package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;

import net.sf.colossus.util.KDialog;


/**
 * Class AcquireAngel allows a player to acquire an angel or archangel.
 * @version $Id$
 * @author David Ripton
 */

final class AcquireAngel extends KDialog implements MouseListener,
    WindowListener, ActionListener
{
    private final List<Chit> chits = new ArrayList<Chit>();
    private final List<String> recruits;
    private final Client client;
    private final String markerId;
    private final SaveWindow saveWindow;

    AcquireAngel(JFrame parentFrame, Client client, String markerId,
        List<String> recruits)
    {
        super(parentFrame, client.getPlayerName()
            + ": Acquire Angel in legion " + markerId, false);

        this.client = client;
        this.markerId = markerId;
        this.recruits = recruits;

        addMouseListener(this);
        addWindowListener(this);

        Container contentPane = getContentPane();
        contentPane.setLayout(new FlowLayout());

        setBackground(Color.lightGray);

        Iterator<String> it = recruits.iterator();
        while (it.hasNext())
        {
            String creatureName = it.next();
            Chit chit = new Chit(4 * Scale.get(), creatureName);
            chits.add(chit);
            contentPane.add(chit);
            chit.addMouseListener(this);
        }

        JButton acquireButton = new JButton("Acquire");
        contentPane.add(acquireButton);
        acquireButton.addActionListener(this);

        JButton cancelButton = new JButton("Cancel");
        contentPane.add(cancelButton);
        cancelButton.addActionListener(this);

        pack();
        saveWindow = new SaveWindow(client, "AcquireAngel");
        Point location = saveWindow.loadLocation();
        if (location == null)
        {
            centerOnScreen();
        }
        else
        {
            setLocation(location);
        }
        setVisible(true);
        repaint();
    }

    void cleanup(String angelType)
    {
        client.acquireAngelCallback(markerId, angelType);
        saveWindow.saveLocation(getLocation());
        dispose();
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        int i = chits.indexOf(source);
        if (i != -1)
        {
            cleanup(recruits.get(i));
        }
    }

    @Override
    public void windowClosing(WindowEvent e)
    {
        cleanup(null);
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Cancel"))
        {
            cleanup(null);
        }
        else if (e.getActionCommand().equals("Acquire"))
        {
            if (recruits.size() == 1)
            {
                cleanup(recruits.get(0));
            }
            else
            {
                client.showMessageDialog("Acquire which type?");
            }
        }
    }
}
