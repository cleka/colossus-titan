package net.sf.colossus.server;


import java.util.logging.Logger;

import net.sf.colossus.common.IStartHandler;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;


public class StartGameForWebclient implements IStartHandler
{
    private static final Logger LOGGER = Logger
        .getLogger(StartGameForWebclient.class.getName());

    private final WhatNextManager whatNextManager;
    private GameServerSide game;
    private String username;

    public StartGameForWebclient(WhatNextManager whatNextMgr)
    {
        LOGGER.info("Instance created");
        this.whatNextManager = whatNextMgr;
    }

    public void startWebGameLocally(Options presetOptions, String username)
    {
        this.game = new GameServerSide(whatNextManager, presetOptions, null);
        this.username = username;

        // initServer does not return before it has accepted all clients,
        // so we have to run it inside a runnable
        Runnable doNewGame = new Runnable()
        {
            public void run()
            {
                getGame().startNewGameAndWaitUntilOver(getHostingUsername());
            }
        };
        new Thread(doNewGame).start();
    }

    private GameServerSide getGame()
    {
        return this.game;
    }

    private String getHostingUsername()
    {
        return this.username;
    }
}
