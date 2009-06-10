package net.sf.colossus.cmdline;


import java.util.Collection;
import java.util.TreeMap;


/**
 * Class Opts represents a configured group of command-line options
 * for a program.
 *
 * @author David Ripton
 */
public class Opts
{
    // maps the single char abbreviation to the Opt
    private final TreeMap<Character, Opt> optchToOpt = new TreeMap<Character, Opt>();

    // maps the long option name to the Opt
    private final TreeMap<String, Opt> nameToOpt = new TreeMap<String, Opt>();

    public Opts()
    {
        // Nothing to do... just to have a constructor here for completeness
    }

    /** Return a Collection of all my Opt objects. */
    public Collection<Opt> getOptions()
    {
        return optchToOpt.values();
    }

    /** Add one Opt. */
    public void addOption(char optch, String name, boolean hasarg, String help)
    {
        Opt opt = new Opt(optch, name, hasarg, help);
        optchToOpt.put(new Character(optch), opt);
        nameToOpt.put(name, opt);
    }

    /** Parse args and return a CmdLine. */
    public CmdLine parse(String[] args)
    {
        return new CmdLine(this, args);
    }

    /** Return the Opt corresponding to short option optch. */
    public Opt getOpt(char optch)
    {
        return optchToOpt.get(new Character(optch));
    }

    /** Return the Opt corresponding to long option name. */
    public Opt getOpt(String name)
    {
        return nameToOpt.get(name);
    }
}
