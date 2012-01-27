package net.sf.colossus.server;


import java.util.logging.Logger;

import junit.framework.TestCase;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.TestConstants;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.variant.Variant;


public class StartEachVariant extends TestCase
{
    private static final Logger LOGGER = Logger
        .getLogger(StartEachVariant.class.getName());

    public StartEachVariant(String name)
    {
        super(name);
    }

    public void testDefault()
    {
        testStartOneVariant("Default", 6);
    }

    public void testAbyssal3()
    {
        testStartOneVariant("Abyssal3", 3);
    }

    public void testAbyssal6()
    {
        testStartOneVariant("Abyssal6", 6);
    }

    /*
    public void testIllegalVariant()
    {
        testStartOneVariant("NoSuchVariant", 6);
    }
    */

    // TODO nrOfAIs still dummy...
    private void testStartOneVariant(String variantName, int nrOfAIs)
    {
        LOGGER.info("test: starting variant " + variantName + " with "
            + nrOfAIs + "AIs");

        Options.setStartupTest(true);
        ErrorUtils.clearErrorDuringFunctionalTest();

        // noFile argument true, we never want the START options to be saved.
        Options startOptions = new Options(Constants.OPTIONS_START, true);

        WhatNextManager whatNextManager = new WhatNextManager(startOptions);

        // use colossus-home from resource directory.
        // Get -server.cf file from there.
        // The booleans are: noFile=false, readOnly=true
        Options serverOptions = new Options(Constants.OPTIONS_SERVER_NAME,
            TestConstants.TEST_COLOSSUS_HOME, false, true);

        serverOptions.loadOptions();
        serverOptions.setOption(Options.variant, variantName);
        Variant variant = VariantSupport.loadVariantByName(variantName, true);

        GameServerSide game = new GameServerSide(whatNextManager,
            serverOptions, variant);
        game.startNewGameAndWaitUntilOver(null);

        assertFalse("Starting game with variant " + variantName + " failed!",
            ErrorUtils.checkErrorDuringFunctionalTest());

        LOGGER.info("OK: starting variant Default COMPLETED.");
    }

}
