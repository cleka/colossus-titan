package net.sf.colossus.ai;


import java.util.List;
import java.util.Set;

import net.sf.colossus.ai.helper.CritterMove;
import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.game.SummonInfo;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * interface to allow for multiple AI implementations
 *
 * @author Bruce Sherrod
 * @author David Ripton
 */
public interface AI
{

    public void setVariant(Variant variant);

    /** pick a color of legion markers */
    PlayerColor pickColor(List<PlayerColor> colors,
        List<PlayerColor> favoriteColors);

    /** pick a legion marker */
    String pickMarker(Set<String> markerIds, String preferredShortColor);

    /** make splits for current player.  Return true if done */
    boolean split();

    /** continue making splits.  Return true if done. */
    boolean splitCallback(Legion parent, Legion child);

    /** make masterboard moves for current player in the Game */
    boolean masterMove();

    /** make recruits for current player */
    void muster();

    /** pick one reinforcement for legion */
    void reinforce(Legion legion);

    /** pick an engagement to resolve */
    MasterHex pickEngagement();

    /** choose whether legion should flee from enemy */
    boolean flee(Legion legion, Legion enemy);

    /** choose whether legion should concede to enemy */
    boolean concede(Legion legion, Legion enemy);

    /** pick an entry side */
    EntrySide pickEntrySide(MasterHex hex, Legion legion,
        Set<EntrySide> entrySides);

    /** a Battle start */
    void initBattle();

    /** return a list of battle moves for the active legion */
    List<CritterMove> battleMove();

    /** Initialize battle move related variables */
    public void setupBattleMove();

    /** Move a critter move from bestMoveOrder to failedBattleMoves */
    public void handleFailedBattleMove();

    /** Try another move for creatures whose moves failed. */
    void retryFailedBattleMoves(List<CritterMove> bestMoveOrder);

    /** Remove critter move from BestMoveOrser list */
    public void markBattleMoveSuccessful(int tag, BattleHex endingHex);

    /** make battle strikes for legion */
    boolean strike(Legion legion);

    /** choose carry target */
    void handleCarries(int carryDamage, Set<String> carryTargets);

    /** pick an optional strike penalty */
    String pickStrikePenalty(List<String> choices);

    /** choose whether to summon an angel or archangel */
    SummonInfo summonAngel(Legion summoner, List<Legion> possibleDonors);

    /** a Battle is finished */
    void cleanupBattle();

    /** choose whether to acquire an angel or archangel */
    CreatureType acquireAngel(Legion legion, List<CreatureType> recruits);

    CreatureType getVariantRecruitHint(LegionClientSide legion, MasterHex hex,
        List<CreatureType> recruits);

}
