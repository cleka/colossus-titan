package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import net.sf.colossus.server.Options;
import net.sf.colossus.util.KDialog;


/** 
 *  Allows picking a new AI delay.
 *  @version $Id$ 
 *  @author David Ripton
 */

final class PickDelay extends KDialog implements WindowListener,
    ChangeListener, ActionListener
{
    private static int newDelay;
    // A JSpinner would be better, but is not supported until JDK 1.4.
    private JSlider slider;
    private Client client;


    private PickDelay(JFrame parentFrame, Client client, int oldDelay)
    {
        super(parentFrame, "Set AI Delay in ms", false);

        this.client = client;

        setBackground(Color.lightGray);
        addWindowListener(this);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        if (oldDelay < 0)
        {
            oldDelay = 0;
        }
        slider = new JSlider(JSlider.HORIZONTAL, 0, 3000, oldDelay);
        slider.setMajorTickSpacing(1000);
        slider.setMinorTickSpacing(250);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        contentPane.add(slider);
        slider.addChangeListener(this);

        // Need another BoxLayout to place buttons horizontally.
        Box buttonBar = new Box(BoxLayout.X_AXIS);
        contentPane.add(buttonBar);

        JButton accept = new JButton("Accept");
        accept.addActionListener(this);
        buttonBar.add(accept);

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(this);
        buttonBar.add(cancel);

        pack();
        centerOnScreen();
        setVisible(true);
        repaint();
    }

    static void pickDelay(JFrame parentFrame, Client client, int oldDelay)
    {
        new PickDelay(parentFrame, client, oldDelay);
    }


    public void stateChanged(ChangeEvent e)
    {
        newDelay = slider.getValue();
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Accept"))
        {
            dispose();
            if (client != null)
            {
                client.setIntOption(Options.aiDelay, newDelay);
            }
        }
        else if (e.getActionCommand().equals("Cancel"))
        {
            newDelay = -1;
            dispose();
            if (client != null)
            {
                client.setIntOption(Options.aiDelay, newDelay);
            }
        }
    }
}
