package net.sf.colossus.gui;


import guiutil.KDialog;

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;

import net.sf.colossus.client.Client;
import net.sf.colossus.game.Legion;


/**
 * Class AcquireAngel allows a player to acquire an angel or archangel.
 * @version $Id$
 * @author David Ripton
 */

final class AcquireAngel extends KDialog
{
    private final List<Chit> chits = new ArrayList<Chit>();
    private final Client client;
    private final Legion legion;
    private final SaveWindow saveWindow;

    AcquireAngel(JFrame parentFrame, final Client client, Legion legion,
        final List<String> recruits)
    {
        super(parentFrame, client.getOwningPlayer().getName()
            + ": Acquire Angel in legion " + legion, false);

        this.client = client;
        this.legion = legion;

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                cleanup(null);
            }

        });

        Container contentPane = getContentPane();
        contentPane.setLayout(new FlowLayout());

        setBackground(Color.lightGray);

        Iterator<String> it = recruits.iterator();
        while (it.hasNext())
        {
            final String creatureName = it.next();
            Chit chit = new Chit(4 * Scale.get(), creatureName);
            chits.add(chit);
            contentPane.add(chit);
            chit.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    cleanup(creatureName);
                }
            });
        }

        JButton acquireButton = new JButton("Acquire");
        contentPane.add(acquireButton);
        acquireButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
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
        });

        JButton cancelButton = new JButton("Cancel");
        contentPane.add(cancelButton);
        cancelButton.addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent e)
            {
                cleanup(null);
            }

        });

        pack();
        saveWindow = new SaveWindow(client.getOptions(), "AcquireAngel");
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
        client.acquireAngelCallback(legion, angelType);
        saveWindow.saveLocation(getLocation());
        dispose();
    }
}
