package net.sf.colossus.game;

import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;

/**
 *
 * @author Romain Dolbeau
 */
public interface BattleCritter {

    CreatureType getCreatureType();

    BattleHex getCurrentHex();

    String getDescription();

    int getHits();

    int getPointValue();

    int getPower();

    int getSkill();

    BattleHex getStartingHex();

    int getTag();

    int getTitanPower();

    boolean hasMoved();

    boolean hasStruck();

    void moveToHex(BattleHex hex);

    boolean isDead();

    boolean isInverted();

    boolean isRangestriker();

    boolean isTitan();

    void setDead(boolean dead);

    void setHex(BattleHex hex);

    void setHits(int hits);

    void setMoved(boolean moved);

    void setStruck(boolean struck);

    boolean wouldDieFrom(int hits);

}
