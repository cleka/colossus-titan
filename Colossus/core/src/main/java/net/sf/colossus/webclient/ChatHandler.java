package net.sf.colossus.webclient;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
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
        newMessage.addActionListener(submitListener);
        newMessage.setEnabled(false);
        chatSubmitButton = new JButton(chatSubmitButtonText);
        chatSubmitButton.addActionListener(submitListener);
        chatSubmitButton.setEnabled(false);
        submitPane.setPreferredSize(chatSubmitButton.getMinimumSize());

        submitPane.add(newMessage);
        submitPane.add(chatSubmitButton);
        chatTab.add(submitPane, BorderLayout.SOUTH);
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
            newMessage.setText("");
            server.chatSubmit(chatId, username, message);
        }
        else
        {
            LOGGER.warning("");
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
                    displayArea.append(dashes
                        + " end of redisplaying older messages " + dashes
                        + "\n");
                }
                else
                {
                    displayArea.append(dashes + " (no messages to redisplay) "
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
                    displayArea.append("\n" + dashes
                        + " redisplaying last (up to 10) messages " + dashes
                        + "\n");
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
        if (dateChange != null)
        {
            displayArea.append("\n" + doubledashes + " " + dateChange + " "
                + doubledashes + "\n");
        }
        displayArea.append(whenTime + " " + sender + ": " + message + "\n");
        if (displayArea.getLineCount() - 2 > displayArea.getRows())
        {
            displayScrollBar.setValue(displayScrollBar.getMaximum());
        }
    }

    public class FormatWhen
    {
        public static final String DATE_FORMAT = "yyyy-MM-dd";
        public static final String TIME_FORMAT = "HH:mm:ss";

        private final SimpleDateFormat dateFormatter;
        private final SimpleDateFormat timeFormatter;

        private String datePrev;
        private String changedDateString = null;

        public FormatWhen()
        {
            datePrev = "";
            dateFormatter = new SimpleDateFormat(DATE_FORMAT);
            timeFormatter = new SimpleDateFormat(TIME_FORMAT);

        }

        /* call this *after* timeAsString() call
         * It will return the new date, if changed, null otherwise */

        public String hasDateChanged()
        {
            return changedDateString;
        }

        public String timeAsString(long when)
        {
            Date whenDate = new Date(when);
            String timeNow = timeFormatter.format(whenDate);
            String dateNow = dateFormatter.format(whenDate);

            if (!dateNow.equals(datePrev))
            {
                changedDateString = dateNow;
            }
            else
            {
                changedDateString = null;
            }
            datePrev = dateNow;

            return timeNow;
        }

    } // END class FormatWhen

} // END class ChatHandler
