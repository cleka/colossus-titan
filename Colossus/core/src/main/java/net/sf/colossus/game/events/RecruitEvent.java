package net.sf.colossus.game.events;


import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.CreatureType;


// TODO why is there only one recruiter? Does it imply knowledge about how many creatures are needed?
// Since there is always N of one type that could work.
public class RecruitEvent extends AddCreatureEvent
{
    private final CreatureType recruiter;

    public RecruitEvent(int turn, Legion legion,
        CreatureType recruited, CreatureType recruiter)
    {
        super(turn, legion.getPlayer(), legion, recruited);
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

    @SuppressWarnings("boxing")
    @Override
    public String toString()
    {
        return String
            .format(
                "In turn %d, player %s has recruited creature of type %s into legion %s",
                getTurn(), getPlayer(), getRecruited(), getLegion())
            + (getRecruiter() != null ? ", recruiter type is "
                + getRecruiter() : "");
    }
}
