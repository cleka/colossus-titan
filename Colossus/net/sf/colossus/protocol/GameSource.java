
package net.sf.colossus.protocol;

/**
 * Utility class to manage event stuff
 */

import java.util.*;

public class GameSource
{
    private ArrayList oListenerArrayList = new ArrayList();

    public void addGameListener(GameListener oListener)
	{
	    oListenerArrayList.add(oListener);
	}

    public void removeGameListener(GameListener oListener)
	{
	    oListenerArrayList.remove(oListener);
	}

    public void fireEvent(GameEvent evt)
	{
	    Iterator oListeners = oListenerArrayList.iterator();
	    while(oListeners.hasNext())
	    {
		GameListener oListener = (GameListener) oListeners.next();
		oListener.requestDenied(evt);
	    }
	}

    public void fireEvent(PlayerEvent evt)
	{
	    Iterator oListeners = oListenerArrayList.iterator();
	    while(oListeners.hasNext())
	    {
		GameListener oListener = (GameListener) oListeners.next();
		oListener.playerChanged(evt);
	    }
	}

    public void fireEvent(LegionEvent evt)
	{
	    Iterator oListeners = oListenerArrayList.iterator();
	    while(oListeners.hasNext())
	    {
		GameListener oListener = (GameListener) oListeners.next();
		oListener.legionChange(evt);
	    }
	}
    public void fireEvent(MasterBoardEvent evt)
	{
	    Iterator oListeners = oListenerArrayList.iterator();
	    while(oListeners.hasNext())
	    {
		GameListener oListener = (GameListener) oListeners.next();
		oListener.masterBoardMove(evt);
	    }
	}

    public void fireEvent(BattleBoardEvent evt)
	{
	    Iterator oListeners = oListenerArrayList.iterator();
	    while(oListeners.hasNext())
	    {
		GameListener oListener = (GameListener) oListeners.next();
		oListener.battleBoardMove(evt);
	    }
	}

    public void fireEvent(MessageEvent evt)
	{
	    Iterator oListeners = oListenerArrayList.iterator();
	    while(oListeners.hasNext())
	    {
		GameListener oListener = (GameListener) oListeners.next();
		oListener.messageSent(evt);
	    }
	}

}
