package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import net.sf.colossus.util.KDialog;

/** 
 *  Allows picking any integer value
 *  @version $Id$
 *  @author David Ripton
 */

final class PickIntValue extends KDialog implements WindowListener,
    ChangeListener, ActionListener
{
    private static int newValue;
    private static int oldValue;

    // A JSpinner would be better, but is not supported until JDK 1.4.
    private JSlider slider;


    private PickIntValue(JFrame parentFrame, int oldValue, String title,
        int min, int max)
    {
        super(parentFrame, title, true);

        this.oldValue = oldValue;

        setBackground(Color.lightGray);

        addWindowListener(this);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        slider = new JSlider(JSlider.HORIZONTAL, min, max, oldValue);
        slider.setMajorTickSpacing((max - min) / 4);
        slider.setMinorTickSpacing((max - min) / 16);
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

    /** Return the new value if the user accepted it, or oldValue if
     *  user cancelled the dialog. */
    static int pickIntValue(JFrame parentFrame, int oldValue, String title,
        int min, int max)
    {
        new PickIntValue(parentFrame, oldValue, title, min, max);
        return newValue;
    }


    public void stateChanged(ChangeEvent e)
    {
        newValue = slider.getValue();
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Accept"))
        {
            dispose();
        }
        else if (e.getActionCommand().equals("Cancel"))
        {
            newValue = oldValue;
            dispose();
        }
    }
}
