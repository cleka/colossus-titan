package net.sf.colossus.xmlparser;


/**
 * General Exception for our xml data parsers.
 *
 * @author David Ripton
 */
class ParseException extends Exception
{
    ParseException(String msg)
    {
        super(msg);
    }

    ParseException()
    {
        super();
    }
}
