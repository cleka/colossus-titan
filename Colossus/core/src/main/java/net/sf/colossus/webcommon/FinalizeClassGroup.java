package net.sf.colossus.webcommon;


import java.util.Iterator;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;


/**
 *  Storage object for all objects of one class type registered
 *  in FinalizeManager.
 *   
 *  @version $Id$
 *  @author Clemens Katzer
 *    
 */

public class FinalizeClassGroup
{
    private WeakHashMap instances;
    // private String type;
    private String shortType;

    public FinalizeClassGroup(String type)
    {
        instances = new WeakHashMap();
        this.shortType = shortType(type);
    }

    public static String shortType(String type)
    {
        int index = type.lastIndexOf(".");
        if (index == -1)
        {
            return type;
        }
        else
        {
            return type.substring(index+1);
        }
    }

    public synchronized void addInstance(Object o, String id)
    {
        if (id == null)
        {
            // System.out.println("\n00000000\naddInstance, id null!");
        }
        typeInstance i = new typeInstance(o, id);
        instances.put(o, i);
    }

    public synchronized void printStatistics(boolean detailed)
    {
        int count = instances.size();
        if (count == 0)
        {
            return;
        }

        System.out.print("  " + count + " instances of type " + shortType);

        if (!detailed)
        {
            // System.out.println("");
            // return;
        }

        String sep = ": ";
        Iterator it = instances.keySet().iterator();
        while (it.hasNext())
        {
            Object key = it.next();
            if (key == null)
            {
                System.out.println("object key already null, removing it...");
                it.remove();
            }
            else
            {
                typeInstance i = (typeInstance)instances.get(key);
                String id = i.getId();
                System.out.print(sep + id);
                sep = ", ";
            }
        }
        it = null;
        System.out.println("");
    }

    public synchronized int amountLeft()
    {
        int amount = instances.size();
        return amount;
    }

    public synchronized void removeInstance(Object o)
    {
        String type = o.getClass().getName();
        System.out.println("removeInstance, trying to find object of type " +
            type + "o="+ o);

        int count = instances.size();
        System.out.println("this group has " + count + " entries: " +
            instances.toString());
        Iterator it = instances.keySet().iterator();
        boolean done = false;

        while (!done && it.hasNext())
        {
            typeInstance i = (typeInstance)it.next();
            Object o2 = i.getObj();
            System.out.println("comparing it to object " + o2);
            if (i.getObj() == o)
            {
                System.out.println("Found object, removing it, id is " +
                    i.getId());
                it.remove();
                done = true;
            }
        }
        if (!done)
        {
            System.out.println("removeInstance: Ooops! object of type " + type +
                " to remove not found!");
        }
    }

    public synchronized typeInstance getInstance(Object o)
    {
        // String type = o.getClass().getName();
        // System.out.println("getInstance, trying to find object of type " + type + "o="+ o);

        // int count = instances.size();
        Iterator it = instances.keySet().iterator();
        typeInstance foundInstance = null;

        while (foundInstance == null && it.hasNext())
        {
            Object key = it.next();
            if (key == null)
            {
                return null;
            }
            else
            {
                typeInstance i = (typeInstance)instances.get(key);
                if (i.getObj() == o)
                {
                    foundInstance = i;
                }
            }
        }
        it = null;
        return foundInstance;
    }

    public class typeInstance
    {
        private WeakReference objRef;
        private String id;

        private typeInstance(Object o, String id)
        {
            this.objRef = new WeakReference(o);
            this.id = id;
        }

        String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            // System.out.println("instance.setId: old="+this.id+", new="+id);
            this.id = id;
        }

        Object getObj()
        {
            return objRef.get();
        }

        public String toString()
        {
            return "Object with id " + id;
        }
    }
}
