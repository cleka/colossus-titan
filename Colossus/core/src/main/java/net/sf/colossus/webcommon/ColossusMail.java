package net.sf.colossus.webcommon;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import net.sf.colossus.webserver.SmtpSimple;
import net.sf.colossus.webserver.WebServerConstants;
import net.sf.colossus.webserver.WebServerOptions;


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

    // For sending the registration mail:
    private final String mailServer;
    private final String fromAddress;
    private final String fromName;
    private final String thisServer;
    private final String contactMail;
    private final String contactWWW;
    private final boolean reallyMail;
    private final String mailToFileName;
    private final File mailToFileFile;
    private final boolean mailToFileFlag;

    public ColossusMail(WebServerOptions options)
    {
        mailServer = options.getStringOption(WebServerConstants.optMailServer);
        fromAddress = options
            .getStringOption(WebServerConstants.optMailFromAddress);
        fromName = options.getStringOption(WebServerConstants.optMailFromName);
        thisServer = options
            .getStringOption(WebServerConstants.optMailThisServer);
        contactMail = options
            .getStringOption(WebServerConstants.optMailContactEmail);
        contactWWW = options
            .getStringOption(WebServerConstants.optMailContactWWW);
        reallyMail = options.getOption(WebServerConstants.optMailReallyMail);
        mailToFileName = options
            .getStringOption(WebServerConstants.optMailToFile);

        boolean success = false;

        File testFile = null;

        if (mailToFileName != null && !mailToFileName.equals(""))
        {
            try
            {
                testFile = new File(mailToFileName);
                PrintWriter mailToFileWriter = new PrintWriter(
                    new FileOutputStream(testFile, true));
                mailToFileWriter.println("");
                mailToFileWriter.println("WebServer started.");
                mailToFileWriter.println("");
                mailToFileWriter.close();
                success = true;
            }
            catch (IOException e)
            {
                LOGGER.warning("Exception while) trying to write "
                    + "initial message to mail file: " + e);
            }

        }
        mailToFileFlag = success;
        mailToFileFile = testFile;
    }

    public String sendConfirmationMail(String username, String email,
        String confCode)
    {
        try
        {
            SmtpSimple smtp = new SmtpSimple();

            String subject = "Confirmation code for registration at "
                + thisServer;
            String message = "Hello " + username + ",\n\n"
                + "please use the following confirmation code\n\n    "
                + confCode + "\n\n" + "to complete your registration at the "
                + thisServer + "." + "\n\n\nWith Regards,\n\n"
                + "Clemens Katzer (administrator of this server)\n\n\n"
                + "\n-------------\n\n" + "NOTE:\n"
                + "If you didn't do anything related to a registration "
                + "at this server,\n"
                + "probably someone else used your email address\n"
                + "(accidentally or intentionally).\n\n"
                + "If you wish, you may report this to " + contactMail + ",\n"
                + "or go to " + contactWWW + " to contact us.\n\n\n--\n"
                + "PS: do not reply to this email - noone will read it...\n";

            if (reallyMail)
            {
                LOGGER.fine("ok, sending mail to " + username + " <" + email
                    + ">");

                smtp.sendEmail(mailServer, fromAddress, fromName, email,
                    username, subject, message);
            }

            if (mailToFileFlag)
            {
                PrintWriter mailOut = null;
                try
                {
                    mailOut = new PrintWriter(new FileOutputStream(
                        mailToFileFile, true));

                    mailOut
                        .println("\nI WOULD NOW SEND THE FOLLOWING MAIL:\n\n"
                            + "From: " + fromName + " <" + fromAddress + ">\n"
                            + "To: " + username + " <" + email + ">\n"
                            + "Subject: " + subject + "\n\n" + message
                            + "\nEND OF MAIL\n\n");
                }
                catch (IOException e)
                {
                    LOGGER.warning("Exception while) trying to write "
                        + "a mail for user '" + username + "' to mail file: "
                        + e);
                }
                finally
                {
                    if (mailOut != null)
                    {
                        mailOut.close();
                    }
                }
            }
        }
        catch (Exception ex)
        {
            LOGGER.severe("Exception during mail sending: " + ex);
            return "Sending mail failed - see log file!";
        }

        return null;
    }
}
