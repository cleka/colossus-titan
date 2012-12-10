package net.sf.colossus.server;


import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import net.sf.colossus.guiutil.KFrame;


/**
 * Simple log window for Startup progress (waiting for clients)
 *
 * @author Clemens Katzer
 */
public final class StartupProgress implements ActionListener
{
    /**
     * The time the window takes to show itself.
     *
     * This is a number of milliseconds to wait before showing the window in this
     * class is shown. This means that in local games where everyone is there straight
     * away the window will never be visible.
     */
    private static final int SHOWUP_DELAY = 1000;

    private KFrame logFrame;
    private final TextArea text;
    private final Container pane;
    private Server server;
    private JButton b;
    private final JCheckBox autoCloseCheckBox;
    private final Timer showUpTimer;

    public StartupProgress(Server server)
    {
        this.server = server;

        net.sf.colossus.util.InstanceTracker.register(this, "only one");

        //Create and set up the window.
        this.logFrame = new KFrame("Server startup progress log");

        logFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        this.pane = logFrame.getContentPane();

        this.text = new TextArea("", 20, 80);
        pane.add(text, BorderLayout.CENTER);

        JButton b1 = new JButton("Abort");
        this.b = b1;
        b1.setVerticalTextPosition(SwingConstants.CENTER);
        b1.setHorizontalTextPosition(SwingConstants.LEADING); //aka LEFT, for left-to-right locales
        b1.setMnemonic(KeyEvent.VK_A);
        b1.setActionCommand("abort");
        b1.addActionListener(this);
        b1.setToolTipText("Click this button to abort the start process.");
        pane.add(b1, BorderLayout.SOUTH);

        this.autoCloseCheckBox = new JCheckBox(
            "Automatically close when game starts");
        autoCloseCheckBox.setSelected(true);
        pane.add(autoCloseCheckBox, BorderLayout.NORTH);

        //Display the window with delay -- if the game starts quickly we don't want to see it
        showUpTimer = new Timer(SHOWUP_DELAY, new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                logFrame.pack();
                logFrame.setVisible(true);
                showUpTimer.stop();
            }
        });
        showUpTimer.start();
    }

    public void append(String s)
    {
        this.text.append(s + "\n");
    }

    public JFrame getFrame()
    {
        return logFrame;
    }

    /**
     * Server startup calls this so that possible warning message
     * can be noticed.
     */
    public void disableAutoClose()
    {
        this.autoCloseCheckBox.setSelected(false);
    }

    public void setCompleted()
    {
        if (showUpTimer.isRunning())
        {
            // don't show anymore if not necessarily
            showUpTimer.stop();
        }
        if (this.autoCloseCheckBox.isSelected())
        {
            this.dispose();
            return;
        }
        this.text
            .append("OK, all clients have come in. You can close this window now.");

        JButton b2 = new JButton("Close");
        b2.setMnemonic(KeyEvent.VK_C);
        b2.setActionCommand("close");
        b2.addActionListener(this);
        b2.setToolTipText("Click this button to close this window.");

        this.pane.remove(this.b);
        this.pane.add(b2, BorderLayout.SOUTH);
        this.b = b2;
        this.logFrame.pack();
    }

    public void dispose()
    {
        if (showUpTimer.isRunning())
        {
            // don't try to show anymore
            showUpTimer.stop();
        }
        if (this.logFrame != null)
        {
            this.logFrame.dispose();
            this.logFrame = null;
        }
    }

    public void cleanRef()
    {
        this.server = null;
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("abort"))
        {
            // change the abort button to a QUIT button, with which
            // one could request a System.exit() if the attempted
            // "clean up everything nicely and return to GetPlayers menu"
            // fails or hangs or something ...
            JButton b3 = new JButton("QUIT");
            b3.setMnemonic(KeyEvent.VK_C);
            b3.setActionCommand("totallyquit");
            b3.addActionListener(this);
            b3.setToolTipText("Click this button to totally exit "
                + "this application.");
            this.pane.remove(this.b);
            this.pane.add(b3, BorderLayout.SOUTH);
            this.logFrame.pack();

            this.text.append("\nAbort requested, please wait...\n");
            this.server.startupProgressAbort();
        }

        // if abort fails (hangs, NPE, ... , button is a QUIT instead,
        // so user can request a System.exit() then.
        else if (e.getActionCommand().equals("totallyquit"))
        {
            this.text.append("\nQUIT - Total Exit requested, "
                + "doing System.exit() !!\n");
            this.server.startupProgressQuit();
        }

        else if (e.getActionCommand().equals("close"))
        {
            this.text.append("\nClosing...\n");
            this.dispose();
        }
    }

    public void tooOldClient(String clientName)
    {
        this.append("\nERROR:\n");
        this.append("Player  '" + clientName + "'  tried to join with "
            + "too old version of Colossus.\n"
            + "At least release 0.13.3 is needed to join a game with "
            + "the changed BeelzeGods12 variant!");
        this.append("\nGame startup can't proceed - please press the "
            + " Abort button!\n");
    }
}
