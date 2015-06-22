package net.sf.colossus.webclient;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.webcommon.FormatWhen;
import net.sf.colossus.webcommon.IWebServer;


public class ChatHandler
{
    private static final Logger LOGGER = Logger.getLogger(ChatHandler.class
        .getName());

    private final static String chatSubmitButtonText = "Submit";

    private final static int textAreaHeight = 20;
    private final String id;
    private final String title;
    private String username;
    private IWebServer server = null;

    private final JPanel chatTab;

    private final JButton chatSubmitButton;
    private final JTextArea displayArea;
    private final JScrollPane displayScrollPane;
    private final JScrollBar displayScrollBar;
    private final JTextField newMessage;

    private final FormatWhen whenFormatter;
    private boolean loginState = false;

    private long lastMsgWhen = -1;
    private boolean resentMode = false;
    private long afterResentWhen = -1;
    private String afterResentSender = null;
    private String afterResentMessage = null;

    private final static String dashes = "--------------------";
    private final static String doubledashes = "=========================";

    private final LinkedList<String> history = new LinkedList<String>();

    private static final int MAX_HISTORY = 100;
    private int posInHistory = -1;
    private String unsentMessage = "";

    public ChatHandler(String id, String title, IWebServer server,
        String username)
    {
        this.id = id;
        this.title = title;
        this.server = server;
        this.username = username;

        whenFormatter = new FormatWhen();

        chatTab = new JPanel(new BorderLayout());
        displayArea = new JTextArea();
        displayArea.setRows(textAreaHeight);
        displayArea.setEditable(false);
        displayArea.setLineWrap(false);
        displayArea.setBackground(HTMLColor.white);
        displayScrollPane = new JScrollPane(displayArea);
        displayScrollBar = displayScrollPane.getVerticalScrollBar();
        chatTab.add(displayScrollPane, BorderLayout.CENTER);

        Box submitPane = new Box(BoxLayout.X_AXIS);
        newMessage = new JTextField(60);

        ActionListener submitListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                submitText(e.getSource());
            }
        };

        KeyListener keyListener = new KeyListener()
        {
            public void keyReleased(KeyEvent e)
            {
                int kc = e.getKeyCode();
                if (kc == KeyEvent.VK_UP || kc == KeyEvent.VK_KP_UP)
                {
                    historyUp();
                }
                if (kc == KeyEvent.VK_DOWN || kc == KeyEvent.VK_KP_DOWN)
                {
                    historyDown();
                }

            }

            public void keyTyped(KeyEvent e)
            {
                // not needed
            }

            public void keyPressed(KeyEvent e)
            {
                // not needed
            }
        };

        newMessage.addActionListener(submitListener);
        newMessage.addKeyListener(keyListener);
        newMessage.setEnabled(false);
        chatSubmitButton = new JButton(chatSubmitButtonText);
        chatSubmitButton.addActionListener(submitListener);
        chatSubmitButton.setEnabled(false);
        submitPane.setPreferredSize(chatSubmitButton.getMinimumSize());

        submitPane.add(newMessage);
        submitPane.add(chatSubmitButton);
        chatTab.add(submitPane, BorderLayout.SOUTH);
    }

    private void historyUp()
    {
        if (posInHistory == -1)
        {
            unsentMessage = newMessage.getText();
            posInHistory = history.size();
        }
        if (posInHistory > 0)
        {
            posInHistory--;
            newMessage.setText(history.get(posInHistory));
        }
    }

    private void historyDown()
    {
        if (posInHistory == -1)
        {
            // not in history, do nothing
        }
        else if (posInHistory == history.size() - 1)
        {
            posInHistory = -1;
            newMessage.setText(unsentMessage);
            unsentMessage = "";
        }
        else if (posInHistory < history.size() - 1)
        {
            posInHistory++;
            newMessage.setText(history.get(posInHistory));
        }
    }

    public String getId()
    {
        return this.id;
    }

    public String getTitle()
    {
        return this.title;
    }

    public JComponent getTab()
    {
        return this.chatTab;
    }

    public void setLoginState(boolean loggedIn, IWebServer server,
        String username)
    {
        this.server = server;
        if (loggedIn != loginState)
        {
            // when logged in, button/field are enabled and vice versa
            newMessage.setEnabled(loggedIn);
            chatSubmitButton.setEnabled(loggedIn);

            long now = new Date().getTime();
            String txt = (loggedIn ? " logged in " : " logged out ");

            if (!loggedIn)
            {
                // logout show immediately
                chatDisplay(now, username, dashes + txt + dashes);
            }
            else
            {
                this.username = username;
                afterResentWhen = now;
                afterResentSender = username;
                afterResentMessage = dashes + txt + dashes;
            }
        }
        loginState = loggedIn;

    }

    public void submitText(Object source)
    {
        if (source == chatSubmitButton || source == newMessage)
        {
            String chatId = IWebServer.generalChatName;
            String message = newMessage.getText();
            if (message.equals(""))
            {
                message = " ";
            }
            server.chatSubmit(chatId, username, message);
            addToHistory(message);
            posInHistory = -1;
            newMessage.setText(unsentMessage);
            unsentMessage = "";
        }
        else
        {
            LOGGER.warning("");
        }
    }

    private void addToHistory(String message)
    {
        if (!message.trim().equals(""))
        {
            history.add(message);
            if (history.size() > MAX_HISTORY)
            {
                history.removeFirst();
            }
        }
    }

    public void chatDeliver(long when, String sender, String message,
        boolean resent)
    {
        if (resent)
        {

            // null, null on sender side, signals end of resending
            if (sender.equals("null") && message.equals("null"))
            {
                if (resentMode)
                {
                    chatDisplayInEDT(dashes
                        + " end of redisplaying older messages " + dashes
                        + "\n");
                }
                else
                {
                    chatDisplayInEDT(dashes + " (no messages to redisplay) "
                        + dashes + "\n");
                }
                resentMode = false;
                if (afterResentSender != null)
                {
                    chatDisplay(afterResentWhen, afterResentSender,
                        afterResentMessage);
                    afterResentWhen = -1;
                    afterResentSender = null;
                    afterResentMessage = null;
                }
            }
            else if (when <= lastMsgWhen)
            {
                // skip display message -- older than lastMsgWhen
            }
            else
            {
                if (!resentMode)
                {
                    chatDisplayInEDT("\n" + dashes + " redisplaying last "
                        + "(currently up to 50) messages " + dashes + "\n");
                }
                resentMode = true;
                lastMsgWhen = when;
                chatDisplay(when, sender, message);
            }
        }
        else
        {

            lastMsgWhen = when;
            chatDisplay(when, sender, message);
        }
    }

    private void chatDisplay(long when, String sender, String message)
    {
        String whenTime = whenFormatter.timeAsString(when);
        String dateChange = whenFormatter.hasDateChanged();

        String doubleDashLine = "";
        if (dateChange != null)
        {
            doubleDashLine = "\n" + doubledashes + " " + dateChange + " "
                + doubledashes + "\n";
        }

        String textToAppend = doubleDashLine + whenTime + " " + sender + ": "
            + message + "\n";
        chatDisplayInEDT(textToAppend);
    }

    private void chatDisplayInEDT(final String textToAppend)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            appendToDisplayArea(textToAppend);
        }
        else
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    appendToDisplayArea(textToAppend);
                }
            });
        }

    }

    private void appendToDisplayArea(final String textToAppend)
    {
        displayArea.append(textToAppend);
        displayArea.setCaretPosition(displayArea.getDocument().getLength());
        if (displayArea.getLineCount() - 2 > displayArea.getRows())
        {
            displayScrollBar.setValue(displayScrollBar.getMaximum());
        }
    }

    public void setBackgroundColor(Color color)
    {
        displayArea.setBackground(color);
    }

} // END class ChatHandler
