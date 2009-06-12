package net.sf.colossus.util;


import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Storage object for all objects of one class type registered in
 * InstanceTracker.
 *
 * @author Clemens Katzer
 */
public class InstanceGroup
{
    private static final Logger LOGGER = Logger.getLogger(InstanceGroup.class
        .getName());

    private final WeakHashMap<Object, TypeInstance> instances;
    private final String shortType;

    public InstanceGroup(String type)
    {
        instances = new WeakHashMap<Object, TypeInstance>();
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
        TypeInstance i = new TypeInstance(o, id);
        instances.put(o, i);
    }

    public synchronized String getPrintStatistics()
    {
        StringBuilder gstat = new StringBuilder("");

        int count = instances.size();
        if (count == 0)
        {
            return gstat.substring(0);
        }

        gstat.append("  " + count + " instances of type " + shortType);

        String sep = ": ";
        Iterator<Object> it = instances.keySet().iterator();
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
                TypeInstance i = instances.get(key);
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

    public synchronized TypeInstance getInstance(Object o)
    {
        Iterator<Object> it = instances.keySet().iterator();
        TypeInstance foundInstance = null;

        while (foundInstance == null && it.hasNext())
        {
            Object key = it.next();
            if (key == null)
            {
                return null;
            }
            else
            {
                TypeInstance i = instances.get(key);
                if (i.getObj() == o)
                {
                    foundInstance = i;
                }
            }
        }
        it = null;
        return foundInstance;
    }

    public class TypeInstance
    {
        private final WeakReference<Object> objRef;
        private String id;

        private TypeInstance(Object o, String id)
        {
            this.objRef = new WeakReference<Object>(o);
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

        @Override
        public String toString()
        {
            return "Object with id " + id;
        }
    }
}
