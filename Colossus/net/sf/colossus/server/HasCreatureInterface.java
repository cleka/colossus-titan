package net.sf.colossus.server;

/**
 * Interface for an Oracle (a creature is available or not) used for AI Hints.
 * @version $Id$
 * @author Romain Dolbeau
 */
public interface HasCreatureInterface
{
    public boolean hasCreature(String name);
}
