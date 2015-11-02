package net.sf.colossus.webclient;


import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import net.sf.colossus.guiutil.KDialog;


public class ContactAdminDialog extends KDialog implements ActionListener
{
    private static final Logger LOGGER = Logger
        .getLogger(ContactAdminDialog.class.getName());

    private final JTextArea textArea;

    private final JTextField nameField;
    private final JTextField mailField;

    public ContactAdminDialog(Frame owner, String title, boolean modal)
    {
        super(owner, title, modal);

        LOGGER.finest("ContactAdminDialog instantiated.");

        Container contentPane = getContentPane();
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        String text = "<html><p /><p />"
            + "Fill in name and email, and type your message below, and  then click <b>Submit</b>.<p /><p />"
            + "If your text is very long, you can also send me a 'normal' email to "
            + "<tt>support@play-colossus.net  </tt>.<p /><p />" + "</html>";
        JLabel instructionLabel = WebClient.nonBoldLabel(text);

        Box topPanel = new Box(BoxLayout.Y_AXIS);
        topPanel.add(instructionLabel);
        nameField = addTextField(topPanel, "Your name: ");
        mailField = addTextField(topPanel, "Your email: ");
        mainPanel.add(topPanel, BorderLayout.NORTH);

        textArea = new JTextArea("Type your message here...", 10, 60);
        textArea.selectAll();
        mainPanel.add(textArea, BorderLayout.CENTER);

        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(this);
        Box bottomPanel = new Box(BoxLayout.X_AXIS);
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(submitButton);
        bottomPanel.add(Box.createHorizontalGlue());
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        contentPane.add(mainPanel);

        this.pack();
        this.setVisible(true);
    }

    private JTextField addTextField(Box topPanel, String text)
    {
        Box line = new Box(BoxLayout.X_AXIS);
        line.add(new JLabel(text));
        JTextField textField = new JTextField(30);
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
            String text = textArea.getText();
            System.out.println("Text: '" + text + "'");
        }
    }
}
