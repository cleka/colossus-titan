package net.sf.colossus.common;


import java.beans.PropertyChangeListener;


/**
 * Allows getting and setting options.
 *
 * An attempt to reduce the God-class nature of Client.
 *
 * @author David Ripton
 */
public interface IOptions
{
    /**
     * A callback interface for changes to the options.
     *
     * This is a bit like {@link PropertyChangeListener}, only that we currently
     * still use a lot of booleans and ints. Replacing them with Boolean and Integer
     * instances would allow us to just make a proper Java Bean.
     *
     * This class is meant to be extended as anonymous inner class with exactly one
     * method overridden matching the type of the option specified, i.e. if the option
     * "showSomething" is a boolean value, then the a listener for that option should look
     * like this:
     *
     * <code>
     *   options.addListener("showSomething", new Listener()
     *   {
     *     public void booleanOptionChanged(String optname, boolean oldValue, boolean newValue)
     *     {
     *        .... do something ...
     *     }
     *   }
     * </code>
     *
     * It is also always possible to subscribe to options by their String value.
     *
     * Of course that is one-way, if there is a need to remove the listener individually a reference
     * has to be kept.
     *
     * TODO get rid of this, ideally use something that has an Option<T> or similar to make
     * things typesafe all the way through. Note that setting a boolean or int value as string
     * will potentially avoid listeners.
     */
    abstract class Listener
    {
        public void booleanOptionChanged(
            @SuppressWarnings("unused") String optname,
            @SuppressWarnings("unused") boolean oldValue,
            @SuppressWarnings("unused") boolean newValue)
        {
            // default does nothing
        }

        public void intOptionChanged(
            @SuppressWarnings("unused") String optname,
            @SuppressWarnings("unused") int oldValue,
            @SuppressWarnings("unused") int newValue)
        {
            // default does nothing
        }

        public void stringOptionChanged(
            @SuppressWarnings("unused") String optname,
            @SuppressWarnings("unused") String oldValue,
            @SuppressWarnings("unused") String newValue)
        {
            // default does nothing
        }
    }

    /**
     * Adds a listener to get callbacks for changes on the specified option.
     *
     * The listener should be called only on true changes, not if an option gets set to
     * the value it has anyway.
     */
    void addListener(String optname, Listener listener);

    /**
     * Removes the listener from all options it is subscribed to.
     *
     * TODO there is a chance of leakage if classes subscribe to the options but never
     * remove their listeners.
     */
    void removeListener(Listener listener);

    boolean getOption(String optname);

    boolean getOption(String optname, boolean defaultValue);

    String getStringOption(String optname);

    int getIntOption(String optname);

    void setOption(String optname, String value);

    void setOption(String optname, boolean value);

    void setOption(String optname, int value);
}
