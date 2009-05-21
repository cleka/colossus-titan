/**
 *
 */
package net.sf.colossus.common;


/**
 *  An interface for Non-Server-Side (not in server package being) GUI
 *  classes (so far, the WebClient), so that they can request from the
 *  "start-Object" to initiate a new game without actually having to
 *  have a reference to the net.sf.colossus.server package.
 *  This way the WebClient does not depend on server package
 *  (otherwise it would be a cyclic dependency).
 */
public interface IStartHandler
{
    public void startWebGameLocally(Options presetOptions, String username);
}
