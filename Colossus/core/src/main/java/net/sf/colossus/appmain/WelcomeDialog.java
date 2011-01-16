package net.sf.colossus.appmain;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import net.sf.colossus.common.Options;
import net.sf.colossus.guiutil.KFrame;
import net.sf.colossus.util.ErrorUtils;


/**
 * Provides a simple JTextArea to display some welcome message.
 * Right now, to notify about the Public Game Server.
 *
 * @author Clemens Katzer
 */
public final class WelcomeDialog extends KFrame
{
    private final static String title = "Welcome to this new Colossus Release!";

    private final static String CURRENT_WELCOME_KEY = "0.12.0";

    private final static String baseUrl = "http://sourceforge.net/projects/colossus/files";
    private final static String colossusReleaseNoteUrl = baseUrl
        + "/Release-0.12.0-beta1/Release-Note-0.12.0-beta1.html/download";

    private static WelcomeDialog showWelcome = null;


    WelcomeDialog()
    {
        super(title);

        String text = "\n"
            + "  Welcome to this new Colossus release 0.12.0-beta1!"
            + "\n\n"
            + "  New features:\n"
            + "  - New variant Dino-Titan added\n"
            + "  - New option to disable Warlock recruiting on the first turn\n"
            + "  - A next key ('n') in the move, muster and split phases to move to the next legion that requires action\n"
            + "\n"
            + "  Plus the usual amount of fixes / stability improvements."
            + "\n\n"
            + "  For a more detailed list of changes see the ReleaseNote document:\n      "
            + colossusReleaseNoteUrl
            + "\n\n"
            + "  Use button below to copy the URL to your clipboard to paste it "
            + "into your browser.\n\n"
            + "  This \"Welcome Dialog\" will only be shown once for every "
            + "new release;\n"
            + "  (you can find this welcome message also from the "
            + "MasterBoard Help menu).\n";

        JTextArea contentPanel = new JTextArea(text, 20, 60);
        contentPanel.setEditable(false);

        setLayout(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);

        Box buttonBox = new Box(BoxLayout.X_AXIS);
        JButton copyButton = new JButton("Copy URL to clipboard");
        copyButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ErrorUtils.copyToClipboard(colossusReleaseNoteUrl);

                JOptionPane.showMessageDialog(WelcomeDialog.this,
                    "URL has been copied to your clipboard.", "URL copied!",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });


        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(copyButton);
        buttonBox.add(Box.createRigidArea(new Dimension(10, 10)));
        buttonBox.add(closeButton);
        buttonBox.add(Box.createHorizontalGlue());

        buttonBox.setAlignmentX(CENTER_ALIGNMENT);

        JPanel buttonArea = new JPanel();
        buttonArea.setMinimumSize(new Dimension(100, 50));
        buttonArea.add(Box.createVerticalGlue());
        buttonArea.add(Box.createRigidArea(new Dimension(10, 10)));
        buttonArea.add(buttonBox);
        buttonArea.add(Box.createRigidArea(new Dimension(10, 10)));
        buttonArea.add(Box.createVerticalGlue());

        buttonArea.setAlignmentY(CENTER_ALIGNMENT);

        add(buttonArea, BorderLayout.SOUTH);

        add(Box.createRigidArea(new Dimension(10, 10)), BorderLayout.NORTH);
        add(Box.createRigidArea(new Dimension(10, 10)), BorderLayout.EAST);
        add(Box.createRigidArea(new Dimension(10, 10)), BorderLayout.WEST);

        this.pack();
        this.centerOnScreen();
        this.setVisible(true);
    }

    public static void showWelcomeDialogMaybe(Options options)
    {
        String toSuppress = options.getStringOption(
            Options.suppressedWelcomeDialog);

        if (toSuppress == null
            || (!toSuppress.equals("ANY") && !toSuppress
                .equals(CURRENT_WELCOME_KEY)))
        {
            showWelcomeDialog();
            options.setOption(Options.suppressedWelcomeDialog,
                CURRENT_WELCOME_KEY);
            options.saveOptions();
        }
    }

    public static void showWelcomeDialog()
    {
        disposeDialogIfNecessary();
        showWelcome = new WelcomeDialog();
    }

    public static void disposeDialogIfNecessary()
    {
        if (showWelcome != null)
        {
            showWelcome.dispose();
        }
    }

}
