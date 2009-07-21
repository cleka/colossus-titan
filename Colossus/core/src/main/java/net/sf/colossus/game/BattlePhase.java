/**
 * 
 */
package net.sf.colossus.game;


public enum BattlePhase
{
    SUMMON("Summon", true, false), RECRUIT("Recruit", true, false), MOVE(
        "Move", true, false), FIGHT("Fight", false, true), STRIKEBACK(
        "Strikeback", false, true);

    /**
     * Determine if the phase is part of the fighting.
     *
     * @return true iff the phase is either FIGHT or STRIKEBACK.
     */
    public boolean isFightPhase()
    {
        return isFightPhase;
    }

    /**
     * Determine if the phase is part of the fighting.
     * Right now we consider also SUMMON or RECRUT as move phase
     * (currently they are own phases; I would like those actions
     *  to happen as part of the move phase instead, then this here
     *  can be changed).
     *
     * @return true iff the phase is a move phase;
     *
     */
    public boolean isMovePhase()
    {
        return isMovePhase;
    }

    /**
     * Returns a non-localized UI string for the phase.
     */
    @Override
    public String toString()
    {
        return name;
    }

    private final String name;
    private final boolean isMovePhase;
    private final boolean isFightPhase;

    private BattlePhase(String name, boolean isMovePhase, boolean isFightPhase)
    {
        this.name = name;
        this.isMovePhase = isMovePhase;
        this.isFightPhase = isFightPhase;
    }
}