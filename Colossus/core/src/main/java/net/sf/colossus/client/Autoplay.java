/**
 *
 */
package net.sf.colossus.client;


import java.util.logging.Logger;

import net.sf.colossus.common.Options;


/**
 *  This is a central place to handle various aspects of Autoplay.
 *
 *  * It replaces the hack in Options class for 'if option name start with
 *    "Auto "... then check the normal complete autoPlay first'
 *  * The "autoplay because of user's inactivity" can also be handled here
 *    better
 *  * At the long run, I guess here might be added some logic for the
 *    "kick AI into taking over" for when that happens in battle.
 *    (during masterboard play, a kickPhase() does the job, but for the
 *    various battle things / dialogs / ...  one might need to do some
 *    per-situation-specific things to make AI kick in.
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
        return isRealAutoplayActive() || isInactivityAutoplayActive();
    }

    public boolean isRealAutoplayActive()
    {
        return options.getOption(Options.autoPlay);
    }

    public boolean isInactivityAutoplayActive()
    {
        // TODO: this should be changed to be handled via own boolean
        // variable, instead of temporary overriding the normal
        // autoplay option
        return originalAutoplayOverridden
            && options.getOption(Options.autoPlay);
    }

    public void switchOnInactivityAutoplay()
    {
        if (originalAutoplayOverridden)
        {
            LOGGER
                .warning("Requested to store autoplay value, but is already overriden! Ignored.");
        }
        else
        {
            LOGGER.fine("Activating inactivityAutoplay");
            originalAutoplayValue = options.getOption(Options.autoPlay);
            originalAutoplayOverridden = true;
            options.setOption(Options.autoPlay, true);
        }
    }

    public void switchOffInactivityAutoplay()
    {
        if (originalAutoplayOverridden)
        {
            LOGGER.fine("Restoring original autoplay value "
                + originalAutoplayValue);
            options.setOption(Options.autoPlay, originalAutoplayValue);
            originalAutoplayOverridden = false;
        }
        else
        {
            LOGGER
                .warning("Asked to restore originalAutoplay, but is not overridden right now.");
        }

    }

    public boolean autoPlay()
    {
        return isAutoplayActive();
    }

    public boolean autoPickColor()
    {
        return autoPlay() || options.getOption(Options.autoPickColor);
    }

    public boolean autoPickMarker()
    {
        return autoPlay() || options.getOption(Options.autoPickMarker);
    }

    public boolean autoSplit()
    {
        return autoPlay() || options.getOption(Options.autoSplit);
    }

    public boolean autoMasterMove()
    {
        return autoPlay() || options.getOption(Options.autoMasterMove);
    }

    public boolean autoPickEntrySide()
    {
        return autoPlay() || options.getOption(Options.autoPickEntrySide);
    }

    public boolean autoPickLord()
    {
        return autoPlay() || options.getOption(Options.autoPickLord);
    }

    public boolean autoPickEngagements()
    {
        return autoPlay() || options.getOption(Options.autoPickEngagements);
    }

    public boolean autoFlee()
    {
        return autoPlay() || options.getOption(Options.autoFlee);
    }

    public boolean autoConcede()
    {
        return autoPlay() || options.getOption(Options.autoConcede);
    }

    public boolean autoNegotiate()
    {
        return autoPlay() || options.getOption(Options.autoNegotiate);
    }

    public boolean autoForcedStrike()
    {
        return autoPlay() || options.getOption(Options.autoForcedStrike);
    }

    public boolean autoCarrySingle()
    {
        return autoPlay() || options.getOption(Options.autoCarrySingle);
    }

    public boolean autoRangeSingle()
    {
        return autoPlay() || options.getOption(Options.autoRangeSingle);
    }

    public boolean autoSummonAngels()
    {
        return autoPlay() || options.getOption(Options.autoSummonAngels);
    }

    public boolean autoAcquireAngels()
    {
        return autoPlay() || options.getOption(Options.autoAcquireAngels);
    }

    public boolean autoRecruit()
    {
        return autoPlay() || options.getOption(Options.autoRecruit);
    }

    public boolean autoPickRecruiter()
    {
        return autoPlay() || options.getOption(Options.autoPickRecruiter);
    }

    public boolean autoReinforce()
    {
        return autoPlay() || options.getOption(Options.autoReinforce);
    }

}
