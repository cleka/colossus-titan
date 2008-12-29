package net.sf.colossus.webcommon;


import java.util.logging.Logger;

import net.sf.colossus.webserver.SmtpSimple;

/** Encapsulates the way how the web server sends mail in some situations,
 *  so far only for registration procedure.
 *  This is in webcommon (even if the client side never needs this)
 *  because on client side the "User" gets instantiated and that one
 *  needs the ColossusMail class during compile time.
 *  
 *  Right now during development time (30.12.2008) there is a boolean variable
 *    reallySendMail 
 *  which controls whether it actually sends the mail, or just prints 
 *  something to STDERR (because I do not have a mailserver running locally
 *  on the PC where I do the development). 
 */

public class ColossusMail
{
    private static final Logger LOGGER = Logger.getLogger(ColossusMail.class
        .getName());


    // TODO Those (or at least some of those) should come from a config file...
    // For sending the registration mail:
    private final static String MAILSERVER = "localhost";
    private final static String FROM_ADR = "cpgs@cleka.net";
    private final static String FROM_NAME = "Colossus Public Game Server Registration Service";
    private final static String THIS_SERVER = "Colossus Public Game Server";
    private final static String CONTACT_EMAIL = "no_such_user_yet@cleka.net";
    private final static String CONTACT_WWW = "www.cleka.net:/no.such.page.yet.html";


    public static String sendConfirmationMail(String username,
        String email, String confCode)
    {
        try
        {
            SmtpSimple smtp = new SmtpSimple();
            
            String subject = "Confirmation code for registration at "
                + THIS_SERVER;
            String message = "Hello " + username + ",\n\n"
                + "please use the following confirmation code\n\n    "
                + confCode + "\n\n"
                + "to complete your registration at the " + THIS_SERVER + "."
                + "\n\n\nWith Regards,\n\n"
                + "Clemens Katzer (administrator of this server)\n\n\n"
                + "\n-------------\n\n"
                + "NOTE:\n"
                + "If you didn't do anything related to a registration "
                + "at this server,\n"
                + "probably someone else used your email address\n"
                + "(accidentally or intentionally).\n\n"
                + "If you wish, you may report this to " + CONTACT_EMAIL + ",\n"
                + "or go to " + CONTACT_WWW + " to contact us.\n\n\n--\n"
                + "PS: do not reply to this email - noone will read it...\n";
                
            // SmtpServer FromAdr FromRealName ToAdr ToRealName Subject Text

            boolean reallySendMail = false;
            if (reallySendMail)
            {
                LOGGER.fine("ok, sending mail to " + username
                    + " <" + email + ">");
                
                String result = smtp.sendEmail(MAILSERVER, FROM_ADR, FROM_NAME,
                    email, username, subject, message);
                System.out.println("SENDING EMAIL, RESULT BEGIN\n-----\n"
                    + result
                    + "SENDING EMAIL, RESULT END\n-----\n");
            }
            else
            {
                System.err.println("\nI WOULD NOW SEND THE FOLLOWING MAIL:\n\n"
                    + "From: " + FROM_NAME + " <" + FROM_ADR + ">\n"
                    + "To: " + username + " <" + email + ">\n"
                    + "Subject: " + subject + "\n\n"
                    + message + "\nEND OF MAIL\n\n");
            }
        }
        catch(Exception ex)
        {
            LOGGER.severe("Exception during mail sending: " + ex);
            return "Sending mail failed - see log file!";
        }

        return null;
    }
}
