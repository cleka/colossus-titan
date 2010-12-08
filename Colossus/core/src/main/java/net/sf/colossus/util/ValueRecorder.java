package net.sf.colossus.util;


import java.util.ArrayList;
import java.util.List;


/**
 * An integer value, along with a detailed record of how and why
 * the value has the value it has.
 * @author Romain Dolbeau
 */
public class ValueRecorder implements IValueRecorderItem
{
    /** The current value */
    private int value = 0;
    private float scale = 1;
    private final String desc;
    /** All the explanations and value changes */
    private final List<IValueRecorderItem> items = new ArrayList<IValueRecorderItem>();

    public ValueRecorder()
    {
        desc = null;
    }

    public ValueRecorder(String desc)
    {
        this.desc = desc;
    }

    public boolean isReset()
    {
        return false;
    }

    private class TrivialValueRecorderItem implements IValueRecorderItem
    {
        private final int v;
        private final String why;
        private final boolean isReset;

        TrivialValueRecorderItem(int v, String why, boolean isReset)
        {
            this.v = v;
            this.why = why;
            this.isReset = isReset;
        }

        public int getValue()
        {
            return v;
        }

        public String getWhy(String prefix)
        {
            return why;
        }

        public String getFull(String prefix)
        {
            StringBuffer buf = new StringBuffer();
            buf.append("\n");
            buf.append(prefix);
            if (isReset())
            {
                buf.append("| ");
            }
            else if (v >= 0)
            {
                buf.append("+ ");
            }
            buf.append(getValue());
            buf.append(" [" + why + "]");
            return buf.toString();
        }

        public boolean isReset()
        {
            return isReset;
        }
    }

    /** Augment the value.
     * @param v By how much the value change.
     * @param r The reason of the change.
     */
    public void add(int v, String r)
    {
        items.add(new TrivialValueRecorderItem(v, r, false));

        value += v;
    }

    /**
     * Augment the value.
     * @param v By how much the value change, and why
     */
    public void add(ValueRecorder v)
    {
        items.add(v);

        value += v.getValue();
    }

    /**
     * Reset the value to a specific value.
     * @param v The new value to use.
     * @param r The reason of the change.
     */
    public void resetTo(int v, String r)
    {
        items.add(new TrivialValueRecorderItem(v, r, true));

        value = v;
    }

    /** Get the value.
     * @return The current value.
     */
    public int getValue()
    {
        return Math.round(value * scale);
    }

    public void setScale(float scale)
    {
        this.scale = scale;
    }

    public boolean isEmpty()
    {
        return items.isEmpty();
    }

    public String getWhy(String prefix)
    {
        StringBuffer buf = new StringBuffer();
        for (IValueRecorderItem item : items)
        {
            buf.append(item.getFull(prefix + "\t"));
        }
        return buf.toString();
    }

    public String getFull(String prefix)
    {
        StringBuffer buf = new StringBuffer();
        buf.append("\n");
        if (desc != null)
            buf.append(desc);
        buf.append(prefix);
        if (isReset())
        {
            buf.append("| ");
        }
        else if (getValue() >= 0)
        {
            buf.append("+ ");
        }
        buf.append(getValue());
        buf.append(" [" + getWhy(prefix) + "]");
        if (scale != 1.)
        {
            buf.append(" * " + scale);
        }
        return buf.toString();
    }

    /** Get the detailed explanations and final value as String.
     * @return The detailed explanations and final value.
     */
    @Override
    public String toString()
    {
        return getWhy("") + " = " + getValue();
    }
}
