package net.sf.colossus.webclient;

import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;



/** A panel with which one can either create a new account,
 *  or change the password.
 */
class RegisterPasswordPanel extends JDialog
{
    private final static Point defaultLocation = new Point(600, 100);
    private final static String defaultEmail = "your.email@some.domain";

    private final WebClient webClient;
    private final boolean isRegister;

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

        JPanel p = new JPanel(new GridLayout(0, 2));
        rootPane.add(p);

        p.add(new JLabel("Login name"));
        rploginField = new JTextField(username);
        p.add(rploginField);

        // old password only needed in change password
        if (!isRegister)
        {
            // @@TODO: in future, for change password (change properties)
            //   fetch email from server and let user change it?
            p.add(new JLabel("Old Password"));
            rpOldPW = new JPasswordField("");
            p.add(rpOldPW);
        }
        else
        {
            // register instead needs an email:
            p.add(new JLabel("Email address"));
            rpEmailField = new JTextField(defaultEmail);
            p.add(rpEmailField);
        }

        p.add(new JLabel("Password"));
        rpNewPW1 = new JPasswordField("");
        p.add(rpNewPW1);

        p.add(new JLabel("Repeat Password"));
        rpNewPW2 = new JPasswordField("");
        p.add(rpNewPW2);

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

        this.setLocation(defaultLocation);
    }

    public void packAndShow()
    {
        pack();
        setVisible(true);
    }

    private void buttonPressed()
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
                "Old and new password do not match!");
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

            if (!ok)
            {
                return;
            }

            webClient.createRegisterWebClientSocketThread(name, newPW1, email);
        }

        else
        {
            String reason = webClient.tryChangePassword(name, oldPW, newPW1);
            if (reason == null)
            {
                JOptionPane.showMessageDialog(this,
                    "Password was changed successfully.",
                    "Password change OK",
                    JOptionPane.INFORMATION_MESSAGE);
                this.dispose();
            }
            else
            {
                JOptionPane.showMessageDialog(this,
                    "Changing password failed: " + reason,
                    "Changing password failed",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void dispose()
    {
        setVisible(false);
        super.dispose();
    }
}
