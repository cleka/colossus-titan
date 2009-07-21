package net.sf.colossus.guiutil;


import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import net.sf.colossus.util.InstanceTracker;


/**
 * Dummy JFrame (KFrame) with menu.
 *
 * Seems due to some bug, AWT keeps always some reference to the last
 * JFrame which has a menu used (or even the last two of those) -
 * which will nearly always being some MasterBoard, thus preventing
 * MasterBoard and with that very often also the Client (and many other
 * related objects) from being properly garbage-collected.
 * So, by opening one or two dummy frames, we get the MasterBoards
 * free and AWT hold on those dummy frames - which are small and
 * don't hurts us much.
 * And if the SwingCleanup is done afterwards, we get even rid of
 * the dummyFrames.
 *
 * @author Clemens Katzer
 */
public class DummyFrameWithMenu extends KFrame
{
    String id;

    private AbstractAction closeBoardAction;

    public DummyFrameWithMenu(String nr)
    {
        super("dummyFrame " + nr);
        id = nr;

        setupGUI();
    }

    public static void doOneDummyFrame(String id)
    {
        DummyFrameWithMenu fdebug = new DummyFrameWithMenu(id);
        fdebug.setVisible(false);
        fdebug.dispose();
        fdebug = null;
    }

    private void setupGUI()
    {
        setupActions();

        JMenuBar menuBar = new JMenuBar();
        this.setJMenuBar(menuBar);

        // File menu

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);
        JMenuItem mi;

        mi = fileMenu.add(closeBoardAction);
        mi.setMnemonic(KeyEvent.VK_C);

        this.pack();
        this.setVisible(true);
    }

    public void setupActions()
    {
        closeBoardAction = new AbstractAction("Close")
        {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        };
    }

    // Special hack to cleanup some static reference to the JFrame
    // inside Swing; copied from here:
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4907798
    // Possibly works only under 1.4.2.

    // This is not strictly necessary to ensure proper functioning
    // of Colossus; if it does not work, it just means that the last
    // displayed JFrame will not be garbage collected, so the GC
    // will always be behind with some objects.

    // The main reason why I try to ensure this proper cleanup
    // is that this way I can use the InstanceTracker in between
    // and at program end, to check how well GC cleanup is working
    // (would be working )if there weren't this Swing issue.

    static public void swingCleanup()
    {
        SwingReferenceCleanupHacks.cleanupJPopupMenuGlobals(true);
        SwingReferenceCleanupHacks.cleanupJMenuBarGlobals();
    }

    boolean disposed = false;

    @Override
    public void dispose()
    {
        if (disposed)
        {
            return;
        }
        disposed = true;
        super.dispose();
    }

    /*
     *  Dummy Main
     *
     */
    public static void main(String[] args)
    {
        DummyFrameWithMenu f1 = new DummyFrameWithMenu("1");
        DummyFrameWithMenu f2 = new DummyFrameWithMenu("2");
        DummyFrameWithMenu f3 = new DummyFrameWithMenu("3");

        DebugMethods.waitReturn();

        f3.dispose();
        f2.dispose();
        f1.dispose();

        f3 = null;
        f1 = null;
        f2 = null;

        DebugMethods.waitReturn();

        InstanceTracker.printStatistics();

        Logger LOGGER = Logger.getLogger(DummyFrameWithMenu.class.getName());
        LOGGER.log(Level.FINEST,
            "\nDummyFrameWithMenu.main() should end now by itself.");
    }
}
