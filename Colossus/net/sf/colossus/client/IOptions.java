package net.sf.colossus.client;

import java.util.*;

/** Allows getting and setting options.
 *  An attempt to reduce the God-class nature of Client.
 *  @version $Id$
 *  @author David Ripton
 */
public interface IOptions
{
    boolean getOption(String optname);
    String getStringOption(String optname);
    int getIntOption(String optname);
    void setOption(String optname, String value);
    void setOption(String optname, boolean value);
    void setOption(String optname, int value);
}
