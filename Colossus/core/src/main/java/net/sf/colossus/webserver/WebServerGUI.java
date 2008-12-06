package net.sf.colossus.webserver;


import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;


/** The web server GUI - so far shows only simple info
 *  like amount of users logged in, potential-, running-
 *  and ending games.
 *  
 *  @version $Id$
 *  @author Clemens Katzer
 */

public class WebServerGUI extends JFrame implements WindowListener,
    ActionListener, IWebServerGUI
{

    private WebServer webServer;
    private JLabel userInfo;

    private JLabel scheduledGamesInfo;
    private JLabel potentialGamesInfo;
    private JLabel runningGamesInfo;
    private JLabel endingGamesInfo;

    public WebServerGUI(WebServer webServer)
    {
        super("Colossus Web Server");

        this.webServer = webServer;

        getContentPane().setLayout(new GridLayout(0, 2));

        Container mainPane = new Box(BoxLayout.Y_AXIS);
        JScrollPane mainScrollPane = new JScrollPane(mainPane,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainScrollPane, BorderLayout.CENTER);

        userInfo = new JLabel("No users connected.");
        mainPane.add(userInfo);

        scheduledGamesInfo = new JLabel("No scheduled games.");
        mainPane.add(scheduledGamesInfo);

        potentialGamesInfo = new JLabel("No potential games.");
        mainPane.add(potentialGamesInfo);

        runningGamesInfo = new JLabel("No running games.");
        mainPane.add(runningGamesInfo);

        endingGamesInfo = new JLabel("No ending games.");
        mainPane.add(endingGamesInfo);

        addWindowListener(this);
        pack();

        setVisible(true);
    }

    public void setUserInfo(String s)
    {
        userInfo.setText(s);
    }

    public void setScheduledGamesInfo(String s)
    {
        scheduledGamesInfo.setText(s);
    }

    public void setPotentialGamesInfo(String s)
    {
        potentialGamesInfo.setText(s);
    }

    public void setRunningGamesInfo(String s)
    {
        runningGamesInfo.setText(s);
    }

    public void setEndingGamesInfo(String s)
    {
        endingGamesInfo.setText(s);
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Shutdown"))
        {
            dispose();
            webServer.initiateShutdown(true);
        }

        else
        // A combo box was changed.
        {
            //     no combo boxes yet.
        }
    }

    public void cleanup()
    {
        this.webServer = null;
    }

    public void windowClosing(WindowEvent e)
    {
        webServer.shutdownServer();
        cleanup();
        dispose();
    }

    public void windowClosed(WindowEvent e)
    {
        // nothing to do
    }

    public void windowActivated(WindowEvent e)
    {
        // nothing to do
    }

    public void windowDeactivated(WindowEvent e)
    {
        // nothing to do
    }

    public void windowDeiconified(WindowEvent e)
    {
        // nothing to do
    }

    public void windowIconified(WindowEvent e)
    {
        // nothing to do
    }

    public void windowOpened(WindowEvent e)
    {
        // nothing to do
    }
}
