
package com.werken.opt;

import java.util.List;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

/** <p>Represents list of arguments parsed against
    a {@link Options} descriptor.<p>

    <p>It allows querying of a boolean {@link #optIsSet(char opt)},
    in addition to retrieving the {@link #getOptValue(char opt)}
    for options requiring arguments.</p>

    <p>Additionally, any left-over or unrecognized arguments,
    are available for further processing.</p>
*/
    
public class CommandLine
{
    private List _args    = new LinkedList();
    private Map  _options = new HashMap();

    CommandLine()
    {

    }

    /** <p>Query to see if an option has been set.</p>

        @param opt Short single-character name of the option
        @return true if set, false if not
    */
    public boolean optIsSet(char opt)
    {
        return _options.containsKey( new Character(opt) );
    }

    /** <p>Retrieve the argument, if any,  of an option.</p>

        @param opt Short single-character name of the option
        @return Value of the argument if option is set, and has an argument, else null.
    */
    public String getOptValue(char opt)
    {
        return (String) _options.get( new Character(opt) );
    }

    /** <p>Retrieve any left-over non-recognized options and arguments</p>

        @return List of remaining items passed in but not parsed
    */
    public List getArgs()
    {
        return _args;
    }

    void addArg(String arg)
    {
        _args.add( arg );
    }

    void setOpt(char opt)
    {
        _options.put( new Character(opt),
                      null );
    }

    void setOpt(char opt, String value)
    {
        _options.put( new Character(opt),
                      value );

    }

    /** <p>Dump state, suitable for debugging.</p>

        @return Stringified form of this object
    */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();

        buf.append( "[ CommandLine: [ options: " );
        buf.append( _options.toString() );
        buf.append( " ] [ args: ");
        buf.append( _args.toString() );
        buf.append( " ] ]" );

        return buf.toString();
    }
        
}
