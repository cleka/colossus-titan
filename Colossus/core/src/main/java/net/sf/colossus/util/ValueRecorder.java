package net.sf.colossus.util;

/**
 * An integer value, along with a detailed record of how and why
 * the value has the value it has.
 * @author Romain Dolbeau
 */
public class ValueRecorder
{
    /** The current value */
    private int value = 0;
    /** All the explanations and value changes */
    private final StringBuffer why = new StringBuffer();

    /** Augment the value.
     * @param v By how much the value change.
     * @param r The reason of the change.
     */
    public void add(int v, String r)
    {
        if (why.toString().equals("") || v < 0)
        {
            why.append("" + v);
        }
        else
        {
            why.append("+" + v);
        }
        why.append(" [" + r + "]");
        value += v;
    }

    /** Reset the value to a specific value.
     * @param v The new value to use.
     * @param r The reason of the change.
     */
    public void resetTo(int v, String r)
    {
        why.append(" | " + v);
        why.append(" [ " + r + "]");
        value = v;
    }

    /** Get the value.
     * @return The current value.
     */
    public int getValue()
    {
        return value;
    }

    /** Get the detailed explanations and final value as String.
     * @return The detailed explanations and final value.
     */
    @Override
    public String toString()
    {
        return why.toString() + " = " + value;
    }
}
