package net.sf.colossus.server;


import java.util.*;

import net.sf.colossus.client.Client;


/**
 * interface to allow for multiple AI implementations
 *
 * @author Bruce Sherrod
 * @author David Ripton
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

    /** pick one reinforcement for legion */
    void reinforce(Legion legion, Game game);

    /** choose whether legion should flee from enemy */
    boolean flee(Legion legion, Legion enemy, Game game);

    /** choose whether legion should concede to enemy */
    boolean concede(Legion legion, Legion enemy, Game game);

    /** make battle strikes for legion */
    void strike(Legion legion, Battle battle);

    /** choose whether to take a penalty in order to possibly carry */
    PenaltyOption chooseStrikePenalty(SortedSet penaltyOptions);

    /** make battle moves for the active legion */
    void battleMove(Game game);

    /** pick an entry side */
    int pickEntrySide(String hexLabel, Legion legion, Game game,
        boolean left, boolean bottom, boolean right);

    /** pick an engagement to resolve */
    String pickEngagement(Game game);

    /** choose whether to acquire an angel or archangel */
    String acquireAngel(String markerId, List recruits);

    /** choose whether to summon an angel or archangel */
    String summonAngel(String summonerId, Client client);

    /** pick a color of legion markers */
    String pickColor(Set colors, List favoriteColors);

    /** pick a legion marker */
    String pickMarker(Set markerIds, String preferredShortColor);
}
