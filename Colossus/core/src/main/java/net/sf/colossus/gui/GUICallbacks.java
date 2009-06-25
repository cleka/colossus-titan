package net.sf.colossus.gui;


import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;


/**
 * Anything that happens in the GUI and that has effect to Client or Server,
 * especially all things that client shall send to server.
 */
public interface GUICallbacks
{

    public void leaveCarryMode();

    public void applyCarries(BattleHex hex);

    public void acquireAngelCallback(Legion legion, CreatureType angelType);

    public void answerFlee(Legion ally, boolean answer);

    public void answerConcede(Legion legion, boolean answer);

    public void doBattleMove(int tag, BattleHex hex);

    public void undoBattleMove(BattleHex hex);

    public void strike(int tag, BattleHex hex);

    public void doneWithBattleMoves();

    public void doneWithStrikes();

    public void concede();
}
