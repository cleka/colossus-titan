package net.sf.colossus.game.actions;


import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.CreatureType;


// TODO why is there only one recruiter? Does it imply knowledge about how many creatures are needed?
// Since there is always N of one type that could work.
public class Recruitment extends AddCreatureAction
{
    private final CreatureType recruiter;

    public Recruitment(Legion legion, CreatureType recruited,
        CreatureType recruiter)
    {
        super(legion, recruited);
        this.recruiter = recruiter;
    }

    public CreatureType getRecruited()
    {
        return getAddedCreatureType();
    }

    public CreatureType getRecruiter()
    {
        return recruiter;
    }

    @Override
    public CreatureType[] getRevealedCreatures()
    {
        return new CreatureType[] {};
    }

    @Override
    public String getReason()
    {
        // TODO distinguish between Constants.reasonRecruited and Constants.reasonReinforced
        return Constants.reasonRecruited;
    }

    @Override
    public String toString()
    {
        return String.format(
            "Recruitment of creature of type %s into legion %s",
            getRecruited(), getLegion())
            + (getRecruiter() != null ? ", recruiter type is "
                + getRecruiter() : "");
    }
}
