
package com.werken.opt;

/** <p>Exception thrown when an option requiring an argument
    is not provided with an argument.</p>

    @author bob mcwhirter (bob @ werken.com)
*/
public class MissingArgumentException extends ParseException
{
    /** Construct a new Exception with a message

        @param msg Explanation of the exception
    */
    public MissingArgumentException(String msg)
    {
        super(msg);
    }
}
