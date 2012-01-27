package net.sf.colossus.server;


import java.util.logging.Logger;

import junit.framework.TestCase;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.TestConstants;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.variant.Variant;


public class StartDefaultTest extends TestCase
{
    private static final Logger LOGGER = Logger
        .getLogger(StartDefaultTest.class.getName());

    public StartDefaultTest(String name)
    {
        super(name);
    }

    private final static String CUSTOM_PATH = TestConstants.TEST_COLOSSUS_HOME;

    public void testStartDefault()
    {
        LOGGER.info("test: starting Default variant.");
        Options.setStartupTest(true);
        ErrorUtils.clearErrorDuringFunctionalTest();

        // noFile argument true, we never want the START options to be saved.
        Options startOptions = new Options(Constants.OPTIONS_START, true);

        WhatNextManager whatNextManager = new WhatNextManager(startOptions);

        // use colossus-home from resource directory.
        // Get -server.cf file from there.
        // The booleans are: noFile=false, readOnly=true
        Options serverOptions = new Options(Constants.OPTIONS_SERVER_NAME,
            CUSTOM_PATH, false, true);
        serverOptions.loadOptions();

        Variant variant = VariantSupport.loadVariantByName("Default", true);
        GameServerSide game = new GameServerSide(whatNextManager,
            serverOptions, variant);

        game.startNewGameAndWaitUntilOver(null);

        assertFalse("Starting game with Default variant encountered problems",
            ErrorUtils.checkErrorDuringFunctionalTest());

        LOGGER.info("OK: starting variant Default COMPLETED.");
    }

}
