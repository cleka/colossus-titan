package net.sf.colossus.server;

/**
 * Interface for an Oracle (a terrain is reachable or not) used for AI Hints.
 * @version $Id$
 * @author Romain Dolbeau
 */
public interface CanReachTerrainInterface
{
    public boolean canReach(char t);
}
