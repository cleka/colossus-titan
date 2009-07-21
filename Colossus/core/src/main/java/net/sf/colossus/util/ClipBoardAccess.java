package net.sf.colossus.util;


import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;


/** Little helper class to copy some text (e.g. an error message 
 *  or exception ;-) to the system's clipboard.
 *  
 *  Copied with some modifications from:
 *    http://www.javapractices.com/topic/TopicAction.do?Id=82
 */

public final class ClipBoardAccess implements ClipboardOwner
{
    public static void main(String... aArguments)
    {
        ClipBoardAccess cbAccess = new ClipBoardAccess();

        //display what is currently on the clipboard
        System.out.println("Clipboard contains:"
            + cbAccess.getClipboardContents());

        //change the contents and then re-display
        cbAccess.setClipboardContents("blah, blah, blah");
        System.out.println("Clipboard contains:"
            + cbAccess.getClipboardContents());
    }

    /**
     * Empty implementation of the ClipboardOwner interface.
     */
    public void lostOwnership(Clipboard aClipboard, Transferable aContents)
    {
        //do nothing
    }

    /**
     * Place a String on the clipboard, and make this class the
     * owner of the Clipboard's contents.
     */
    public void setClipboardContents(String text)
    {
        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, this);
    }

    /**
     * Get the String residing on the clipboard.
     *
     * @return any text found on the Clipboard; if none found, return an
     * empty String.
     */
    public String getClipboardContents()
    {
        String result = "";
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        //odd: the Object param of getContents is not currently used
        Transferable contents = clipboard.getContents(null);

        if (contents != null
            && contents.isDataFlavorSupported(DataFlavor.stringFlavor))
        {
            try
            {
                result = (String)contents
                    .getTransferData(DataFlavor.stringFlavor);
            }
            catch (UnsupportedFlavorException ex)
            {
                //highly unlikely since we are using a standard DataFlavor
                System.out.println(ex);
                ex.printStackTrace();
            }
            catch (IOException ex)
            {
                System.out.println(ex);
                ex.printStackTrace();
            }
        }
        return result;
    }
}
