package net.sf.colossus.cmdline;

/** 
 *  Class Opt represents a single command-line option.
 *  @version $Id$
 *  @author David Ripton
 */

public class Opt
{
    private char optch;
    private String name;
    private boolean hasarg;
    private String help;

    public Opt(char optch, String name, boolean hasarg, String help)
    {
        this.optch = optch;
        this.name = name;
        this.hasarg = hasarg;
        this.help = help;
    }

    /** short option name */
    public char getOptch()
    {
        return optch;
    }

    /** long option name */
    public String getName()
    {
        return name;
    }

    /** whether the option takes an argument */
    public boolean hasArg()
    {
        return hasarg;
    }

    /** help string */
    public String getHelp()
    {
        return help;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("-" + optch);
        sb.append(", "); 
        sb.append("--" + name);
        sb.append(", "); 
        if (hasarg)
        {
            sb.append("+ ARG");
            sb.append(", "); 
        }
        sb.append(help);
        return sb.toString();
    }
}
