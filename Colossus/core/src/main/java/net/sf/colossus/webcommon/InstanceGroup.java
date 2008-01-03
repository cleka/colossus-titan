package net.sf.colossus.webcommon;


import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *  Storage object for all objects of one class type registered
 *  in InstanceTracker.
 *   
 *  @version $Id$
 *  @author Clemens Katzer
 *    
 */

public class InstanceGroup
{
    private static final Logger LOGGER =
        Logger.getLogger(InstanceGroup.class.getName());

    private WeakHashMap instances;
    private String shortType;

    public InstanceGroup(String type)
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
            return type.substring(index + 1);
        }
    }

    public synchronized void addInstance(Object o, String id)
    {
        typeInstance i = new typeInstance(o, id);
        instances.put(o, i);
    }

    public synchronized String getPrintStatistics()
    {
        StringBuffer gstat = new StringBuffer("");
        
        int count = instances.size();
        if (count == 0)
        {
            return gstat.substring(0);
        }

        gstat.append("  " + count + " instances of type " + shortType);

        String sep = ": ";
        Iterator it = instances.keySet().iterator();
        while (it.hasNext())
        {
            Object key = it.next();
            if (key == null)
            {
                LOGGER.log(Level.FINEST,
                    "object key already null, removing it...");
                it.remove();
            }
            else
            {
                typeInstance i = (typeInstance)instances.get(key);
                gstat.append(sep + i.getId());
                sep = ", ";
            }
        }
        it = null;
        gstat.append("\n");
        return gstat.substring(0);
    }

    public synchronized int amountLeft()
    {
        int amount = instances.size();
        return amount;
    }

    public synchronized typeInstance getInstance(Object o)
    {
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
