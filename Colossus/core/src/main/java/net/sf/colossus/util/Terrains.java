/*
 * Created on 31.12.2003
 */
package net.sf.colossus.util;

/**
 * Terrain type of hexes.
 *
 * There are two different kinds of hexes -- as you well know.
 * You must not confuse them.
 * - T_xxx constants are for the MASTER BOARD terrain types. TODO.
 * - H_xxx constants are for the HAZARD (or battle hex) terrain types
 * and of course there are the battle hex border types. TODO.
 *
 * Use the constants from this interface best by "implementing"
 * the interface in the class that wants to access them.
 *
 * @author Towi
 * @version $Id$
 */
public interface Terrains
{
    //
    // BattleHex terrain types, aka Hazard Terrain rypes, are:
    //    p, r, s, t, o, v, d, w
    //    plain, bramble, sand, tree, bog, volcano, drift, tower
    // also
    //   l, n
    //   lake, stone
    //
    /** hazard terrain name Plains */
    String H_PLAINS   = "Plains";
    /** hazard terrain name Brambles */
    String H_BRAMBLES = "Brambles";
    /** hazard terrain name Sand */
    String H_SAND     = "Sand";
    /** hazard terrain name Tree */
    String H_TREE     = "Tree";
    /** hazard terrain name Bog */
    String H_BOG      = "Bog";
    /** hazard terrain name Volcano */
    String H_VOLCANO  = "Volcano";
    /** hazard terrain name Drift */
    String H_DRIFT    = "Drift";
    /** hazard terrain name Tower */
    String H_TOWER    = "Tower";
    /** hazard terrain name Stone */
    String H_STONE    = "Stone";
    /** hazard terrain name Lake, in some variants */
    String H_LAKE     = "Lake";

    /**
     * The array of all the valid terrain type for a BattleHex.
     * TODO: in Java 1.5 make an enum of this.
     */
    String[] ALL_HAZARD_TERRAINS = {
        H_PLAINS, H_TOWER, H_BRAMBLES,
        H_SAND, H_TREE, H_BOG, H_VOLCANO, H_DRIFT,
        H_LAKE, H_STONE };

}
