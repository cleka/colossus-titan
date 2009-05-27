package net.sf.colossus.ai;

/**
 * Various constants used by the AIs code for battle evaluation.
 * Each specific AI should be able to override them
 * to tweak the evaluation results w/o rewriting the code.
 * So if an AI needs to modify one or more of those, feel free to
 * remove the 'final' from the appropriate variable(s);
 */
class BattleEvalConstants
{
    /* per critter */
    /** Will be multiplied by point value of creature */
    int OFFBOARD_DEATH_SCALE_FACTOR = -1500;
    /** Straight value */
    final int NATIVE_BONUS_TERRAIN = 40;
    // 50 -- old value
    /** Straight value */
    final int NATIVE_BOG = 20;
    /** Straight value */
    final int NON_NATIVE_PENALTY_TERRAIN = -100;
    /** Multiplied by damage (drift is 1) */
    final int PENALTY_DAMAGE_TERRAIN = -200;
    /** Straight value */
    final int FIRST_RANGESTRIKE_TARGET = 300;
    /** Straight value */
    final int EXTRA_RANGESTRIKE_TARGET = 100;
    /** Straight value */
    final int RANGESTRIKE_TITAN = 500;
    /** Straight value */
    final int RANGESTRIKE_WITHOUT_PENALTY = 100;
    /** Straight value */
    final int ATTACKER_ADJACENT_TO_ENEMY = 400;
    /** Straight value */
    final int DEFENDER_ADJACENT_TO_ENEMY = -20;
    /** Straight value */
    final int ADJACENT_TO_ENEMY_TITAN = 1300;
    /** Straight value */
    final int ADJACENT_TO_RANGESTRIKER = 500;
    /** Multiplied by Kill Value (Point Value + stuff!) */
    final int ATTACKER_KILL_SCALE_FACTOR = 25;
    // 100
    /** Multiplied by Kill Value (Point Value + stuff!) */
    final int DEFENDER_KILL_SCALE_FACTOR = 1;
    // 100
    /** Multiplied by a number of creatures */
    final int KILLABLE_TARGETS_SCALE_FACTOR = 0;
    // 10
    /** Multiplied by Kill Value (Point Value + stuff!) */
    final int ATTACKER_GET_KILLED_SCALE_FACTOR = -20;
    /** Multiplied by Kill Value (Point Value + stuff!) */
    final int DEFENDER_GET_KILLED_SCALE_FACTOR = -40;
    /** Multiplied by Kill Value (Point Value + stuff!) */
    final int ATTACKER_GET_HIT_SCALE_FACTOR = -1;
    /** Multiplied by Kill Value (Point Value + stuff!) */
    final int DEFENDER_GET_HIT_SCALE_FACTOR = -2;
    /** Multiplied by hex elevation */
    final int TITAN_TOWER_HEIGHT_BONUS = 2000;
    /** Multiplied by hex elevation */
    final int DEFENDER_TOWER_HEIGHT_BONUS = 80;
    /** Multiplied by the distance to entrance */
    final int TITAN_FORWARD_EARLY_PENALTY = -5000;
    /** Multiplied by the number of relevant hex */
    final int TITAN_BY_EDGE_OR_BLOCKINGHAZARD_BONUS = 400;
    /** Multiplied by the number of relevant hex */
    int DEFENDER_BY_EDGE_OR_BLOCKINGHAZARD_BONUS = 1;
    /** Multiplied by the number of relevant hex */
    int DEFENDER_BY_DAMAGINGHAZARD_BONUS = 1;
    /** Multiplied by the distance to 2nd row */
    final int DEFENDER_FORWARD_EARLY_PENALTY = -60;
    /** Multiplied by the distance to enemy */
    final int ATTACKER_DISTANCE_FROM_ENEMY_PENALTY = -300;
    /** Multiplied by the number of relevant hex */
    final int ADJACENT_TO_BUDDY = 100;
    /** Multiplied by the number of relevant hex */
    final int ADJACENT_TO_BUDDY_TITAN = 600;
    // 200
    /** Straight value */
    final int GANG_UP_ON_CREATURE = 50;
    /* per legion */
    /** Bonus when no defender will be reachable by the attacker
     * next half-turn.
     */
    final int DEF__NOBODY_GETS_HURT = 2000;
    /** Bonus when no defender will be reachable by more than one
     * attacker next half-turn.
     */
    final int DEF__NOONE_IS_GANGBANGED = 200;
    /** Bonus when at most one defender will be reachable by the
     * attacker next half-turn.
     */
    final int DEF__AT_MOST_ONE_IS_REACHABLE = 100;
}
