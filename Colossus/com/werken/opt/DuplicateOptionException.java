
package com.werken.opt;

/** <p>Exception thrown when a duplicating Option is added to
    an {@link Options} descriptor.<p>

    @author bob mcwhiter (bob @ werken.com)
*/
public class DuplicateOptionException extends OptException
{
    /** Construct a new Exception with a message

        @param msg Explanation of the exception
    */
    public DuplicateOptionException(String msg)
    {
        super(msg);
    }
}
