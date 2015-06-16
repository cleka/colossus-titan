/**
 *
 */
package net.sf.colossus.client;

import java.util.logging.Logger;

import net.sf.colossus.common.Options;


/**
 *
 */
public class Autoplay
{
    private static final Logger LOGGER = Logger.getLogger(Autoplay.class
        .getName());

    private final Options options;

    private boolean originalAutoplayValue = false;

    private boolean originalAutoplayOverridden = false;

    /**
     *
     */
    public Autoplay(Options options)
    {
        // TODO Auto-generated constructor stub
        LOGGER.finest("Class Autoplay instantiated.");

        this.options = options;
    }

    public boolean isAutoplayActive()
    {
        return options.getOption(Options.autoPlay);
    }

    public boolean isRealAutoplayActive()
    {
        return options.getOption(Options.autoPlay);
    }

    public boolean isInactivityAutoplayActive()
    {

        return options.getOption(Options.autoPlay);
    }

    public void setInactivityAutoplay()
    {
        originalAutoplayValue = options.getOption(Options.autoPlay);
        originalAutoplayOverridden = true;
        System.out.println("Stored original autoplay value "
            + originalAutoplayValue);
    }

    public void resetInactivityAutoplay()
    {
        if (originalAutoplayOverridden)
        {
            System.out.println("Restoring original autoplay value "
                + originalAutoplayValue);
            options.setOption(Options.autoPlay, originalAutoplayValue);
            originalAutoplayOverridden = false;
        }
        else
        {
            System.out
                .println("Asked to restore originalAutoplay, but is not overridden right now.");
        }

    }

}
