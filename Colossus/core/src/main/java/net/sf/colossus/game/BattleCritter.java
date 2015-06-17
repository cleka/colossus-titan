package net.sf.colossus.game;


import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;


/**
 *
 * @author Romain Dolbeau
 */
public interface BattleCritter
{

    CreatureType getType();

    BattleHex getCurrentHex();

    String getDescription();

    int getHits();

    int getPointValue();

    int getPower();

    int getPoisonDamage();

    int getPoison();

    int getSlows();

    int getSlowed();

    int getSkill();

    BattleHex getStartingHex();

    int getTag();

    int getTitanPower();

    boolean hasMoved();

    boolean hasStruck();

    void moveToHex(BattleHex hex);

    boolean isDead();

    boolean isDefender();

    boolean isLord();

    boolean isDemiLord();

    boolean isRangestriker();

    boolean isTitan();

    void setDead(boolean dead);

    void setCurrentHex(BattleHex hex);

    void setHits(int hits);

    void setMoved(boolean moved);

    void setPoisonDamage(int damage);

    void setSlowed(int slowValue);

    void addPoisonDamage(int damage);

    void addSlowed(int slowValue);

    void setStruck(boolean struck);

    boolean useMagicMissile();

    boolean wouldDieFrom(int hits);

}
