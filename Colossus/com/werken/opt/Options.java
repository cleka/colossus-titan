
package com.werken.opt;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Collections;

/** <p>Main entry-point into the <code>werken.opt</code> library.</p>

    <p>Options represents a collection of {@link Option} objects, which
    describe the possible options for a command-line.<p>

    <p>It may flexibly parse long and short options, with or without
    values.  Additionally, it may parse only a portion of a commandline,
    allowing for flexible multi-stage parsing.<p>

    @see com.werken.opt.CommandLine

    @author bob mcwhirter (bob @ werken.com)
*/
public class Options
{
    private List _options   = new ArrayList();
    private Map  _shortOpts = new HashMap();
    private Map  _longOpts  = new HashMap();

    /** <p>Construct a new Options descriptor</p>
     */
    public Options()
    {

    }

    /** <p>Add an option that only contains a short-name</p>
        <p>It may be specified as requiring an argument.</p>

        @param opt Short single-character name of the option.
        @param hasArg flag signally if an argument is required
        @param description Self-documenting description
     */
    public Options addOption(char opt,
                             boolean hasArg,
                             String description) throws DuplicateOptionException
    {
        addOption( new Option(opt,
                              hasArg,
                              description) );

        return this;
    }

    /** <p>Add an option that contains a short-name and a long-name</p>
        <p>It may be specified as requiring an argument.</p>

        @param opt Short single-character name of the option.
        @param hasArg flag signally if an argument is required
        @param description Self-documenting description
    */
    public Options addOption(char opt,
                             String longOpt,
                             boolean hasArg,
                             String description) throws DuplicateOptionException
    {
        addOption( new Option(opt,
                              longOpt,
                              hasArg,
                              description) );
        
        return this;
    }

    /** <p>Parse the given list of arguments against this descriptor<p>

        @param args Args to parse 
        
        @returns {@link CommandLine} containing information related to parse state
    */
    public CommandLine parse(String[] args) throws MissingArgumentException, UnrecognizedOptionException
    {
        return parse( args,
                      0,
                      args.length,
                      false);
    }

    /** <p>Parse the given list of arguments against this descriptor</p>

        <p>This method will cease parsing upon the first non-option token,
        storing the rest of the tokens for access through {@link CommandLine#getArgs()}.</p>

        <p>This is useful for parsing a command-line in pieces, such as:</p>

        <p><code>
        <pre>
        myApp -s &lt;server&gt; -p &lt;port&gt; command -p &lt;printer&gt; -s &lt;style&gt;
        </pre>
        </code></p>

        <p>Here, it'll parse up-to, but not including <code>command</code>. The
        tokens <code>command -p &lt;printer&gt; -s &lt;style&gt;</code> are available
        through {@link CommandLine#getArgs()}, which may subsequently be parsed by
        another different <code>Options</code> instance.<p>

        @param args Args to parse 
        @param stopAtNonOption stop parsing at the first non-option token

        @returns {@link CommandLine} containing information related to parse state
    */
    public CommandLine parse(String[] args,
                             boolean stopAtNonOption) throws MissingArgumentException, UnrecognizedOptionException
    {
        return parse( args,
                      0,
                      args.length,
                      stopAtNonOption);
    }

    /** <p>Parse the given list of arguments against this descriptor</p>

        <p>This method allows parsing from <code>formIndex</code> inclusive
        to <code>toIndex</code> exclusive, of the <code>args</code> parameter,
        to allow parsing a specific portion of a command-line.<p>

        @param args Args to parse 
        @param fromIndex index of args to start parsing
        @param toIndex index of args to stop parsing

        @returns {@link CommandLine} containing information related to parse state
    */
    public CommandLine parse(String[] args,
                             int fromIndex,
                             int toIndex) throws MissingArgumentException, UnrecognizedOptionException
    {
        return parse( args,
                      fromIndex,
                      toIndex,
                      false );
    }

