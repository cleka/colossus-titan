
package net.sf.colossus.protocol;

public interface GameListener
{
    public void requestDenied(GameEvent evt);
    public void playerChanged(PlayerEvent evt);
    public void legionChange(LegionEvent evt);
    public void masterBoardMove(MasterBoardEvent evt);
    public void battleBoardMove(BattleBoardEvent evt);
    public void messageSent(MessageEvent evt);
}
