package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import net.sf.colossus.util.KDialog;

/**
 * Class AcquireAngel allows a player to acquire an angel or archangel.
 * @version $Id$
 * @author David Ripton
 */


final class AcquireAngel extends KDialog implements MouseListener,
    WindowListener, ActionListener
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

        JButton acquireButton = new JButton("Acquire");
        contentPane.add(acquireButton);
        acquireButton.addActionListener(this);

        JButton cancelButton = new JButton("Cancel");
        contentPane.add(cancelButton);
        cancelButton.addActionListener(this);

        pack();
        centerOnScreen();
        setVisible(true);
        repaint();
    }


    void cleanup(String angelType)
    {
        client.acquireAngelCallback(markerId, angelType);
        dispose();
    }


    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        int i = chits.indexOf(source);
        if (i != -1)
        {
            cleanup((String)recruits.get(i));
        }
    }


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
                cleanup((String)recruits.get(0));
            }
            else
            {
                client.showMessageDialog("Acquire which type?");
            }
        }
    }
}
