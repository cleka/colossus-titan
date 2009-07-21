package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Container;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;

import net.sf.colossus.guiutil.KDialog;


/**
 * Class ChooseScreen allows a player to choose the screen
 *   on which to display the secondaries windows.
 *
 * @author Romain Dolbeau
 */
@SuppressWarnings("serial")
final class ChooseScreen extends KDialog implements ActionListener
{
    private final ClientGUI gui;

    ChooseScreen(JFrame parentFrame, ClientGUI clientGui)
    {
        super(parentFrame, "Choose A Screen", true);

        this.gui = clientGui;

        Container contentPane = getContentPane();
        contentPane.setLayout(new GridLayout(0, 1));

        setBackground(Color.lightGray);

        GraphicsDevice[] all = GraphicsEnvironment
            .getLocalGraphicsEnvironment().getScreenDevices();

        for (GraphicsDevice device : all)
        {
            JButton sb = new JButton("Screen: " + device.getClass().getName()
                + " " + device.getIDstring());

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

        for (GraphicsDevice device : all)
        {
            if (fullName.equals("Screen: " + device.getClass().getName() + " "
                + device.getIDstring()))
            {
                chosen = device;
            }
        }
        setVisible(false);
        gui.setChosenDevice(chosen);
        dispose();
    }
}