    /** <p>Parse the given list of arguments against this descriptor</p>

        <p>This method will cease parsing upon the first non-option token,
        storing the rest of the tokens for access through {@link CommandLine#getArgs()}.</p>

        <p>This is useful for parsing a command-line in pieces, such as:</p>

        <p><code>
        <pre>
        myApp -s &lt;server&gt; -p &lt;port&gt; command -p &lt;printer&gt; -s &lt;style&gt;
        </pre>
        </code></p>

        <p>Here, it'll parse up-to, but not including <code>command</code>. The
        tokens <code>command -p &lt;printer&gt; -s &lt;style&gt;</code> are available
        through {@link CommandLine#getArgs()}, which may subsequently be parsed by
        another different <code>Options</code> instance.<p>

        <p>This method also allows parsing from <code>formIndex</code> inclusive
        to <code>toIndex</code> exclusive, of the <code>args</code> parameter,
        to allow parsing a specific portion of a command-line.<p>

        @param args Args to parse 
        @param fromIndex index of args to start parsing
        @param toIndex index of args to stop parsing
        @param stopAtNonOption stop parsing at the first non-option token

        @returns {@link CommandLine} containing information related to parse state
    */
    public CommandLine parse(String[] args,
                             int fromIndex,
                             int toIndex,
                             boolean stopAtNonOption) throws MissingArgumentException, UnrecognizedOptionException
    {
        List argList = new ArrayList();

        for ( int i = fromIndex; i < toIndex ; ++i )
        {
            argList.add( args[i] );
        }

        return parse( argList,
                      stopAtNonOption);
    }

    /** <p>Parse the given list of arguments against this descriptor</p>

        @param args Args to parse 

        @returns {@link CommandLine} containing information related to parse state
    */
    public CommandLine parse(List args) throws MissingArgumentException, UnrecognizedOptionException
    {
        return parse( args,
                      false );
    }

    /** <p>Parse the given list of arguments against this descriptor</p>

        <p>This method will cease parsing upon the first non-option token,
        storing the rest of the tokens for access through {@link CommandLine#getArgs()}.</p>

        <p>This is useful for parsing a command-line in pieces, such as:</p>

        <p><code>
        <pre>
        myApp -s &lt;server&gt; -p &lt;port&gt; command -p &lt;printer&gt; -s &lt;style&gt;
        </pre>
        </code></p>

        <p>Here, it'll parse up-to, but not including <code>command</code>. The
        tokens <code>command -p &lt;printer&gt; -s &lt;style&gt;</code> are available
        through {@link CommandLine#getArgs()}, which may subsequently be parsed by
        another different <code>Options</code> instance.<p>

        <p>This method also allows parsing from <code>formIndex</code> inclusive
        to <code>toIndex</code> exclusive, of the <code>args</code> parameter,
        to allow parsing a specific portion of a command-line.<p>

        @param args Args to parse 
        @param stopAtNonOption stop parsing at the first non-option token

        @returns {@link CommandLine} containing information related to parse state
    */
    public CommandLine parse(List inArgs,
                             boolean stopAtNonOption) throws MissingArgumentException, UnrecognizedOptionException
    {
        CommandLine cl = new CommandLine();

        List args = burst( inArgs,
                           stopAtNonOption );

        Iterator argIter = args.iterator();
        String   eachArg = null;
        Option   eachOpt = null;
        boolean  eatTheRest = false;

        while ( argIter.hasNext() )
        {
            eachArg = (String) argIter.next();

            if ( eachArg.equals("--") )
            {
                // signalled end-of-opts.  Eat the rest

                eatTheRest = true;
            }
            else if ( eachArg.startsWith("--") )
            {
                eachOpt = (Option) _longOpts.get( eachArg );

                if ( eachOpt == null )
                {
                    throw new UnrecognizedOptionException("Unrecognized option: " + eachArg);
                    // maybe someone will parse these args later
                    // cl.addArg( eachArg );
                }
                else
                {
                    if ( eachOpt.hasArg() )
                    {
                        if ( argIter.hasNext() )
                        {
                            eachArg = (String) argIter.next();

                            cl.setOpt( eachOpt.getOpt(),
                                           eachArg );
                        }
                        else
                        {
                            throw new MissingArgumentException( eachArg + " requires an argument.");
                        }
                        
                    }
                    else
                    {
                        cl.setOpt( eachOpt.getOpt() );
                    }
                }

            }
            else if ( eachArg.equals("-") )
            {
                // Just-another-argument

                if ( stopAtNonOption )
                {
                    eatTheRest = true;
                }
                else
                {
                    cl.addArg( eachArg );
                }
            }
            else if ( eachArg.startsWith("-") )
            {
                eachOpt = (Option) _shortOpts.get( eachArg );

                if ( eachOpt == null )
                {
                    throw new UnrecognizedOptionException("Unrecognized option: " + eachArg);
                    // maybe someone will parse these args later
                    // cl.addArg( eachArg );
                }
                else
                {
                    if ( eachOpt.hasArg() )
                    {
                        if ( argIter.hasNext() )
                        {
                            eachArg = (String) argIter.next();

                            cl.setOpt( eachOpt.getOpt(),
                                           eachArg );
                                
                        }
                        else
                        {
                            throw new MissingArgumentException( eachArg + " requires an argument.");
                        }
                        
                    }
                    else
                    {
                        cl.setOpt( eachOpt.getOpt() );
                    }
                }
            }
            else
            {
                cl.addArg( eachArg );
                if ( stopAtNonOption )
                {
                    eatTheRest = true;
                }
            }

            if ( eatTheRest )
            {
                while ( argIter.hasNext() )
                {
                    eachArg = (String) argIter.next();
                    cl.addArg( eachArg );
                }
            }
        }

        return cl;
    }

