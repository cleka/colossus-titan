package net.sf.colossus.common;


/** Someone to ask for an (I)Options object.
 *  Kind of temporary solution to get rid of direct dependency to Client
 *  or Server; sometimes the actual object can not (easily) be passed in
 *  already at creation time of a "options-needer" because the options
 *  are actually instantiated (and/or synchronized from server to client)
 *  later.
 *
 *  TODO using options listeners instead would probably be a better solution,
 *  but I don't dare to go that far yet...
 *
 *  @author Clemens Katzer
 *
 */
public interface OptionObjectProvider
{
    public abstract IOptions getOptions();
}
