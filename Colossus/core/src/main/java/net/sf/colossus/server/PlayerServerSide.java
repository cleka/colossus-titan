package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.PlayerClientSide;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.Dice;
import net.sf.colossus.game.Game;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.util.Glob;
import net.sf.colossus.util.InstanceTracker;
import net.sf.colossus.variant.CreatureType;


/**
 * Class Player holds the data for one player in a Titan game.
 *
 * @author David Ripton
 */
public final class PlayerServerSide extends Player implements
    Comparable<PlayerServerSide>
{
    private static final Logger LOGGER = Logger
        .getLogger(PlayerServerSide.class.getName());

    // TODO the half-points are really used only in the die(..) method,
    // they could be summed up there and then added all in one go. That
    // would save us from storing a double and truncating things later
    // and the getScore/setScore overrides could go.
    private double score; // track half-points, then round
    private boolean summoned;

    /**
     * TODO {@link PlayerClientSide} just checks if any legion has teleported.
     *      Pick one version and move up into {@link Player}.
     */
    private boolean teleported;

    /**
     * TODO this might be better as a state in {@link Game} since there is
     *      always only one per game, not per player
     */
    private int movementRoll; // 0 if movement has not been rolled.

    private boolean titanEliminated;

    /**
     * The legion which gave a summonable creature.
     */
    private LegionServerSide donor;
    private String firstMarker;

    /* Needed during loading a game: */
    private String playersEliminatedBackup = "";
    private final List<Legion> legionsBackup = new ArrayList<Legion>();

    PlayerServerSide(String name, GameServerSide game, String shortTypeName)
    {
        // TODO why are the players on the client side numbered but not here?
        super(game, name, 0);

        // add package path to AI names and choose random type for "anyAI":
        setType(shortTypeName);

        InstanceTracker.register(this, name);
    }

    /**
     * Overridden to return specific flavor of Game until the upper class is sufficient.
     */
    @Override
    public GameServerSide getGame()
    {
        return (GameServerSide)super.getGame();
    }

    // TODO strong redundancy with Client.setType(String)
    @Override
    public void setType(final String shortTypeName)
    {
        String type = shortTypeName;
        LOGGER.log(Level.FINEST, "Called Player.setType() for " + getName()
            + " " + type);

        if (type.endsWith(Constants.anyAI))
        {
            int aiCount = Constants.numAITypes;
            // Do not choose an ExperimentalAI as "A Random AI" in user games,
            // but in stresstest we DO want to test them.
            if (!Options.isStresstest())
            {
                aiCount--;
            }
            int whichAI = Dice.rollDie(aiCount) - 1;
            type = Constants.aiArray[whichAI];
        }

        if (!type.startsWith(Constants.aiPackage))
        {
            type = Constants.aiPackage + type;
        }
        super.setType(type);
    }

    void initMarkersAvailable()
    {
        initMarkersAvailable(getShortColor());
    }

    void initMarkersAvailable(String shortColor)
    {
        for (int i = 1; i <= 9; i++)
        {
            addMarkerAvailable(shortColor + '0' + Integer.toString(i));
        }
        for (int i = 10; i <= 12; i++)
        {
            addMarkerAvailable(shortColor + Integer.toString(i));
        }
    }

    /** Set markersAvailable based on other available information.
     *  NOTE: to be used only during loading a Game!
     */
    void computeMarkersAvailable()
    {
        if (isDead())
        {
            clearMarkersAvailable();
        }
        else
        {
            initMarkersAvailable();
            StringBuilder allVictims = new StringBuilder(getPlayersElim());
            for (int i = 0; i < allVictims.length(); i += 2)
            {
                String shortColor = allVictims.substring(i, i + 2);
                initMarkersAvailable(shortColor);
                Player victim = getGame().getPlayerByShortColor(shortColor);
                allVictims.append(victim.getPlayersElim());
            }
            for (Legion legion : getLegions())
            {
                removeMarkerAvailable(legion.getMarkerId());
            }
        }
    }

    void setFirstMarker(String firstMarker)
    {
        this.firstMarker = firstMarker;
    }

    String getFirstMarker()
    {
        return firstMarker;
    }

    /** Players are sorted in order of decreasing starting tower.
     This is inconsistent with equals(). */
    public int compareTo(PlayerServerSide other)
    {
        return other.getStartingTower().getLabel().compareTo(
            this.getStartingTower().getLabel());
    }

    @Override
    public boolean hasTeleported()
    {
        return teleported;
    }

    void setTeleported(boolean teleported)
    {
        this.teleported = teleported;
    }

    boolean hasSummoned()
    {
        return summoned;
    }

    void setSummoned(boolean summoned)
    {
        this.summoned = summoned;
    }

    LegionServerSide getDonor()
    {
        return donor;
    }

    void setDonor(LegionServerSide donor)
    {
        this.donor = donor;
    }

    /** Remove all of this player's zero-height legions. */
    void removeEmptyLegions()
    {
        Iterator<LegionServerSide> it = getLegions().iterator();
        while (it.hasNext())
        {
            LegionServerSide legion = it.next();
            if (legion.getHeight() == 0)
            {
                if (legion.equals(donor))
                {
                    donor = null;
                }
                legion.prepareToRemove(true, true);
                it.remove();
            }
        }
    }

    /**
     * TODO remove once noone needs the specific version anymore
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<LegionServerSide> getLegions()
    {
        return (List<LegionServerSide>)super.getLegions();
    }

    /** Return the number of this player's legions that have moved. */
    int legionsMoved()
    {
        int count = 0;

        for (Legion legion : getLegions())
        {
            if (legion.hasMoved())
            {
                count++;
            }
        }
        return count;
    }

    /** Return the number of this player's legions that have legal
     non-teleport moves remaining. */
    int countMobileLegions()
    {
        int count = 0;
        for (LegionServerSide legion : getLegions())
        {
            if ((legion).hasConventionalMove())
            {
                count++;
            }
        }
        return count;
    }

    void commitMoves()
    {
        for (LegionServerSide legion : getLegions())
        {
            (legion).commitMove();
        }
    }

    // TODO temporary for Movement class; move up to game.Player?
    public int getMovementRollSS()
    {
        return getMovementRoll();
    }

    int getMovementRoll()
    {
        return movementRoll;
    }

    void setMovementRoll(int movementRoll)
    {
        this.movementRoll = movementRoll;
    }

    void resetTurnState()
    {
        summoned = false;
        donor = null;

        setTeleported(false);
        movementRoll = 0;

        // Make sure that all legions are allowed to move and recruit.
        commitMoves();
    }

    int rollMovement()
    {
        // Only roll if it hasn't already been done.
        if (movementRoll != 0)
        {
            LOGGER.warning("Called rollMovement() more than once");
        }
        else
        {
            movementRoll = Dice.rollDie();
            LOGGER.info(getName() + " rolls a " + movementRoll
                + " for movement");
        }
        return movementRoll;
    }

    void takeMulligan()
    {
        int mulligans = getMulligansLeft();
        if (mulligans > 0)
        {
            undoAllMoves();
            LOGGER.info(getName() + " takes a mulligan");
            if (!getGame().getOption(Options.unlimitedMulligans))
            {
                mulligans--;
                setMulligansLeft(mulligans);
            }
            movementRoll = 0;
        }
    }

    void undoMove(Legion legion)
    {
        if (legion != null)
        {
            ((LegionServerSide)legion).undoMove();
        }
    }

    void undoAllMoves()
    {
        for (LegionServerSide legion : getLegions())
        {
            legion.undoMove();
        }
    }

    /** Return true if two or more of this player's legions share
     *  a hex and they have a legal non-teleport move. */
    boolean splitLegionHasForcedMove()
    {
        for (LegionServerSide legion : getLegions())
        {
            if (getGame().getNumFriendlyLegions(legion.getCurrentHex(), this) > 1
                && (legion).hasConventionalMove())
            {
                LOGGER.finest("Found unseparated split legions at hex "
                    + legion.getCurrentHex());
                return true;
            }
        }
        return false;
    }

    /** Return true if any legion can recruit. */
    boolean canRecruit()
    {
        for (LegionServerSide legion : getLegions())
        {
            if (legion.hasMoved() && legion.canRecruit())
            {
                return true;
            }
        }
        return false;
    }

    //
    private Legion previousUndoRecruitLegion = null;

    /**
     * Tell legion to do undo the recruiting and trigger needed messages
     * to be sent to clients
     * @param legion The legion which undoes the recruiting
     */
    void undoRecruit(Legion legion)
    {
        assert legion != null : "Player.undoRecruit: legion must not be null";

        // This is now permanently fixed in Player.java, so this should
        // never happen again. Still, leaving this in place, just to be sure...
        CreatureType recruit = ((LegionServerSide)legion).getRecruit();
        if (recruit == null)
        {
            if (previousUndoRecruitLegion != null
                && previousUndoRecruitLegion.equals(legion))
            {
                LOGGER
                    .info("Player.undoRecruit: "
                        + "Nothing to unrecruit for legion "
                        + legion.getMarkerId()
                        + " but that's probably just a duplicate click - ignoring it.");
                LOGGER.info(getGame().getServer().processingCH
                    .dumpLastProcessedLines());
            }
            else
            {
                LOGGER.log(Level.WARNING,
                    "Player.undoRecruit: Nothing to unrecruit for legion "
                    + legion.getMarkerId());
                LOGGER.info(getGame().getServer().processingCH
                    .dumpLastProcessedLines());
            }
            return;
        }
        previousUndoRecruitLegion = legion;
        ((LegionServerSide)legion).undoRecruit();

        // Update number of creatures in status window.
        getGame().getServer().allUpdatePlayerInfo();
        getGame().getServer().undidRecruit(legion, recruit, false);
    }

    /**
     * Tell legion to do undo the reinforcement and trigger needed messages
     * to be sent to clients
     * (quite similar to undorecuit, but not exactly the same)
     * @param legion The legion which cancels the reinforcement
     */
    void undoReinforcement(Legion legion)
    {
        // This is now permanently fixed in Player.java, so this should
        // never happen again. Still, leaving this in place, just to be sure...
        CreatureType recruit = ((LegionServerSide)legion).getRecruit();
        if (recruit == null)
        {
            LOGGER.log(Level.SEVERE,
                "Player.undoReinforcement: Nothing to unreinforce for marker "
                    + legion);
            return;
        }
        ((LegionServerSide)legion).undoReinforcement();

        // We don't do the allUpdatePlayerInfo() here, because the remove
        // is done later by iterator (so amounts are not even changed yet)
        getGame().getServer().undidRecruit(legion, recruit, true);
    }

    void undoSplit(Legion splitoff)
    {
        Legion parent = ((LegionServerSide)splitoff).getParent();
        ((LegionServerSide)splitoff).recombine(parent, true);
        getGame().getServer().allUpdatePlayerInfo();
    }

    void recombineIllegalSplits()
    {
        Iterator<LegionServerSide> it = getLegions().iterator();
        while (it.hasNext())
        {
            Legion legion = it.next();
            // Don't use the legion's real parent, as there could have been
            // a 3-way split and the parent could be gone.
            Legion parent = getGame().getFirstFriendlyLegion(
                legion.getCurrentHex(), this);
            if (legion != parent)
            {
                ((LegionServerSide)legion).recombine(parent, false);
                it.remove();
            }
        }
        getGame().getServer().allUpdatePlayerInfo();
    }

    @Override
    public void setScore(int score)
    {
        this.score = score;
    }

    @Override
    public int getScore()
    {
        return (int)score;
    }

    /** Add points to this player's score.  Update the status window
     *  to reflect the addition. */
    void addPoints(double points, boolean halfPoints)
    {
        if (points > 0)
        {
            score += points;
            if (getGame() != null)
            {
                getGame().getServer().allUpdatePlayerInfo();
            }

            LOGGER.info(getName() + " earns " + points + " "
                + (halfPoints ? "half-points" : "points") + " ("
                + (score - points) + " + " + points + " => " + score + ")");
        }
    }

    /** Remove half-points. */
    void truncScore()
    {
        score = Math.floor(score);
    }

    /**
     * Award points and handle all acquiring related issues.
     *
     * Note that this is not used for adding points for cleaning up legions
     * of a dead player!
     *
     * @param points the points to award
     * @param legion the legion which is entitled to acquire due to that
     * @param halfPoints this are already halfPoints (from fleeing)
     */
    void awardPoints(int points, LegionServerSide legion, boolean halfPoints)
    {
        int scoreBeforeAdd = getScore(); // 375
        addPoints(points, halfPoints); // 375 + 150 = 525

        getGame().acquireMaybe(legion, scoreBeforeAdd, points);
    }

    /**
     * Turns the player dead.
     *
     * This method calculates the points other players get, adds them to their score and
     * then cleans up this player and marks him dead.
     *
     * TODO is it really the Player's role to assign points? I'd rather see that responsibility
     * with the Game object
     *
     * TODO the slayer could be non-null if we introduce a null object (some object called
     * e.g. "NOONE" that behaves like a Player as far as possible, giving a name and swallowing
     * points)
     *
     * @param slayer The player who killed us. May be null if we just gave up or it is a draw.
     */
    void die(Player slayer)
    {
        LOGGER.info("Player '" + getName() + "' is dying, killed by "
            + (slayer == null ? "nobody" : slayer.getName()));
        // Engaged legions give half points to the player they're
        // engaged with.  All others give half points to slayer,
        // if non-null.

        for (Iterator<LegionServerSide> itLeg = getLegions().iterator(); itLeg
            .hasNext();)
        {
            LegionServerSide legion = itLeg.next();
            Legion enemyLegion = getGame().getFirstEnemyLegion(
                legion.getCurrentHex(), this);
            double halfPoints = legion.getPointValue() / 2.0;

            Player scorer;

            if (enemyLegion != null)
            {
                scorer = enemyLegion.getPlayer();
            }
            else
            {
                scorer = slayer;
            }
            if (scorer != null)
            {
                ((PlayerServerSide)scorer).addPoints(halfPoints, true);
            }

            // Call the iterator's remove() method rather than
            // removeLegion() to avoid concurrent modification problems.
            legion.prepareToRemove(true, true);
            itLeg.remove();
        }

        // Truncate every player's score to an integer value.
        for (Player player : getGame().getPlayers())
        {
            ((PlayerServerSide)player).truncScore();
        }

        // Mark this player as dead.
        setDead(true);

        // Record the slayer and give him this player's legion markers.
        handleSlaying(slayer);

        getGame().getServer().allUpdatePlayerInfo();

        LOGGER.info(getName() + " is dead, telling everyone about it");

        getGame().getServer().allTellPlayerElim(this, slayer, true);
    }

    // Record the slayer and give him this player's legion markers.
    public void handleSlaying(Player slayer)
    {
        if (slayer != null)
        {
            slayer.addPlayerElim(this);
            for (String markerId : getMarkersAvailable())
            {
                slayer.addMarkerAvailable(markerId);
            }
            clearMarkersAvailable();
        }
    }

    void eliminateTitan()
    {
        titanEliminated = true;
    }

    boolean isTitanEliminated()
    {
        return titanEliminated;
    }

    /** Return a colon-separated string with a bunch of info for
     *  the status screen. */
    String getStatusInfo(boolean treatDeadAsAlive)
    {
        List<String> li = new ArrayList<String>();
        li.add(Boolean.toString(!treatDeadAsAlive && isDead()));
        li.add(getName());
        li.add(getStartingTower() != null ? getStartingTower().getLabel()
            : null);
        li.add((getColor() != null) ? getColor().getName() : null);
        li.add(getType());
        li.add(getPlayersElim());
        li.add(Integer.toString(getLegions().size()));
        li.add(Integer.toString(getNumCreatures()));
        li.add(Integer.toString(getTitanPower()));
        li.add(Integer.toString(getScore()));
        li.add(Integer.toString(getMulligansLeft()));
        li.addAll(getMarkersAvailable());
        return Glob.glob(":", li);
    }

    /* After legions are loaded, put them aside (legions and who owns them
     * might be totally mismatching esp. after player elimination.
     * Reconstruct creation, deceasing, surviving legions, player elimination
     * and slayer-setting and markerTransfer from history;
     * After that, synchronize state info like location, hasMoved etc
     * from the backup.
     */

    public void backupLoadedData()
    {
        playersEliminatedBackup = getPlayersElim();
        setPlayersElim("");

        for (LegionServerSide l : getLegions())
        {
            legionsBackup.add(l);
        }
        removeAllLegions();
    }

    public boolean resyncBackupData()
    {
        if (!(playersEliminatedBackup != null && getPlayersElim() != null && getPlayersElim()
            .equals(playersEliminatedBackup)))
        {
            LOGGER.severe("PlayersEliminated strings NOT in sync: replayed "
                + " is '" + getPlayersElim() + "' but loaded is '"
                + playersEliminatedBackup + "'!");
            return false;
        }

        if (getLegions().size() != legionsBackup.size())
        {
            LOGGER.severe("Loaded player data, legion lists different size!"
                + " replay: " + getLegions().size() + ", loaded: "
                + legionsBackup.size());
            return false;
        }

        boolean ok = true;
        for (Legion bl : legionsBackup)
        {
            Legion l = getLegionByMarkerId(bl.getMarkerId());

            List<CreatureType> ourCreatures = new ArrayList<CreatureType>(l
                .getCreatureTypes());
            ourCreatures.removeAll(bl.getCreatureTypes());

            if ((l.getHeight() != bl.getHeight()) || !ourCreatures.isEmpty())
            {
                LOGGER
                    .severe("Loaded legion data differs from replay result: "
                        + "Replay-Legion content for " + l.getMarkerId()
                        + " is " + Glob.glob(l.getCreatureTypes())
                        + ", Loaded-Legion content is "
                        + Glob.glob(bl.getCreatureTypes()));
                ok = false;
            }
        }

        if (ok)
        {
            // discard the replay results and get the backup data back into
            // place which has the right legion state:
            removeAllLegions();
            for (Legion l : legionsBackup)
            {
                addLegion(l);
            }
        }
        return ok;
    }
}
