import java.util.*;

/**
 * interface to allow for multiple AI implementations
 *
 * @version $Id$
 */
public interface AI
{
    /** make masterboard moves for current player in the Game */
    void masterMove(Game game);

    /** make splits for current player */
    void split(Game game);

    /** make recruits for current player */
    void muster(Game game);

    /** pick one reinforcement */
    Creature reinforce(Legion legion, Game game);

    /** choose whether legion should flee from enemy */
    boolean flee(Legion legion, Legion enemy, Game game);

    /** choose whether legion should concede to enemy */
    boolean concede(Legion legion, Legion enemy, Game game);

    /** make battle strikes for legion */
    void strike(Legion legion, Battle battle, Game game,
        boolean fakeDice);

    /** choose whether to take a penalty in order to possibly carry */
    boolean chooseStrikePenalty(Critter critter, Critter target,
        Critter carryTarget, Battle battle, Game game);

    /** make battle moves for the active legion */
    void battleMove(Game game);

    /** pick an entry side */
    int pickEntrySide(String hexLabel, Legion legion, Game game);

    /** pick an engagement to resolve */
    String pickEngagement(Game game);

    /** choose whether to acquire an angel or archangel */
    String acquireAngel(Legion legion, ArrayList recruits, Game game);

    /** choose whether to summon an angel or archangel */
    String summonAngel(Legion legion, Game game);

    /** pick a color of legion markers */
    String pickColor(Set colors, List favoriteColors);

    /** pick a legion marker */
    String pickMarker(Collection markerIds);
}
