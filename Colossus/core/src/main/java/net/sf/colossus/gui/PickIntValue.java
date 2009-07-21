package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.colossus.common.IOptions;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.guiutil.SaveWindow;


/**
 * Allows picking any integer value
 *
 * @author David Ripton
 */
public final class PickIntValue extends KDialog
{
    private int newValue;

    private final JSpinner spinner;
    private final SpinnerNumberModel model;
    private final SaveWindow saveWindow;

    private PickIntValue(JFrame parentFrame, final int oldValue, String title,
        int min, int max, int step, IOptions options)
    {
        super(parentFrame, title, true);
        this.newValue = oldValue; // oldValue is also the new unless changed

        setBackground(Color.lightGray);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        model = new SpinnerNumberModel(oldValue, min, max, step);
        spinner = new JSpinner(model);
        contentPane.add(spinner);
        spinner.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                newValue = ((Integer)spinner.getValue()).intValue();
            }
        });

        // Need another BoxLayout to place buttons horizontally.
        Box buttonBar = new Box(BoxLayout.X_AXIS);
        contentPane.add(buttonBar);

        JButton accept = new JButton("Accept");
        accept.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });
        buttonBar.add(accept);

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                newValue = oldValue;
                dispose();
            }
        });
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
}
