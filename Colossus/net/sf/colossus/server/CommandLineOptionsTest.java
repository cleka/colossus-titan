package net.sf.colossus.server;

import java.util.*;
import junit.framework.*;

import com.werken.opt.CommandLine;


/** JUnit test for command-line options. */
public class CommandLineOptionsTest extends TestCase
{
    public CommandLineOptionsTest(String name)
    {
        super(name);
    }

    public void testHelp()
    {
        String [] args = new String[9];
        args[0] = "-h";
        Start.main(args);
    }

    public void testLoad()
    {
        String [] args = new String[9];
        args[0] = "-l";
        args[1] = "savegamename";
        Start.main(args);
        // TODO Need to always have the savegame somewhere.
        fail("Not done yet");
    }

    public void testLoadLatest()
    {
        String [] args = new String[9];
        args[0] = "-z";
        Start.main(args);
        // TODO Need to always have the savegame somewhere.
        fail("Not done yet");
    }

    public void testStart()
    {
        String [] args = new String[9];
        args[0] = "-s";
        Start.main(args);
    }

    public void testStartWithVariant()
    {
        String [] args = new String[9];
        args[0] = "-s";
        args[1] = "-v";
        args[2] = "Outlands";
        Start.main(args);
        // TODO Need to implement variant loading.
        fail("Not done yet");
    }

    public void testStartWithAIs()
    {
        String [] args = new String[9];
        args[0] = "-s";
        args[1] = "-i";
        args[2] = "6";
        Start.main(args);
    }

    public void testStartWithHumans()
    {
        String [] args = new String[9];
        args[0] = "-s";
        args[1] = "-u";
        args[2] = "6";
        Start.main(args);
    }

    public void testStartWithHumansAndAIs()
    {
        String [] args = new String[9];
        args[0] = "-s";
        args[1] = "-u";
        args[2] = "2";
        args[3] = "-i";
        args[4] = "2";
        Start.main(args);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(CommandLineOptionsTest.class);
        return suite;
    }

    public static void main(String [] args) 
    {
        junit.textui.TestRunner.run(suite());
    }
}
