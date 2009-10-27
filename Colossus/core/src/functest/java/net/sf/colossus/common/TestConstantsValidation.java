package net.sf.colossus.common;


import java.util.logging.Logger;

import junit.framework.TestCase;


public class TestConstantsValidation extends TestCase
{
    private static final Logger LOGGER = Logger
        .getLogger(TestConstantsValidation.class.getName());

    private final static String EXPECTED_VARIANT = "Default";

    public void testPathes()
    {
        // The booleans are: noFile=false, readOnly=true
        Options serverOptions = new Options(Constants.OPTIONS_SERVER_NAME,
            TestConstants.TEST_COLOSSUS_HOME, false, true);

        serverOptions.loadOptions();
        String variantName = serverOptions.getStringOption(Options.variant);

        LOGGER.info("Got as variant name from server options: " + variantName);

        boolean allOk = (variantName != null && variantName
            .equals(EXPECTED_VARIANT));

        assertTrue("The file Colossus-server.cf from resources/colossus-home "
            + "should have as variant have set '" + EXPECTED_VARIANT
            + "' but it has '" + variantName + "'!", allOk);
    }

}
