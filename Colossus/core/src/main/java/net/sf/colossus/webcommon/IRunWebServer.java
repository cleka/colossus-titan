package net.sf.colossus.webcommon;


/**
 *  Interface for GameInfo, what it needs to communicate with
 *  WebServer regarding (so far only) ending a game.
 *  The functionality is needed only on Server side, but
 *  GameInfo also goes to Client ( = main Colossus.jar) side
 *  and I don't want to deliver all Web server stuff inside
 *  the main jar.
 *  
 *  @version $Id$
 *  @author Clemens Katzer
 */

public interface IRunWebServer
{
    public void tellEnrolledGameStartsSoon(GameInfo gi);

    public void tellEnrolledGameStartsNow(GameInfo gi, String host, int port);

    public void tellEnrolledGameStarted(GameInfo gi);

    public void allTellGameInfo(GameInfo gi);

    public void gameFailed(GameInfo gi, String reason);

    public void unregisterGame(GameInfo gi, int port);
}
