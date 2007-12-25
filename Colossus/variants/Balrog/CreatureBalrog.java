package Balrog;


/**
 * Custom class implementing the Balrog Creature. It is a DemiLord yet isn't immortal, and it's Image Name is Balrog no matter what is it's Creature Name.
 * @version $Id$
 * @author Romain Dolbeau
 */
public class CreatureBalrog extends net.sf.colossus.server.Creature
{
    private int localMaxCount;

    public CreatureBalrog(String name, Integer power, Integer skill,
        Boolean rangestrikes, Boolean flies, Boolean nativeBramble,
        Boolean nativeDrift, Boolean nativeBog, Boolean nativeSandDune,
        Boolean nativeSlope, Boolean nativeVolcano, Boolean nativeRiver,
        Boolean nativeStone, Boolean nativeTree, Boolean waterDwelling,
        Boolean magicMissile, Boolean summonable, Boolean lord,
        Boolean demilord, Integer maxCount, String pluralName, String baseColor)
    {
        super(name, power.intValue(), skill.intValue(), rangestrikes
            .booleanValue(), flies.booleanValue(), nativeBramble
            .booleanValue(), nativeDrift.booleanValue(), nativeBog
            .booleanValue(), nativeSandDune.booleanValue(), nativeSlope
            .booleanValue(), nativeVolcano.booleanValue(), nativeRiver
            .booleanValue(), nativeStone.booleanValue(), nativeTree
            .booleanValue(), waterDwelling.booleanValue(), magicMissile
            .booleanValue(), summonable.booleanValue(), lord.booleanValue(),
            demilord.booleanValue(), maxCount.intValue(), pluralName,
            baseColor);
        localMaxCount = maxCount.intValue();
    }

    public boolean isImmortal()
    { // demilord yet not immortal
        return false;
    }

    public String getImageName()
    {
        return "Balrog";
    }

    public String getDisplayName()
    {
        return "Balrog";
    }

    public int getMaxCount()
    {
        return localMaxCount;
    }

    void setNewMaxCount(int count)
    {
        this.localMaxCount = count;
    }
}
