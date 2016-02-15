package net.sf.colossus.webserver;

import java.util.logging.Logger;

import junit.framework.TestCase;
import net.sf.colossus.webcommon.GameInfo;

public class PortBookKeeperTest extends TestCase
{
    private static final Logger LOGGER = Logger
        .getLogger(PortBookKeeperTest.class.getName());

    private PortBookKeeper bookKeeper;
    GameInfo gi;

    /**
     * This is not really testing anything... just an easy way to
     * make it get some ports and print them.
     */
    public PortBookKeeperTest()
    {
        // TODO Auto-generated constructor stub
    }

    public PortBookKeeperTest(String name)
    {
        super(name);
        // TODO Auto-generated constructor stub

        gi = new GameInfo("10000", true);
        this.bookKeeper = new PortBookKeeper(32768, 20);
    }

    public void testGetSomePorts()
    {
        int port;
        port = bookKeeper.getFreePort(gi);
        LOGGER.info("Got port " + port);

        port = bookKeeper.getFreePort(gi);
        LOGGER.info("Got port " + port);

        port = bookKeeper.getFreePort(gi);
        LOGGER.info("Got port " + port);

        assertTrue(bookKeeper != null);
    }
}
