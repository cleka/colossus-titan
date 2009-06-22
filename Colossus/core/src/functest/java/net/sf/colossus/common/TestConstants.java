package net.sf.colossus.common;


/**
 * Some constants (so far, some directory pathes) used for testing
 *
 * @author Clemens Katzer
 */
public class TestConstants
{

    /**
     * To use as colossus-home (instead of $HOME/.colossus);
     * Used both for cfg files and as base path for the saves/ directory
     */
    public static final String TEST_COLOSSUS_HOME = "./core/src/functest/"
        + "resource/colossus-home";

    /**
     * The directory in which saved games (snapshots) are to be saved to or
     * loaded from
     */
    public static final String SAVED_GAMES_TEST_DIR = TEST_COLOSSUS_HOME
        + "/saves";

}
