package net.sf.colossus.webserver;


import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;


/**
 *  The web server GUI - so far shows only simple info
 *  like amount of users logged in, scheduled-, instant-,
 *  running- and ending games.
 *
 *  @author Clemens Katzer
 */
public class WebServerGUI extends JFrame implements IWebServerGUI
{

    private WebServer webServer;
    private final JLabel userInfo;

    private final JLabel scheduledGamesInfo;
    private final JLabel instantGamesInfo;
    private final JLabel runningGamesInfo;
    private final JLabel endingGamesInfo;
    private final JLabel suspendedGamesInfo;
    private final JLabel usedPortsInfo;
    private final JButton dumpInfoButton;

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

        instantGamesInfo = new JLabel("No instant games.");
        mainPane.add(instantGamesInfo);

        runningGamesInfo = new JLabel("No running games.");
        mainPane.add(runningGamesInfo);

        endingGamesInfo = new JLabel("No ending games.");
        mainPane.add(endingGamesInfo);

        suspendedGamesInfo = new JLabel("No suspended games.");
        mainPane.add(suspendedGamesInfo);

        usedPortsInfo = new JLabel("All ports free.");
        mainPane.add(usedPortsInfo);

        dumpInfoButton = new JButton("Dump Info");
        mainPane.add(dumpInfoButton);
        dumpInfoButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dumpInfoButtonAction();
            }
        });

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                initiateShutdown();
            }
        });
        pack();

        setVisible(true);
    }

    public void initiateShutdown()
    {
        webServer.initiateShutdown(null);
        // rest is done when webServer calls our shutdown()
    }

    public void setUserInfo(String s)
    {
        userInfo.setText(s);
    }

    public void setScheduledGamesInfo(String s)
    {
        scheduledGamesInfo.setText(s);
    }

    public void setInstantGamesInfo(String s)
    {
        instantGamesInfo.setText(s);
    }

    public void setRunningGamesInfo(String s)
    {
        runningGamesInfo.setText(s);
    }

    public void setEndingGamesInfo(String s)
    {
        endingGamesInfo.setText(s);
    }

    public void setSuspendedGamesInfo(String s)
    {
        suspendedGamesInfo.setText(s);
    }

    public void setUsedPortsInfo(String s)
    {
        usedPortsInfo.setText(s);
    }

    public void dumpInfoButtonAction()
    {
        webServer.dumpInfo();
    }

    public void cleanup()
    {
        this.webServer = null;
    }

    public void shutdown()
    {
        cleanup();
        dispose();
    }
}
