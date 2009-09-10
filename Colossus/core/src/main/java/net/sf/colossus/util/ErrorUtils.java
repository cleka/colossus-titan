package net.sf.colossus.util;


import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import net.sf.colossus.common.Options;


/**
 *  Small helper methods to deal with Exceptions, how to get them into
 *  String-format and display them to the user etc.<br><br>
 *  Displaying of the message dialog is also provided here, so that
 *  otherwise non-GUI classes have a simple way to show a dialog,
 *  without need to worry about being headless etc.
 */
public class ErrorUtils
{
    private static final Logger LOGGER = Logger.getLogger(ErrorUtils.class
        .getName());

    private static List<String> errorDuringFunctionalTest = new ArrayList<String>();

    /**
     * Query the stacktrace items from an exception, and put them
     * nicely into a single string.
     * @param e An exception that was caught somewhere
     * @return A string object containing all the stack trace lines.
     */
    public static String makeStackTraceString(Throwable e)
    {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw, true));
        String stackTrace = sw.toString();

        return stackTrace;
    }

    public static void clearErrorDuringFunctionalTest()
    {
        errorDuringFunctionalTest.clear();
    }

    /**
     *
     * @param reason
     * @throws IllegalArgumentException
     */
    public static void setErrorDuringFunctionalTest(String reason)
        throws IllegalArgumentException
    {
        if (reason == null)
        {
            throw new IllegalArgumentException(
                "reason to setErrorDuringFunctionalTest must not be null!");
        }
        errorDuringFunctionalTest.add(reason);
    }

    public static String getErrorDuringFunctionalTest()
    {
        StringBuilder errorList = new StringBuilder("");
        for (String errorMsg : errorDuringFunctionalTest)
        {
            errorList.append(errorMsg + "\n");
        }
        return errorList.toString();
    }

    public static boolean checkErrorDuringFunctionalTest()
    {
        return !(errorDuringFunctionalTest.isEmpty());
    }

    /** During stress-testing, don't bother to show message,
     *  instead exit immediately:
     */

    private static void exitIfStresstest()
    {
        if (Options.isStresstest())
        {
            String info = "Exiting due to an Exception: "
                + "A dialog box should have been shown now, "
                + "but we are in stresstest so we rather exit immediately "
                + "to get data for troubleshooting.";
            LOGGER.info(info);
            System.exit(1);
        }
    }

    /**
     * Show display an error/warning in an JOptionPage message dialog,
     * typically for the situation that an exception had occured.
     * Creates a special frame for the dialog, if given frame is null.
     * If called during stresstest, do System.exit(1) with explanatory
     * message to logfile. If headless, display is skipped.
     *
     * @param frame A frame to be used as parent for the dialog.
     *        If null, an own frame is created for that purpose.
     * @param message Message to be displayed in the dialog window
     * @param title Title of the dialog window
     * @param error If true, type is error message, for false only warning
     */
    public static void showExceptionDialog(JFrame frame, String message,
        String title, boolean error)
    {
        // as method name says...
        exitIfStresstest();

        // Skip copying to clipboard and showing of the message dialog
        // if there is no Graphics device available:
        if (GraphicsEnvironment.isHeadless())
        {
            return;
        }

        // Try to copy the whole message text to clipboard already
        String copiedInfo = "";
        boolean copied = copyToClipboard(message);
        if (!copied)
        {
            LOGGER.info("NOTE: Attempt to copy message to Clipboard failed.");
        }
        else
        {
            copiedInfo = "\n[This error message should now also be "
                + "in your clipboard.]";
        }

        String frameTitle = "EXCEPTION CAUGHT - see dialog box!";
        showTheDialog(frame, frameTitle, title, message + copiedInfo, error);
    }

    /**
     * Show display an error/warning in an JOptionPage message dialog,
     * but this one here typically NOT for the situation that an exception
     * had occured. Does NOT copy anything to clipboard.
     * Creates a special frame for the dialog, if given frame is null.
     * If called during stresstest, do System.exit(1) with explanatory
     * message to logfile. If headless, display is skipped.
     *
     * @param frame A frame to be used as parent for the dialog.
     *        If null, an own frame is created for that purpose.
     * @param title Title of the dialog window
     * @param message Message to be displayed in the dialog window
     */
    public static void showErrorDialog(JFrame frame, String title,
        String message)
    {
        // as method name says...
        exitIfStresstest();

        /* During functional testing, don't bother to show message,
         * instead log it and return immediately:
         */
        if (Options.isFunctionalTest())
        {
            String info = "Exiting due to an Error or Exception: "
                + "A dialog box should have been shown now, "
                + "but we are in a functional test so we rather exit "
                + "immediately instead of waiting for user input .";
            LOGGER.severe(info);
            setErrorDuringFunctionalTest("showErrorDialog requested");
            return;
        }

        // Skip copying to clipboard and showing of the message dialog
        // if there is no Graphics device available:
        if (GraphicsEnvironment.isHeadless())
        {
            return;
        }

        String frameTitle = "AN ERROR OCCURRED - see dialog box!";
        boolean error = true;
        showTheDialog(frame, frameTitle, title, message, error);
    }

    /**
     * Show the dialog box with given parameters;
     * if necessary (no parent frame given), create own dummy frame
     * to avoid that the message dialog is hidden behind other GUI
     * frames/dialogs, and is not even visible in the task bar.
     *
     * @param frame      A parent frame to use, might be null
     * @param frameTitle The title to use for the frame to create
     * @param title      The title for the message dialog
     * @param message    The actual message to show in the dialog
     * @param error      Type of message (true for error, false for warning)
     */
    private static void showTheDialog(JFrame frame, String frameTitle,
        String title, String message, boolean error)
    {
        JFrame showFrame = frame;
        if (showFrame == null)
        {
            showFrame = makeDummyErrorFrame(frameTitle != null ? frameTitle
                : "PROBLEM OCCURED - see dialog box!");
        }

        JOptionPane.showMessageDialog(showFrame, message, title,
            error ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE);

        // If we created the dummy frame, must get also rid of it.
        if (frame == null)
        {
            showFrame.dispose();
        }
    }

    /**
     * Creates a JFrame object which can be used as parent for a dialog; the
     * frame is centered and contains a text telling that it is a dummy frame
     * just for that purpose that one does not miss the message dialog.
     *
     * @returns The JFrame object that can be used as parent for the dialog
     */
    private static JFrame makeDummyErrorFrame(String frameTitle)
    {
        JFrame f = new JFrame(frameTitle);
        Box panel = new Box(BoxLayout.Y_AXIS);
        panel.add(new JLabel(
            "This is a dummy frame. It is only created in order to display"));
        panel.add(new JLabel(
            "a message dialog box, which without this frame might be hidden"));
        panel
            .add(new JLabel(
                "behind some other window(s) and is easily missed, because it does"));
        panel.add(new JLabel("not show up in the task bar..."));
        f.getContentPane().add(panel);
        f.pack();
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        f.setLocation(new Point(d.width / 2 - f.getSize().width / 2, d.height
            / 2 - f.getSize().height / 2));
        f.setVisible(true);
        f.requestFocus();

        return f;
    }

    public static boolean copyToClipboard(String message)
    {
        boolean ok = false;
        try
        {
            ClipBoardAccess cbAccess = new ClipBoardAccess();
            cbAccess.setClipboardContents(message);
            ok = true;
        }
        catch (Exception e)
        {
            // Whatever happened here, it does not matter, at least
            // it's not important enough to spoil any higher level
            // processing... so, make sure we never through any
            // problem up.
        }
        return ok;
    }
}
