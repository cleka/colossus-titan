
package com.werken.opt;

/** <p>Describes a single command-line option.  It maintains
    information regarding the short-name of the option, the long-name,
    if any exists, a flag indicating if an argument is required for
    this option, and a self-documenting description of the option.</p>

    <p>An Option is not created independantly, but is create through
    an instance of {@link Options}.<p>

    @see com.werken.opt.Options
    @see com.werken.opt.CommandLine

    @author bob mcwhirter (bob @ werken.com)
*/
    
public class Option
{
    private Character     _opt         = null;
    private String        _longOpt     = null;
    private boolean       _hasArg      = false;
    private String        _description = null;

    Option(char opt,
           boolean hasArg,
           String description)
    {
        _opt         = new Character( opt );
        _hasArg      = hasArg;
        _description = description;
    }

    Option(char opt,
           String longOpt,
           boolean hasArg,
           String description)
    {
        _opt         = new Character( opt );
        _longOpt     = longOpt;
        _hasArg      = hasArg;
        _description = description;
    }

    /** <p>Retrieve the single-character name of this Option</p>

        <p>It is this character which can be used with
        {@link CommandLine#optIsSet(char opt)} and 
        {@link CommandLine#getOptValue(char opt)} to check
        for existence and argument.<p>

        @return Single character name of this option
     */
    public char getOpt()
    {
        return _opt.charValue();
    }

    /** <p>Retrieve the long name of this Option</p>

        @return Long name of this option, or null, if there is no long name
     */
    public String getLongOpt()
    {
        return _longOpt;
    }

    /** <p>Query to see if this Option has a long name</p>

        @return boolean flag indicating existence of a long name
    */
    public boolean hasLongOpt()
    {
        return ( _longOpt != null );
    }

    /** <p>Query to see if this Option requires an argument</p>

        @return boolean flag indicating if an argument is required
    */
    public boolean hasArg()
    {
        return _hasArg;
    }

    /** <p>Retrieve the self-documenting description of this Option</p>

        @return The string description of this option
    */
    public String getDescription()
    {
        return _description;
    }

    /** <p>Dump state, suitable for debugging.</p>

        @return Stringified form of this object
    */
    public String toString()
    {
        StringBuffer buf = new StringBuffer().append("[ option: ");

        buf.append( _opt );

        if ( _longOpt != null )
        {
            buf.append(" ")
                .append(_longOpt);
        }

        buf.append(" ");

        if ( _hasArg )
        {
            buf.append( "+ARG" );
        }

        buf.append(" :: ")
            .append( _description )
            .append(" ]");

        return buf.toString();
    }
}
