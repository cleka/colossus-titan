package net.sf.colossus.variant;


import java.util.HashMap;
import java.util.Map;


/**
 * Stores the skill and power bonuses for a single terrain.
 *
 * For internal use only, so we don't bother with encapsulation.
 */
public class TerrainBonuses
{
    final int attackerPower;
    private final int defenderPower;
    private final int attackerSkill;
    private final int defenderSkill;

    TerrainBonuses(int attackerPower, int defenderPower,
        int attackerSkill, int defenderSkill)
    {
        this.attackerPower = attackerPower;
        this.defenderPower = defenderPower;
        this.attackerSkill = attackerSkill;
        this.defenderSkill = defenderSkill;
    }

    public int getAttackerPower()
    {
        return this.attackerPower;
    }

    public int getAttackerSkill()
    {
        return attackerSkill;
    }

    public int getDefenderSkill()
    {
        return defenderSkill;
    }

    public int getDefenderPower()
    {
        return defenderPower;
    }

    /**
     * Maps the terrain names to their matching bonuses.
     *
     * Only the terrains that have bonuses are in this map, so
     * users have to expect to retrieve null values. Note that
     * the terrain names include names for master board and
     * hazard terrains, so it can be used for lookup up either
     * type.
     *
     * TODO there seems to be some overlap with
     * {@link HazardTerrain#isNativeBonusTerrain()} and
     * {@link HazardTerrain#isNonNativePenaltyTerrain()}.
     *
     * This is a Map<String,TerrainBonuses>.
     *
     * TODO: this shouldn't be here, this is a property of the Variant player
     * (well, not yet for Hazard, but it should be, someday).
     * Actually, this doesn't make sense to me (RD). tower has bonus for
     * both attacker & defender (because of walls, I assume), but is special-
     * cased for attacker & defender. Brush and Jungle assumes Brushes, but
     * Jungle has Tree, too. And the comments themselves makes clear that
     * 'Sand' is actually 'Slope', but mixing up Attacker & Native and Defender
     * & non-native.
     * This and calcBonus should be reviewed thoroughly.
     */
    public static final Map<String, TerrainBonuses> TERRAIN_BONUSES = new HashMap<String, TerrainBonuses>();
    static
    {
        // strike down wall, defender strike up
        TERRAIN_BONUSES.put("Tower", new TerrainBonuses(0, 0, 1, 1));
        // native in bramble has skill to hit increased by 1
        TERRAIN_BONUSES.put("Brush", new TerrainBonuses(0, 0, 0, 1));
        TERRAIN_BONUSES.put("Jungle", new TerrainBonuses(0, 0, 0, 1));
        TERRAIN_BONUSES.put("Brambles", new TerrainBonuses(0, 0, 0, 1));
        // native gets an extra die when attack down slope
        // non-native loses 1 skill when attacking up slope
        TERRAIN_BONUSES.put("Hills", new TerrainBonuses(1, 0, 0, 1));
        // native gets an extra 2 dice when attack down dune
        // non-native loses 1 die when attacking up dune
        TERRAIN_BONUSES.put("Desert", new TerrainBonuses(2, 1, 0, 0));
        TERRAIN_BONUSES.put("Sand", new TerrainBonuses(2, 1, 0, 0));
        // Native gets extra 1 die when attack down slope
        // non-native loses 1 skill when attacking up slope
        TERRAIN_BONUSES.put("Mountains", new TerrainBonuses(1, 0, 0, 1));
        TERRAIN_BONUSES.put("Volcano", new TerrainBonuses(1, 0, 0, 1));
        // the other types have only movement bonuses
    }
}
