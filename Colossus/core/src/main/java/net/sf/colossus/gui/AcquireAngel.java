package net.sf.colossus.gui;


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

import net.sf.colossus.game.Legion;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.guiutil.SaveWindow;
import net.sf.colossus.variant.CreatureType;


/**
 * Class AcquireAngel allows a player to acquire an angel or archangel.
 *
 * @author David Ripton
 */

final class AcquireAngel extends KDialog
{
    private final List<Chit> chits = new ArrayList<Chit>();
    private final ClientGUI gui;
    private final Legion legion;
    private final SaveWindow saveWindow;

    AcquireAngel(JFrame parentFrame, final ClientGUI clientGui, Legion legion,
        final List<CreatureType> recruits)
    {
        super(parentFrame, clientGui.getOwningPlayer().getName()
            + ": Acquire Angel in legion " + legion, false);

        this.gui = clientGui;
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

        Iterator<CreatureType> it = recruits.iterator();
        while (it.hasNext())
        {
            final CreatureType creature = it.next();
            Chit chit = Chit.newCreatureChit(4 * Scale.get(), creature);
            chits.add(chit);
            contentPane.add(chit);
            chit.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    cleanup(creature);
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
                    gui.showMessageDialogAndWait("Acquire which type?");
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
        saveWindow = new SaveWindow(gui.getOptions(), "AcquireAngel");
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

    void cleanup(CreatureType angelType)
    {
        gui.getCallbackHandler().acquireAngelCallback(legion, angelType);
        saveWindow.saveLocation(getLocation());
        dispose();
    }
}
