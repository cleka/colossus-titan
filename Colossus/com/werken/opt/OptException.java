
package com.werken.opt;

/** <p>Base exception for the <code>werken.opt</code> package</p>

    @author bob mcwhiter (bob @ werken.com)
 */
public class OptException extends Exception
{
    /** Construct a new Exception with a message

        @param msg Explanation of the exception
    */
    public OptException(String msg)
    {
        super(msg);
    }
}
