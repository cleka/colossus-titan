
package com.werken.opt;

/** <p>Exception thrown during parsing signalling an unrecognized
    option was seen.<p>


    @author bob mcwhiter (bob @ werken.com)
*/
public class UnrecognizedOptionException extends ParseException
{
    /** Construct a new Exception with a message

        @param msg Explanation of the exception
    */
    public UnrecognizedOptionException(String msg)
    {
        super(msg);
    }
}
