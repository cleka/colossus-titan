package net.sf.colossus.cmdline;


import junit.framework.TestCase;


/**
 * JUnit test for cmdline package.
 *
 * @author David Ripton
 */
public class CmdLineTest extends TestCase
{
    public void testEmptyOpts()
    {
        Opts opts = new Opts();
        assertTrue(opts.getOptions().size() == 0);
        assertEquals(opts.getOpt('a'), null);
    }

    public void testAddOption()
    {
        Opts opts = new Opts();
        opts.addOption('v', "verbose", false, "log more stuff");
        assertTrue(opts.getOptions().size() == 1);

        Opt opt = opts.getOpt('v');
        assertEquals(opt.getOptch(), 'v');
        assertEquals(opt.getName(), "verbose");
        assertEquals(opt.hasArg(), false);
        assertEquals(opt.getHelp(), "log more stuff");
        assertEquals(opt.toString(), "-v, --verbose, log more stuff");

        opt = opts.getOpt("verbose");
        assertEquals(opt.getOptch(), 'v');
        assertEquals(opt.getName(), "verbose");
        assertEquals(opt.hasArg(), false);
        assertEquals(opt.getHelp(), "log more stuff");
        assertEquals(opt.toString(), "-v, --verbose, log more stuff");
    }

    public void testAddOptionWithArg()
    {
        Opts opts = new Opts();
        opts.addOption('m', "myname", true, "My player name");
        assertTrue(opts.getOptions().size() == 1);

        Opt opt = opts.getOpt('m');
        assertEquals(opt.getOptch(), 'm');
        assertEquals(opt.getName(), "myname");
        assertEquals(opt.hasArg(), true);
        assertEquals(opt.getHelp(), "My player name");
        assertEquals(opt.toString(), "-m, --myname, + ARG, My player name");

        opt = opts.getOpt("myname");
        assertEquals(opt.getOptch(), 'm');
        assertEquals(opt.getName(), "myname");
        assertEquals(opt.hasArg(), true);
        assertEquals(opt.getHelp(), "My player name");
        assertEquals(opt.toString(), "-m, --myname, + ARG, My player name");
    }

    public void testParse()
    {
        Opts opts = new Opts();
        opts.addOption('v', "verbose", false, "log more stuff");
        opts.addOption('p', "port", true, "server port number");
        String[] args = { "-p", "1234", "-v" };
        CmdLine cl = opts.parse(args);
        assertTrue(cl.optIsSet('p'));
        assertTrue(cl.optIsSet("port"));
        assertTrue(cl.optIsSet('v'));
        assertTrue(cl.optIsSet("verbose"));
        assertEquals(cl.getOptValue('p'), "1234");
        assertEquals(cl.getOptValue("port"), "1234");

        String[] args2 = { "--port", "1234", "--verbose" };
        cl = opts.parse(args2);
        assertTrue(cl.optIsSet('p'));
        assertTrue(cl.optIsSet("port"));
        assertTrue(cl.optIsSet('v'));
        assertTrue(cl.optIsSet("verbose"));
        assertEquals(cl.getOptValue('p'), "1234");
        assertEquals(cl.getOptValue("port"), "1234");

        String[] args3 = { "-p1234", "--verbose" };
        cl = opts.parse(args3);
        assertTrue(cl.optIsSet('p'));
        assertTrue(cl.optIsSet("port"));
        assertTrue(cl.optIsSet('v'));
        assertTrue(cl.optIsSet("verbose"));
        assertEquals(cl.getOptValue('p'), "1234");
        assertEquals(cl.getOptValue("port"), "1234");
    }

    public void testLeftovers()
    {
        Opts opts = new Opts();
        opts.addOption('v', "verbose", false, "log more stuff");
        opts.addOption('p', "port", true, "server port number");
        String[] args = { "-p", "1234", "-v", "hi", "mom" };
        CmdLine cl = opts.parse(args);
        assertTrue(cl.optIsSet('p'));
        assertTrue(cl.optIsSet("port"));
        assertTrue(cl.optIsSet('v'));
        assertTrue(cl.optIsSet("verbose"));
        assertEquals(cl.getOptValue('p'), "1234");
        assertEquals(cl.getOptValue("port"), "1234");
        assertEquals(cl.getLeftovers().size(), 2);
        assertEquals(cl.getLeftovers().get(0), "hi");
        assertEquals(cl.getLeftovers().get(1), "mom");
    }

    public void testCombinedNonArgOptions()
    {
        Opts opts = new Opts();
        opts.addOption('v', "verbose", false, "log more stuff");
        opts.addOption('z', "latest", false, "load latest savegame");
        String[] args = { "-vz" };
        CmdLine cl = opts.parse(args);
        assertTrue(cl.optIsSet('v'));
        assertTrue(cl.optIsSet("verbose"));
        assertTrue(cl.optIsSet('z'));
        assertTrue(cl.optIsSet("latest"));
        assertEquals(cl.getLeftovers().size(), 0);
    }
}
