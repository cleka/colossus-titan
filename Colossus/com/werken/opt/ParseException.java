
package com.werken.opt;

/** <p>Base for Exceptions thrown during parsing of a command-line<p>

    @author bob mcwhirter (bob @ werken.com)
*/
public class ParseException extends OptException
{
    /** Construct a new Exception with a message

        @param msg Explanation of the exception
    */
    public ParseException(String msg)
    {
        super(msg);
    }
}