    private List burst(List inArgs,
                       boolean stopAtNonOption)
    {
        List args = new LinkedList();

        Iterator argIter = inArgs.iterator();
        String   eachArg = null;

        boolean eatTheRest = false;

        while ( argIter.hasNext() )
        {
            eachArg = (String) argIter.next();

            if ( eachArg.equals("--") )
            {
                // Look for -- to indicate end-of-options, and
                // just stuff it, along with everything past it
                // into the returned list.

                eatTheRest = true;
            }
            else if ( eachArg.startsWith("--") )
            {
                // It's a long-option, so doesn't need any
                // bursting applied to it.

                args.add( eachArg );
            }
            else if ( eachArg.startsWith("-") )
            {
                // It might be a short arg needing
                // some bursting

                if ( eachArg.length() == 1)
                {
                    // It's not really an option, so
                    // just drop it on the list

                    if ( stopAtNonOption )
                    {
                        eatTheRest = true;
                    }
                    else
                    {
                        args.add( eachArg );
                    }
                }
                else if ( eachArg.length() == 2 )
                {
                    // No bursting required

                    args.add( eachArg );
                }
                else
                {
                    // Needs bursting.  Figure out
                    // if we have multiple options,
                    // or maybe an option plus an arg,
                    // or some combination thereof.

                    for ( int i = 1 ; i < eachArg.length() ; ++i )
                    {
                        String optStr = "-" + eachArg.charAt(i);
                        Option opt    = (Option) _shortOpts.get( optStr );

                        if ( (opt != null) && (opt.hasArg()) )
                        {
                            // If the current option has an argument,
                            // then consider the rest of the eachArg
                            // to be that argument.

                            args.add( optStr );

                            if ( (i+1) < eachArg.length() )
                            {
                                String optArg = eachArg.substring(i+1);
                                args.add( optArg );
                            }

                            break;
                        }
                        else
                        {
                            // No argument, so prepend the single dash,
                            // and then drop it into the arglist.

                            args.add( optStr );
                        }
                    }
                }
            }
            else
            {
                // It's just a normal non-option arg,
                // so dump it into the list of returned
                // values.

                args.add( eachArg );

                if ( stopAtNonOption )
                {
                    eatTheRest = true;
                }
            }

            if ( eatTheRest )
            {
                while ( argIter.hasNext() )
                {
                   args.add( argIter.next() );
                }
            }
        }

        return args;
    }

    private void addOption(Option opt) throws DuplicateOptionException
    {

        String shortOptStr = "-" + opt.getOpt();

        if ( _shortOpts.containsKey( shortOptStr ) )
        {
            throw new DuplicateOptionException( "option [-" + opt.getOpt() + "] already in set.");
        }

        if ( opt.hasLongOpt() )
        {
            if ( _longOpts.containsKey( opt.getLongOpt() ) )
            {
                throw new DuplicateOptionException( "long option [--" + opt.getLongOpt() + "] already in set.");
            }

            _longOpts.put( "--" + opt.getLongOpt(),
                           opt );
        }

        _shortOpts.put( "-" + opt.getOpt(),
                        opt );

        _options.add( opt );
    }

    /** <p>Retrieve a read-only list of options in this set</p>

        @returns read-only List of {@link Option} objects in this descriptor
     */
    public List getOptions()
    {
        return Collections.unmodifiableList(_options);
    }

    /** <p>Retrieve the named {@link Option}<p>

        @param opt short single-character name of the {@link Option}
     */
    public Option getOption(char opt)
    {
        return (Option) _shortOpts.get( new Character(opt) );
    }

    /** <p>Retrieve the named {@link Option}<p>

        @param opt long name of the {@link Option}
     */
    public Option getOption(String longOpt)
    {
        return (Option) _longOpts.get( longOpt );
    }

    /** <p>Dump state, suitable for debugging.</p>

        @return Stringified form of this object
    */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();

        buf.append("[ Options: [ short ");
        buf.append( _shortOpts.toString() );
        buf.append( " ] [ long " );
        buf.append( _longOpts );
        buf.append( " ]");

        return buf.toString();
    }
}
