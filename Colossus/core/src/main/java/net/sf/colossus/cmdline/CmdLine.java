package net.sf.colossus.cmdline;


import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * Class CmdLine represents a parsed command line for one run of a program.
 *
 * @author David Ripton
 */

/* The rules:

   You can choose '-' followed by the one-character short
   option name, or "--" followed by the long option name.
   No other switch characters are valid.

   If the option takes an argument, then it is required to
   provide it.  The next thing on the line will be taken
   as the argument, period.  If it's invalid, boom.

   You may choose to put a short option and its argument
   in the same string without a space, or you may choose
   to separate them with a space.  You may not put an '='
   in between.

   A long option must be separated from its argument using
   a space.  You may not use an '=' here either.

   Options must appear before non-options.  Once a non-option
   is found, we stop parsing and put all the rest into leftovers.
 */

public class CmdLine
{
    // Anything on the command line after the last valid option.
    private final ArrayList<String> leftovers = new ArrayList<String>();
    private final Map<Character, String> optchToValue = new TreeMap<Character, String>();
    private final Map<String, String> nameToValue = new TreeMap<String, String>();
    private final Set<Character> optchSeen = new TreeSet<Character>();
    private final Set<String> nameSeen = new TreeSet<String>();

    public CmdLine(Opts opts, String[] args)
    {
        boolean expectingValue = false;
        char optch = '\0';
        boolean inLeftovers = false;
        for (int ii = 0; ii < args.length; ii++)
        {
            String arg = args[ii];
            if (inLeftovers)
            {
                leftovers.add(arg);
            }
            else if (expectingValue)
            {
                Opt opt = opts.getOpt(optch);
                String name = opt.getName();
                String value = arg;
                optchToValue.put(new Character(optch), value);
                nameToValue.put(name, value);
                expectingValue = false;
            }
            else if (arg.startsWith("--"))
            {
                String name = arg.substring(2);
                Opt opt = opts.getOpt(name);
                optch = opt.getOptch();
                optchSeen.add(new Character(optch));
                nameSeen.add(name);
                expectingValue = opt.hasArg();
            }
            else if (arg.startsWith("-"))
            {
                optch = arg.charAt(1);
                Opt opt = opts.getOpt(optch);
                String name = opt.getName();
                optchSeen.add(new Character(optch));
                nameSeen.add(name);
                expectingValue = opt.hasArg();
                if (arg.length() > 2)
                {
                    if (expectingValue)
                    {
                        String value = arg.substring(2);
                        optchToValue.put(new Character(optch), value);
                        nameToValue.put(name, value);
                        expectingValue = false;
                    }
                    else
                    {
                        // Must be valid single-char options without args.
                        for (int jj = 2; jj < arg.length(); jj++)
                        {
                            optch = arg.charAt(jj);
                            opt = opts.getOpt(optch);
                            name = opt.getName();
                            optchSeen.add(new Character(optch));
                            nameSeen.add(name);
                            expectingValue = opt.hasArg();
                            if (expectingValue)
                            {
                                throw new RuntimeException(
                                    "can't clump options that expect arguments");
                            }
                        }
                    }
                }
            }
            else
            {
                leftovers.add(arg);
                inLeftovers = true;
            }
        }
        if (expectingValue)
        {
            throw new RuntimeException("last option never got its arg");
        }
    }

    /** Return true iff the option has been seen. */
    public boolean optIsSet(char optch)
    {
        return optchSeen.contains(new Character(optch));
    }

    /** Return true iff the option has been seen. */
    public boolean optIsSet(String name)
    {
        return nameSeen.contains(name);
    }

    /** Return the option value as a String.
     *  Will raise if the option has not been set or does
     *  not take an argument. */
    public String getOptValue(char optch)
    {
        return optchToValue.get(new Character(optch));
    }

    /** Return the option value as a String.
     *  Will raise if the option has not been set or does
     *  not take an argument. */
    public String getOptValue(String name)
    {
        return nameToValue.get(name);
    }

    /** Return an ArrayList of all leftover arguments that
     *  were found on the command line after valid options
     *  were parsed. */
    public ArrayList<String> getLeftovers()
    {
        return new ArrayList<String>(leftovers);
    }
}
