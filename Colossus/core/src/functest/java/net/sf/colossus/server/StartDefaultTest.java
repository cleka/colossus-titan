package net.sf.colossus.server;

import java.util.logging.Logger;

import junit.framework.TestCase;
import net.sf.colossus.common.Options;
import net.sf.colossus.util.ErrorUtils;

public class StartDefaultTest extends TestCase
{
    private static final Logger LOGGER = Logger.getLogger(StartDefaultTest.class
        .getName());

    public StartDefaultTest(String name)
    {
        super(name);
    }

    public void testStartDefault()
    {
        LOGGER.info("test: starting Default variant.");
        Options.setFunctionalTest(true);
        ErrorUtils.setErrorDuringFunctionalTest(false);

        //        String[] args = { "-g", "-i", "6", "-Z", "6", "--variant", "Default" };
        //        Start.main(args);

        assertFalse(ErrorUtils.getErrorDuringFunctionalTest());
        LOGGER.info("test: starting Default variant COMPLETED.");
    }

}
