package net.sf.colossus.webserver;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;


/**
 *  Sending a simple (text-only) mail by submitting it to the SMTP
 *  server at port 25.
 *  Basically copied 1:1 (only changed formatting and indentation) from:
 *    http://www.torsten-horn.de/techdocs/java-smtp.htm
 *
 *  @author Clemens Katzer
 */
public class SmtpSimple
{
    private DataOutputStream os = null;
    private BufferedReader is = null;
    private String sRt = "";

    public static void main(String[] args)
    {
        System.out
            .println("\nSmtpSimple.java. Send simple email.\nUsage:\n"
                + "  java SmtpSimple SmtpServer FromAdr FromRealName ToAdr ToRealName Subject Text\n"
                + "Example:\n"
                + "  java SmtpSimple mail.gmx.net MeinName@MeinProvider.de \"Vorname Nachname\" x@y.z xyz S T\n");
        if (null == args || 6 > args.length)
        {
            System.out.println("Error: parameters missing!");
            System.exit(1);
        }
        else
        {
            try
            {
                SmtpSimple smtp = new SmtpSimple();
                String result = smtp.sendEmail(args[0], args[1], args[2],
                    args[3], args[4], args[5], (6 < args.length) ? args[6]
                        : null);
                System.out.println(result);
            }
            catch (Exception ex)
            {
                System.out.println("Error:\n" + ex);
                System.exit(2);
            }
            System.exit(0);
        }
    }

    public synchronized final String sendEmail(String sSmtpServer,
        String sFromAdr, String sFromRealName, String sToAdr,
        String sToRealName, String sSubject, String sText) throws IOException,
        Exception
    {
        Socket so = null;
        try
        {
            sRt = "";
            if (null == sSmtpServer
                || 0 >= sSmtpServer.length()
                || null == sFromAdr
                || 0 >= sFromAdr.length()
                || null == sToAdr
                || 0 >= sToAdr.length()
                || ((null == sSubject || 0 >= sSubject.length()) && (null == sText || 0 >= sText
                    .length())))
            {
                throw new Exception(
                    "Invalid Parameters for SmtpSimple.sendEmail().");
            }
            if (null == sFromRealName || 0 >= sFromRealName.length())
            {
                sFromRealName = sFromAdr;
            }
            if (null == sToRealName || 0 >= sToRealName.length())
            {
                sToRealName = sToAdr;
            }
            so = new Socket(sSmtpServer, 25);
            os = new DataOutputStream(so.getOutputStream());
            is = new BufferedReader(new InputStreamReader(so.getInputStream()));
            so.setSoTimeout(10000);
            writeRead(true, "220", null);
            writeRead(true, "250", "HELO " + sSmtpServer + "\n");
            writeRead(true, "250", "RSET\n");
            writeRead(true, "250", "MAIL FROM:<" + sFromAdr + ">\n");
            writeRead(true, "250", "RCPT TO:<" + sToAdr + ">\n");
            writeRead(true, "354", "DATA\n");
            writeRead(false, null, "To: " + sToRealName + " <" + sToAdr
                + ">\n");
            writeRead(false, null, "From: " + sFromRealName + " <" + sFromAdr
                + ">\n");
            writeRead(false, null, "Subject: " + sSubject + "\n");
            writeRead(false, null, "Mime-Version: 1.0\n");
            writeRead(false, null,
                "Content-Type: text/plain; charset=\"iso-8859-1\"\n");
            writeRead(false, null,
                "Content-Transfer-Encoding: quoted-printable\n\n");
            writeRead(false, null, sText + "\n");
            writeRead(true, "250", ".\n");
            writeRead(true, "221", "QUIT\n");
            return sRt;
        }
        finally
        {
            if (is != null)
                try
                {
                    is.close();
                }
                catch (Exception ex)
                { /* ignore */
                }
            if (os != null)
                try
                {
                    os.close();
                }
                catch (Exception ex)
                { /* ignore */
                }
            if (so != null)
                try
                {
                    so.close();
                }
                catch (Exception ex)
                { /* ignore */
                }
            is = null;
            os = null;
        }
    }

    private final void writeRead(boolean bReadAnswer,
        String sAnswerMustStartWith, String sWrite) throws IOException,
        Exception
    {
        if (null != sWrite && 0 < sWrite.length())
        {
            sRt += sWrite;
            os.writeBytes(sWrite);
        }
        if (bReadAnswer)
        {
            String sRd = is.readLine() + "\n";
            sRt += sRd;
            if (null != sAnswerMustStartWith
                && 0 < sAnswerMustStartWith.length()
                && !sRd.startsWith(sAnswerMustStartWith))
                throw new Exception(sRt);
        }
    }
}
