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

public final class PickIntValue extends KDialog implements WindowListener,
            ChangeListener, ActionListener
{
    private static int newValue;
    private static int oldValue;

    private JSpinner spinner;
    private SpinnerNumberModel model;

    private PickIntValue(JFrame parentFrame, int oldValue, String title,
            int min, int max, int step)
    {
        super(parentFrame, title, true);

        this.oldValue = oldValue;
        this.newValue = oldValue;

        setBackground(Color.lightGray);

        addWindowListener(this);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        model = new SpinnerNumberModel(oldValue, min, max, step);
        spinner = new JSpinner(model);
        contentPane.add(spinner);
        spinner.addChangeListener(this);

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
    public static int pickIntValue(JFrame parentFrame, int oldValue,
            String title, int min, int max, int step)
    {
        new PickIntValue(parentFrame, oldValue, title, min, max, step);
        return newValue;
    }

    public void stateChanged(ChangeEvent e)
    {
        newValue = ((Integer)spinner.getValue()).intValue();
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
