package net.sf.colossus.ai;


import java.util.List;
import java.util.Set;

import net.sf.colossus.client.LegionInfo;


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
    boolean masterMove();

    /** make splits for current player.  Return true if done */
    boolean split();

    /** continue making splits.  Return true if done. */
    boolean splitCallback(String parentId, String childId);

    /** make recruits for current player */
    void muster();

    /** pick one reinforcement for legion */
    void reinforce(LegionInfo legion);

    /** choose whether legion should flee from enemy */
    boolean flee(LegionInfo legion, LegionInfo enemy);

    /** choose whether legion should concede to enemy */
    boolean concede(LegionInfo legion, LegionInfo enemy);

    /** make battle strikes for legion */
    boolean strike(LegionInfo legion);

    /** return a list of battle moves for the active legion */
    List battleMove();

    /** Try another move for creatures whose moves failed. */
    void retryFailedBattleMoves(List bestMoveOrder);

    /** pick an entry side */
    String pickEntrySide(String hexLabel, String markerId, Set entrySides);

    /** pick an engagement to resolve */
    String pickEngagement();

    /** choose whether to acquire an angel or archangel */
    String acquireAngel(String markerId, List recruits);

    /** choose whether to summon an angel or archangel */
    String summonAngel(String summonerId);

    /** pick a color of legion markers */
    String pickColor(List colors, List favoriteColors);

    /** pick a legion marker */
    String pickMarker(Set markerIds, String preferredShortColor);

    /** choose carry target */
    void handleCarries(int carryDamage, Set carryTargets);

    /** pick an optional strike penalty */
    String pickStrikePenalty(List choices);
}
