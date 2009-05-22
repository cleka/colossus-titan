package net.sf.colossus.server;

import java.util.logging.Logger;

import junit.framework.TestCase;

public class LoadGameTest extends TestCase
{
    private static final Logger LOGGER = Logger.getLogger(LoadGameTest.class
        .getName());

    public static final String SAVED_GAMES_TEST_DIR = "./core/src/functest/testdata/saves/";

    public LoadGameTest(String testName)
    {
        super(testName);
    }

    public void testLoadOneGame()
    {
        String fileName = makeFullPath("Simple-6-players.xml");
        LOGGER.info("Filename: " + fileName);
        String[] args = { "--load", fileName };

        Start.main(args);
    }

    private String makeFullPath(String fileName)
    {
        return SAVED_GAMES_TEST_DIR + fileName;
    }
}
