package net.sf.colossus.webclient;


import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;

import net.sf.colossus.common.IOptions;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.guiutil.SaveWindow;


public class RecoverGameDialog extends KDialog
{
    private static final Logger LOGGER = Logger.getLogger(RecoverGameDialog.class
        .getName());

    private final SaveWindow saveWindow;
    private final IOptions options;

    JRadioButton latest;
    JRadioButton choose;

    public RecoverGameDialog(Frame owner, IOptions options)
    {
        super(owner, "Recover a game", true);
        LOGGER.finest("Instantiated a RecoverGameDialog window.");

        this.options = options;

        // TODO Auto-generated constructor stub

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        JPanel recoverPanel = new JPanel();
        recoverPanel.setBorder(new TitledBorder("Recover"));
        JLabel niLabel = new JLabel("(not implemented yet)");
        niLabel.setFont(niLabel.getFont().deriveFont(Font.ITALIC));
        recoverPanel.add(niLabel);
        recoverPanel
            .add(new JLabel(
                "On this dialog, one will then be able to choose one of the recently ended games."));
        contentPane.add(recoverPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        Box cancelButtonBar = new Box(BoxLayout.X_AXIS);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                System.out.println("Cancel");
                dispose();
            }
        });
        cancelButton.setPreferredSize(cancelButton.getMinimumSize());
        cancelButtonBar.add(Box.createHorizontalGlue());
        cancelButtonBar.add(cancelButton);
        cancelButtonBar.add(Box.createHorizontalGlue());
        buttonPanel.add(cancelButtonBar);

        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        pack();
        saveWindow = new SaveWindow(this.options, "ResumeDialog");
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

}
