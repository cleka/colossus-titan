package net.sf.colossus.webclient;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import net.sf.colossus.webcommon.User;


/**
 *  A panel with which one can either create a new account,
 *  or change the password.
 *
 *  @author Clemens Katzer
 */
class RegisterPasswordPanel extends JDialog
{
    private static final Logger LOGGER = Logger
        .getLogger(RegisterPasswordPanel.class.getName());

    private final static Point defaultLocation = new Point(600, 100);
    private final static String defaultEmail = "your.email@some.domain";

    private final WebClient webClient;
    private final boolean isRegister;

    private final Box yBox;
    private final JTextField rploginField;
    private JTextField rpEmailField;

    private JPasswordField rpOldPW;
    private final JPasswordField rpNewPW1;
    private final JPasswordField rpNewPW2;

    private final JButton rpButton;

    public RegisterPasswordPanel(WebClient webClient, boolean isRegister,
        String username)
    {
        super(webClient, "", true);

        this.webClient = webClient;
        this.isRegister = isRegister;
        setTitle(isRegister ? "Create account" : "Change password");

        Container rootPane = getContentPane();

        yBox = new Box(BoxLayout.Y_AXIS);
        yBox.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel p = new JPanel(new GridLayout(0, 2));
        p.setAlignmentX(LEFT_ALIGNMENT);

        if (isRegister)
        {
            addText("A registration to the \"Colossus Public Game Server\" is only needed and useful if you are ");
            addText("familiar with Colossus, and want now to connect to the server to find opponents to play.");
            addText("If you don't know Colossus yet, it's recommended that you first play a few games locally.");
            addText("\n");
            addText("You do not need to register to be able to play Colossus locally on your PC. ");
            addText("\n");
            yBox.add(p);
            addText("\n");
            addText("Fill in the values and click 'Create Account'. In the next step you will be asked ");
            addText("for a confirmation code which is sent to that address.");
            addText("\n");
        }
        else
        {
            yBox.add(p);
            addText("\n");
        }

        p.add(new JLabel("Login name"));
        rploginField = new JTextField(username);
        rploginField.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                rploginField.selectAll();
            }
        });
        p.add(rploginField);

        // old password only needed in change password
        if (!isRegister)
        {
            // @@TODO: in future, for change password (change properties)
            //   fetch email from server and let user change it?
            p.add(new JLabel("Old Password"));
            rpOldPW = new JPasswordField("");
            rpOldPW.addFocusListener(new FocusAdapter()
            {
                @Override
                public void focusGained(FocusEvent e)
                {
                    rpOldPW.selectAll();
                }
            });
            p.add(rpOldPW);
        }
        else
        {
            // register instead needs an email:
            p.add(new JLabel("Email address"));
            rpEmailField = new JTextField(defaultEmail, 20);
            rpEmailField.addFocusListener(new FocusAdapter()
            {
                @Override
                public void focusGained(FocusEvent e)
                {
                    rpEmailField.selectAll();
                }
            });
            p.add(rpEmailField);
        }

        p.add(new JLabel("Password"));
        rpNewPW1 = new JPasswordField("");
        rpNewPW1.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                rpNewPW1.selectAll();
            }
        });
        p.add(rpNewPW1);

        p.add(new JLabel("Repeat Password"));
        rpNewPW2 = new JPasswordField("");
        rpNewPW2.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                rpNewPW2.selectAll();
            }
        });
        p.add(rpNewPW2);

        p.add(new JLabel(""));
        String buttonText = isRegister ? "Create account" : "Change password";

        rpButton = new JButton(buttonText);
        rpButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (e.getSource() == rpButton)
                {
                    buttonPressed();
                }
            }
        });

        rpButton.setEnabled(true);
        p.add(rpButton);

        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (e.getSource() == cancelButton)
                {
                    dispose();
                }
            }
        });

        Box xBox = new Box(BoxLayout.X_AXIS);
        xBox.add(Box.createHorizontalGlue());
        xBox.add(cancelButton);
        xBox.add(Box.createHorizontalGlue());

        JPanel cancelPanel = new JPanel(new BorderLayout());
        cancelPanel.setAlignmentX(LEFT_ALIGNMENT);
        cancelPanel.add(xBox, BorderLayout.CENTER);
        cancelPanel.add(Box.createHorizontalGlue(), BorderLayout.WEST);
        cancelPanel.add(Box.createHorizontalGlue(), BorderLayout.EAST);
        yBox.add(cancelPanel);

        rootPane.add(yBox);

        this.setLocation(defaultLocation);
    }

    private void addText(String message)
    {
        yBox.add(WebClient.nonBoldLabel(message));
    }

    public void packAndShow()
    {
        pack();
        setVisible(true);
    }

    private void buttonPressed()
    {
        Runnable tempRunnable = new Runnable()
        {
            public void run()
            {
                // System.out.println("** Runnable started");
                buttonPressedActualAction();
                // System.out.println("** Runnable completed");
            }
        };
        // System.out.println("* Creating the runnable");
        Thread tempThread = new Thread(tempRunnable,
            "RegisterPasswordPanelButtonPressedRunnable");
        tempThread.start();
        // System.out.println("* Done with creating the runnable");
    }

    /** Run inside the Runnable/thread that was created when the
     *  Register / Change password button was pressed.
     *
     */
    private void buttonPressedActualAction()
    {
        boolean ok = true;

        String name = rploginField.getText();
        String newPW1 = new String(rpNewPW1.getPassword());
        String newPW2 = new String(rpNewPW2.getPassword());
        String oldPW = null;

        ok = ok && webClient.validateField(this, name, "Login name");
        ok = ok && webClient.validateField(this, newPW1, "New Password");
        ok = ok && webClient.validateField(this, newPW2, "Password repeat");

        if (!isRegister)
        {
            oldPW = new String(rpOldPW.getPassword());
            ok = ok && webClient.validateField(this, oldPW, "Old Password");
        }

        if (!newPW1.equals(newPW2))
        {
            JOptionPane.showMessageDialog(this,
                "Password and repeated password do not match!");
            ok = false;
        }

        if (newPW1.equals(oldPW))
        {
            JOptionPane.showMessageDialog(this,
                "Old and new are the same - no point to change!");
            ok = false;
        }

        // validateXXXchecks and PW-compare showed message dialog if
        // something is wrong, so here we simply abort.
        if (!ok)
        {
            return;
        }

        if (isRegister)
        {
            String hostname = webClient.getHost();
            String portText = webClient.getPort();
            String email = rpEmailField.getText();

            ok = ok && webClient.validateField(this, hostname, "Host name");
            ok = ok && webClient.validatePort(this, portText);
            ok = ok && webClient.validateField(this, email, "Email Adress");

            if (email.equals(defaultEmail))
            {
                JOptionPane.showMessageDialog(this,
                    "Please provide an email address!");
                ok = false;
            }
            else if (!looksLikeValidEmailAddress(email))
            {
                ok = false;
            }

            if (!ok)
            {
                return;
            }

            String reason = webClient.createRegisterWebClientSocketThread(
                name, newPW1, email, null);
            LOGGER.info("First createRegisterReturnsWCST() reason:" + reason);

            if (reason.equals(User.PROVIDE_CONFCODE))
            {
                webClient
                    .updateStatus("Provide confirmation code", Color.blue);
                handleConfirmation(name, newPW1, email);
            }
            else if (reason.equals(User.WRONG_CONFCODE))
            {
                LOGGER.severe("WRONG CONF CODE message on first try??");
                JOptionPane.showMessageDialog(this, reason);
                webClient.updateStatus(User.WRONG_CONFCODE, Color.red);
                handleConfirmation(name, newPW1, email);
            }
            else
            {
                webClient.updateStatus("Registration failed " + "(" + reason
                    + ")", Color.red);
                JOptionPane.showMessageDialog(this, reason);
            }
        }

        else
        {
            String reason = webClient.tryChangePassword(name, oldPW, newPW1);
            if (reason == null)
            {
                JOptionPane.showMessageDialog(this,
                    "Password was changed successfully.",
                    "Password change OK", JOptionPane.INFORMATION_MESSAGE);
                this.dispose();
            }
            else
            {
                JOptionPane.showMessageDialog(this,
                    "Changing password failed: " + reason,
                    "Changing password failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private boolean looksLikeValidEmailAddress(String email)
    {
        String regex = "[-.\\w]+@[-.\\w]+\\.[-\\w]+";
        if (email.matches(regex))
        {
            return true;
        }
        else
        {
            JOptionPane.showMessageDialog(this,
                "Email address does appear to be invalid!\n"
                    + "(allowed are: 'a-z, A-Z, 0-9, _ - .' and one '@'.");
            return false;
        }
    }

    private void handleConfirmation(String name, String newPW1, String email)
    {
        boolean done = false;
        while (!done)
        {
            String providedConfCode = JOptionPane.showInputDialog(this,
                "Type in the confirmation code sent to you via mail: ",
                User.TEMPLATE_CONFCODE);

            if (providedConfCode == null)
            {
                // TODO send a cancel message to server so that it
                //      removes the pending user from list?
                //      Not critical, because if user tries again
                //      it will replace it anyway...
                done = true;
            }
            else if (providedConfCode.equals(User.TEMPLATE_CONFCODE)
                || providedConfCode.equals(""))
            {
                JOptionPane.showMessageDialog(this,
                    "Confirmation code must not be empty and not "
                        + "the provided example!!");
            }
            else
            {
                providedConfCode = providedConfCode.trim();
                String reason2 = webClient
                    .createRegisterWebClientSocketThread(name, newPW1, email,
                        providedConfCode);
                if (reason2 == null)
                {
                    // OK!
                    done = true;
                }
                else
                {
                    // otherwise just show what's wrong
                    webClient.updateStatus(reason2, Color.red);
                    JOptionPane.showMessageDialog(this, reason2);
                }
            }
        }
    }

    @Override
    public void dispose()
    {
        webClient.restoreStatusField();
        setVisible(false);
        super.dispose();
    }
}
