package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Container;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;

import net.sf.colossus.util.KDialog;


/**
 * Class ChooseScreen allows a player to choose the screen 
 *   on which to display the secondaries windows.
 * @version $Id$
 * @author Romain Dolbeau
 */

final class ChooseScreen extends KDialog implements ActionListener
{
    private Client client;

    ChooseScreen(JFrame parentFrame, Client client)
    {
        super(parentFrame, "Choose A Screen", true);

        this.client = client;

        Container contentPane = getContentPane();
        contentPane.setLayout(new GridLayout(0, 1));

        setBackground(Color.lightGray);

        GraphicsDevice[] all =
            GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getScreenDevices();

        for (int i = 0; i < all.length; i++)
        {
            JButton sb = new JButton("Screen: " + all[i].getClass().getName() +
                " " + all[i].getIDstring());

            contentPane.add(sb);
            sb.addActionListener(this);
        }

        pack();
        centerOnScreen();
        setVisible(true);
        repaint();
    }

    public void actionPerformed(ActionEvent e)
    {
        String fullName = e.getActionCommand();

        GraphicsDevice[] all = GraphicsEnvironment
            .getLocalGraphicsEnvironment().getScreenDevices();
        GraphicsDevice chosen = null;

        for (int i = 0; i < all.length; i++)
        {
            if (fullName.equals("Screen: " + all[i].getClass().getName() +
                " " + all[i].getIDstring()))
            {
                chosen = all[i];
            }
        }
        setVisible(false);
        client.setChosenDevice(chosen);
        dispose();
    }
}
