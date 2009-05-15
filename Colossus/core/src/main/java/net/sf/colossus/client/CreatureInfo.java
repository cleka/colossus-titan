package net.sf.colossus.client;


/**
 *  Basic information about one creature, for split prediction.
 *
 *  @author David Ripton
 */
class CreatureInfo implements Cloneable
{
    private final String name;

    // Are we sure this creature is in this legion?
    private boolean certain;

    // Was the creature here when this legion was split off?
    private boolean atSplit;

    // TODO first parameter should be CreatureType
    CreatureInfo(String name, boolean certain, boolean atSplit)
    {
        if (name.startsWith("Titan"))
        {
            name = "Titan";
        }
        else if (name.length() == 0)
        {
            throw new RuntimeException("CreatureInfo with empty name!");
        }
        this.name = name;
        this.certain = certain;
        this.atSplit = atSplit;
    }

    final String getName()
    {
        return name;
    }

    void setCertain(boolean certain)
    {
        this.certain = certain;
    }

    boolean isCertain()
    {
        return certain;
    }

    void setAtSplit(boolean atSplit)
    {
        this.atSplit = atSplit;
    }

    boolean isAtSplit()
    {
        return atSplit;
    }

    @Override
    public CreatureInfo clone()
    {
        return new CreatureInfo(name, certain, atSplit);
    }

    /** Two CreatureInfo objects match if the names match. */
    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CreatureInfo))
        {
            throw new ClassCastException();
        }
        return name.equals(((CreatureInfo)other).name);
    }

    /** Two CreatureInfo objects match if the names match. */
    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(name);
        if (!certain)
        {
            sb.append('?');
        }
        if (!atSplit)
        {
            sb.append('*');
        }
        return sb.toString();
    }
}
