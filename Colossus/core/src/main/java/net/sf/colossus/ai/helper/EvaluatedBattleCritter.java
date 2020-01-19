package net.sf.colossus.ai.helper;


import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;


/**
 *
 * @author Romain Dolbeau
 */
public class EvaluatedBattleCritter implements BattleCritter
{
    private final BattleCritter parent;
    private BattleHex startingHex;
    private BattleHex currentHex;

    EvaluatedBattleCritter(BattleCritter parent)
    {
        this.parent = parent;
        startingHex = parent.getStartingHex();
        currentHex = parent.getCurrentHex();
    }

    public CreatureType getType()
    {
        return parent.getType();
    }

    public BattleHex getCurrentHex()
    {
        return currentHex;
    }

    public String getDescription()
    {
        return parent.getDescription();
    }

    public int getHits()
    {
        return parent.getHits();
    }

    public int getPoison()
    {
        return parent.getPoison();
    }

    public int getPoisonDamage()
    {
        return parent.getPoisonDamage();
    }

    public void addPoisonDamage(int damage)
    {
        parent.addPoisonDamage(damage);
    }

    public void setPoisonDamage(int damage)
    {
        parent.setPoisonDamage(damage);
    }

    public int getSlowed()
    {
        return parent.getSlowed();
    }

    public void setSlowed(int slowValue)
    {
        parent.setSlowed(slowValue);
    }

    public void addSlowed(int slowValue)
    {
        parent.addSlowed(slowValue);
    }

    public int getSlows()
    {
        return parent.getSlows();
    }

    public int getPointValue()
    {
        return parent.getPointValue();
    }

    public int getPower()
    {
        return parent.getPower();
    }

    public int getSkill()
    {
        return parent.getSkill();
    }

    public BattleHex getStartingHex()
    {
        return startingHex;
    }

    public int getTag()
    {
        return parent.getTag();
    }

    public int getTitanPower()
    {
        return parent.getTitanPower();
    }

    public boolean hasMoved()
    {
        return !currentHex.equals(startingHex);
    }

    public boolean hasStruck()
    {
        return parent.hasStruck();
    }

    public void moveToHex(BattleHex hex)
    {
        startingHex = currentHex;
        currentHex = hex;
    }

    public boolean isDead()
    {
        return parent.isDead();
    }

    public boolean isDefender()
    {
        return parent.isDefender();
    }

    public boolean isRangestriker()
    {
        return parent.isRangestriker();
    }

    public boolean isLord()
    {
        return parent.isLord();
    }

    public boolean isDemiLord()
    {
        return parent.isDemiLord();
    }

    public boolean isTitan()
    {
        return parent.isTitan();
    }

    public void setDead(boolean dead)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setCurrentHex(BattleHex hex)
    {
        currentHex = hex;
    }

    public void setHits(int hits)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setMoved(boolean moved)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setStruck(boolean struck)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean useMagicMissile()
    {
        return parent.useMagicMissile();
    }

    public boolean wouldDieFrom(int hits)
    {
        return parent.wouldDieFrom(hits);
    }
}
