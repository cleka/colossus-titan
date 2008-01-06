package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.colossus.util.KDialog;


/** 
 *  Allows picking any integer value
 *  @version $Id$
 *  @author David Ripton
 */

public final class PickIntValue extends KDialog implements WindowListener,
    ChangeListener, ActionListener
{
    private int newValue;
    private int oldValue;

    private JSpinner spinner;
    private SpinnerNumberModel model;
    private SaveWindow saveWindow;

    private PickIntValue(JFrame parentFrame, int oldValue, String title,
        int min, int max, int step, IOptions options)
    {
        super(parentFrame, title, true);
        this.newValue = oldValue; // oldValue is also the new unless changed
        this.oldValue = oldValue;

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
        saveWindow = new SaveWindow(options, "PickIntValue");
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

    /** Return the new value if the user accepted it, or oldValue if
     *  user cancelled the dialog. */
    public static int pickIntValue(JFrame parentFrame, int oldValue,
        String title, int min, int max, int step, IOptions options)
    {
        PickIntValue dialog = new PickIntValue(parentFrame, oldValue, title,
            min, max, step, options);
        return dialog.newValue;
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

    @Override
    public void dispose()
    {
        saveWindow.saveLocation(getLocation());
        super.dispose();
    }
}
