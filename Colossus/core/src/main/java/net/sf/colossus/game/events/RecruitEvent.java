package net.sf.colossus.game.events;


import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.CreatureType;

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
}
