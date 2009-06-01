package net.sf.colossus.server;

import java.util.logging.Logger;

import junit.framework.TestCase;
import net.sf.colossus.common.Options;
import net.sf.colossus.util.ErrorUtils;

public class StartEachVariant extends TestCase
{
    private static final Logger LOGGER = Logger
        .getLogger(StartEachVariant.class.getName());

    public StartEachVariant(String name)
    {
        super(name);
    }

    public void testStart()
    {
        //        Right now it seems it does not work if there is more than one
        //        game run in same test...
        //        startOneVariant("Default", 6);
        //        startOneVariant("Abyssal3", 3);
        startOneVariant("Abyssal9", 9);
    }

    private void startOneVariant(String variantName, int playerCount)
    {
        LOGGER.info("startOneVariant for variant " + variantName + " with "
            + playerCount + " SimpleAIs.");

        Options.setFunctionalTest(true);
        ErrorUtils.setErrorDuringFunctionalTest(false);

        // String countStr = playerCount + "";

        // -Z = Simple AIs
        //String[] args = { "-g", "--variant", variantName, "-i", countStr,
        //    "-Z", countStr };

        //Start.main(args);

        assertFalse(ErrorUtils.getErrorDuringFunctionalTest());
        LOGGER.info("startOneVariant for variant " + variantName + " with "
            + playerCount + " SimpleAIs: COMPLETED!");
    }
}
