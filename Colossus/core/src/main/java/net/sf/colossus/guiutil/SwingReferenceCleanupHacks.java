package net.sf.colossus.guiutil;


import java.lang.reflect.Field;


/**
 * Special hack to cleanup some static reference to the JFrame
 * inside Swing; copied from here:
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4907798
 *
 * @author Clemens Katzer
 */
public class SwingReferenceCleanupHacks
{
    public static void cleanupJPopupMenuGlobals(
        boolean removeOnlyMenuKeyboardHelpers)
    {
        try
        {
            javax.swing.MenuSelectionManager aMenuSelectionManager = javax.swing.MenuSelectionManager
                .defaultManager();
            Object anObject = safelyGetReflectedField(
                "javax.swing.MenuSelectionManager", "listenerList",
                aMenuSelectionManager);
            if (null != anObject)
            {
                javax.swing.event.EventListenerList anEventListenerList = (javax.swing.event.EventListenerList)anObject;
                Object[] listeners = anEventListenerList.getListenerList();

                if (removeOnlyMenuKeyboardHelpers)
                {
                    // This gives us back an Array and the even entries are the
                    // class type.  In this case they are all javax.swing.event.ChangeListeners
                    // The odd number entries are the instance themselves.
                    // We were having a problem just blindly removing all of the listeners
                    // because the next time a popupmenu was show, it wasn't getting dispose (i.e you
                    // right click and click off to cancel and the menu doesn't go away).  We traced
                    // the memory leak down to this javax.swing.plaf.basic.BasicPopupMenuUI$MenuKeyboardHelper
                    // holding onto an instance of the JRootPane.  Therefore we just remove all of the
                    // instances of this class and it cleans up fine and seems to work.
                    Class<?> aClass = Class
                        .forName("javax.swing.plaf.basic.BasicPopupMenuUI$MenuKeyboardHelper");
                    for (int i = listeners.length - 1; i >= 0; i -= 2)
                    {
                        if (aClass.isInstance(listeners[i]))
                        {
                            aMenuSelectionManager
                                .removeChangeListener((javax.swing.event.ChangeListener)listeners[i]);
                        }
                    }
                }
                else
                {
                    for (int i = listeners.length - 1; i >= 0; i -= 2)
                    {
                        aMenuSelectionManager
                            .removeChangeListener((javax.swing.event.ChangeListener)listeners[i]);
                    }
                }
            }
        }
        catch (Exception e)
        {
            //      e.printStackTrace();
        }

        try
        {
            javax.swing.ActionMap anActionMap = (javax.swing.ActionMap)javax.swing.UIManager
                .getLookAndFeelDefaults().get("PopupMenu.actionMap");
            while (anActionMap != null)
            {
                Object[] keys = { "press", "release" };
                boolean anyFound = false;
                for (Object aKey : keys)
                {
                    Object aValue = anActionMap.get(aKey);
                    anyFound = anyFound || aValue != null;
                    anActionMap.remove(aKey);
                }
                if (!anyFound)
                {
                    break;
                }
                anActionMap = anActionMap.getParent();
            }
        }
        catch (Exception e)
        {
            //      e.printStackTrace();
        }

        SafelySetReflectedFieldToNull(
            "javax.swing.plaf.basic.BasicPopupMenuUI", "menuKeyboardHelper",
            null);

        Object anObject = safelyGetReflectedField(
            "com.sun.java.swing.plaf.windows.WindowsPopupMenuUI",
            "mnemonicListener", null);
        if (null != anObject)
        {
            SafelySetReflectedFieldToNull(anObject.getClass(), "repaintRoot",
                anObject);
        }

    }

    private static void SafelySetReflectedFieldToNull(Class<?> aClass,
        String aFieldName, Object anObject)
    {
        try
        {
            java.lang.reflect.Field aField = aClass
                .getDeclaredField(aFieldName);
            aField.setAccessible(true);
            aField.set(anObject, null);
        }
        catch (Exception e)
        {
            // ignored...
        }
    }

    private static void SafelySetReflectedFieldToNull(String aClassName,
        String aFieldName, Object anObject)
    {
        try
        {
            Class<?> aClass = Class.forName(aClassName);
            SafelySetReflectedFieldToNull(aClass, aFieldName, anObject);
        }
        catch (Exception e)
        {
            return;
        }
    }

    private static Object safelyGetReflectedField(String aClassName,
        String aFieldName, Object anObject)
    {
        try
        {
            Class<?> aClass = Class.forName(aClassName);
            Field aField = aClass.getDeclaredField(aFieldName);
            aField.setAccessible(true);
            return aField.get(anObject);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static void cleanupJMenuBarGlobals()
    {
        SafelySetReflectedFieldToNull(
            "com.sun.java.swing.plaf.windows.WindowsRootPaneUI$AltProcessor",
            "root", null);
        SafelySetReflectedFieldToNull(
            "com.sun.java.swing.plaf.windows.WindowsRootPaneUI$AltProcessor",
            "winAncestor", null);
    }
}
