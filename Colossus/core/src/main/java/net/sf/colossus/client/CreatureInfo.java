package net.sf.colossus.client;


import net.sf.colossus.variant.CreatureType;


/**
 *  Basic information about one creature, for split prediction.
 *
 *  @author David Ripton
 */
class CreatureInfo implements Cloneable
{
    private final CreatureType type;

    // Are we sure this creature is in this legion?
    private boolean certain;

    // Was the creature here when this legion was split off?
    private boolean atSplit;

    CreatureInfo(CreatureType type, boolean certain, boolean atSplit)
    {
        this.type = type;
        this.certain = certain;
        this.atSplit = atSplit;
    }

    final String getName()
    {
        if (type.isTitan())
        {
            return "Titan";
        }
        else
        {
            return type.getName();
        }
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
        return new CreatureInfo(type, certain, atSplit);
    }

    /**
     * Two CreatureInfo objects match if the types match.
     */
    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CreatureInfo))
        {
            throw new ClassCastException();
        }
        return type.equals(((CreatureInfo)other).type);
    }

    /** Two CreatureInfo objects match if the names match. */
    @Override
    public int hashCode()
    {
        return type.hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(getName());
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

    public CreatureType getType()
    {
        return type;
    }
}
