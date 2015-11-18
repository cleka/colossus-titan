package net.sf.colossus.webclient;


import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;

import net.sf.colossus.common.IOptions;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.guiutil.KDialog;


public class ContactAdminDialog extends KDialog implements ActionListener,
    FocusListener
{
    private static final Logger LOGGER = Logger
        .getLogger(ContactAdminDialog.class.getName());

    private final WebClient webClient;

    private final JTextArea textArea;

    private final JButton submitButton;
    private final JButton cancelButton;

    private final JTextField nameField;
    private final JTextField mailField;

    public ContactAdminDialog(WebClient webClient, IOptions options,
        String initialName, String initialMail)
    {
        super(webClient, "Contact the administrator", false);
        this.webClient = webClient;

        LOGGER.info("ContactAdminDialog instantiated, user='" + initialName
            + "', mail='" + initialMail + "'");

        Container contentPane = getContentPane();
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new TitledBorder(
            "Your message to the administrator"));

        String text = "<html><p /><p />"
            + "Fill in name and email, and type your message below, and  then click <b>Submit</b>.<p /><p />"
            + "If your text is very long, you can also send me a 'normal' email to: "
            + "<tt> support@play-colossus.net  </tt>.<p /><p />" + "</html>";
        JLabel instructionLabel = WebClient.nonBoldLabel(text);

        Box topPanel = new Box(BoxLayout.Y_AXIS);
        topPanel.add(instructionLabel);

        // Keep them here - use for the case when we allow anonymous submit
        // String defaultName = "Jhon";
        // String defaultMail = "johndoe@whateveryoulike.com";

        nameField = addTextField(topPanel, "Your name: ", initialName);
        // TODO: at the moment the email is usually empty; wcst has a value
        // for it only when one has just registered.
        mailField = addTextField(topPanel, "Your email: ", initialMail);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        textArea = new JTextArea("Type your message here...", 10, 60);
        textArea.addFocusListener(this);
        textArea.setFocusTraversalKeysEnabled(true);
        textArea.selectAll();
        mainPanel.add(textArea, BorderLayout.CENTER);

        submitButton = new JButton("Submit");
        submitButton.addActionListener(this);
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);

        Box bottomPanel = new Box(BoxLayout.X_AXIS);
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(submitButton);
        bottomPanel.add(Box.createHorizontalStrut(40));
        bottomPanel.add(cancelButton);
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        contentPane.add(mainPanel);

        this.pack();
        useSaveWindow(options, "ContactAdminDialog", null);
        this.setVisible(true);
    }

    private JTextField addTextField(Box topPanel, String titleText,
        String defaultText)
    {
        Box line = new Box(BoxLayout.X_AXIS);
        line.add(new JLabel(titleText));
        JTextField textField = new JTextField(defaultText, 30);
        textField.addFocusListener(this);

        textField.setMaximumSize(new Dimension(
            textField.getPreferredSize().width, Integer.MAX_VALUE));
        line.add(textField);
        line.add(Box.createHorizontalGlue());
        line.setAlignmentX(LEFT_ALIGNMENT);
        topPanel.add(line);
        return textField;
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Submit"))
        {
            String name = nameField.getText();
            String mail = mailField.getText();
            String text = textArea.getText();

            if (name.isEmpty() || mail.isEmpty() || text.isEmpty())
            {
                JOptionPane.showMessageDialog(this,
                    "Name, Mail and Text cannot be empty!", "Missing values!",
                    JOptionPane.ERROR_MESSAGE);
            }
            // Keep that here - use for the case when we allow anonymous submit
            // else if (name.equals("Jhon")
            //    || mail.equals("johndoe@whateveryoulike.com"))
            // {
            //    JOptionPane.showMessageDialog(this,
            //        "Hey! Too lazy to change the defaults?\n"
            //            + "There's a reason why I ask for them!",
            //        "Missing values!", JOptionPane.ERROR_MESSAGE);
            // }
            else
            {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                WhatNextManager.sleepFor(1000);
                submitButton.setText("Close");
                cancelButton.setEnabled(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                webClient.sendTheMessageToAdmin(name, mail, text);
            }
        }
        else if (e.getActionCommand().equals("Close")
            || e.getActionCommand().equals("Cancel"))
        {
            webClient.reEnableContactAdminButton();
            dispose();
        }
    }

    public void focusGained(FocusEvent e)
    {
        Object c = e.getSource();
        if (c instanceof JTextComponent)
        {
            ((JTextComponent)c).selectAll();
        }
    }

    public void focusLost(FocusEvent e)
    {
        // We don't use this, just to satisfy the interface
    }

}
