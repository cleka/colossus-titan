package net.sf.colossus.cmdline;

import junit.framework.TestCase;
import net.sf.colossus.cmdline.CmdLine;
import net.sf.colossus.cmdline.Opt;
import net.sf.colossus.cmdline.Opts;


/** 
 *  JUnit test for cmdline package.
 *  @version $Id$
 *  @author David Ripton
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
        opt = opts.getOpt("verbose");
        assertEquals(opt.getOptch(), 'v');
        assertEquals(opt.getName(), "verbose");
        assertEquals(opt.hasArg(), false);
        assertEquals(opt.getHelp(), "log more stuff");
    }

    public void testParse()
    {
        Opts opts = new Opts();
        opts.addOption('v', "verbose", false, "log more stuff");
        opts.addOption('p', "port", true, "server port number");
        String [] args = {"-p", "1234", "-v"};
        CmdLine cl = opts.parse(args);
        assertTrue(cl.optIsSet('p'));
        assertTrue(cl.optIsSet("port"));
        assertTrue(cl.optIsSet('v'));
        assertTrue(cl.optIsSet("verbose"));
        assertEquals(cl.getOptValue('p'), "1234");
        assertEquals(cl.getOptValue("port"), "1234");

        String [] args2 = {"--port", "1234", "--verbose"};
        cl = opts.parse(args2);
        assertTrue(cl.optIsSet('p'));
        assertTrue(cl.optIsSet("port"));
        assertTrue(cl.optIsSet('v'));
        assertTrue(cl.optIsSet("verbose"));
        assertEquals(cl.getOptValue('p'), "1234");
        assertEquals(cl.getOptValue("port"), "1234");

        String [] args3 = {"-p1234", "--verbose"};
        cl = opts.parse(args3);
        assertTrue(cl.optIsSet('p'));
        assertTrue(cl.optIsSet("port"));
        assertTrue(cl.optIsSet('v'));
        assertTrue(cl.optIsSet("verbose"));
        assertEquals(cl.getOptValue('p'), "1234");
        assertEquals(cl.getOptValue("port"), "1234");
    }
}
