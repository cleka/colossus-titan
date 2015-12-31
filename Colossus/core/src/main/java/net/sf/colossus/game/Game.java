package net.sf.colossus.game;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.colossus.util.CollectionHelper;
import net.sf.colossus.util.Predicate;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * An ongoing game in Colossus.
 *
 * As opposed to {@link Variant} this class holds information about an ongoing game
 * and its status.
 */
public class Game
{
    private static final Logger LOGGER = Logger
        .getLogger(Game.class.getName());
    /**
     * The variant played in this game.
     */
    private final Variant variant;

    /**
     * The state of the different players in the game.
     */
    protected final List<Player> players = new ArrayList<Player>();

    /**
     * The caretaker takes care of managing the available and dead creatures.
     */
    private final Caretaker caretaker;

    /**
     * The current turn number. Advance when every player has done his move
     */
    protected int turnNumber = -1;

    /**
     * The current game phase (Split, Move, Fight, Muster)
     */
    protected Phase phase;

    /**
     * Last movement roll for any player.
     */
    private int movementRoll = -1;

    /**
     *  Status for Game is over and message for it
     *  On client side this also implies:
     *      If the game is over, then quitting does not require confirmation.
     */
    private boolean gameOver = false;
    private String gameOverMessage = null;

    private boolean suspended;

    private Engagement engagement;

    protected Battle battle = null;

    private final BattleStrike battleStrike;

    /**
     * Create a Game object.
     *
     * @param variant The variant object, not null
     * @param playerNames Names of the players, not used yet
     */
    public Game(Variant variant, String[] playerNames)
    {
        assert variant != null : "Can't create game with null variant!";

        this.variant = variant;
        this.caretaker = new Caretaker(this);
        this.phase = Phase.INIT;

        this.battleStrike = new BattleStrike(this);
    }

    public Variant getVariant()
    {
        return variant;
    }

    public void addPlayer(Player p)
    {
        players.add(p);
    }

    public Collection<Player> getPlayers()
    {
        assert players.size() != 0 : "getPlayers called before player info set (size==0)!";
        return Collections.unmodifiableCollection(players);
    }

    /**
     * Get a list of preliminary player names, during game startup / clients
     * connecting. Preliminary, because some of them might change their name
     * later (e.g. the "byColor" ones).
     * @return List of player names
     */
    public Collection<String> getPreliminaryPlayerNames()
    {
        Collection<String> prePlayerNames = new ArrayList<String>();
        assert players.size() != 0 : "getPreliminaryPlayerNames called before player info set (size==0)!";
        for (Player p : Collections.unmodifiableCollection(players))
        {
            prePlayerNames.add(p.getName());
        }
        return prePlayerNames;
    }

    public int getNumPlayers()
    {
        assert players.size() != 0 : "getNumPlayers called before player info set (size==0)!";
        return players.size();
    }

    public int getNumLivingPlayers()
    {
        int alive = 0;
        for (Player info : players)
        {
            if (!info.isDead() && !info.getDeadBeforeSave())
            {
                alive++;
            }
        }
        return alive;
    }

    /**
     *
     * @return Returns true if all still alive players are AIs
     */
    public boolean onlyAIsRemain()
    {
        for (Player p : players)
        {
            if (!p.isAI() && !p.isDead())
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the number of real players (Human or Network)
     * which are still alive.
     *
     * TODO partly same idea as "onlyAIsRemain()"
     */
    protected int getNumHumansRemaining()
    {
        int remaining = 0;
        for (Player player : getPlayers())
        {
            if (player.isHuman() && !player.isDead())
            {
                remaining++;
            }
        }
        return remaining;
    }

    // Server uses this to decide whether it needs to start a file server
    public int getNumRemoteRemaining()
    {
        int remaining = 0;
        for (Player player : getPlayers())
        {
            if (player.isNetwork() && !player.isDead())
            {
                remaining++;
            }
        }
        return remaining;
    }

    public Caretaker getCaretaker()
    {
        return caretaker;
    }

    public int getMovementRoll()
    {
        return movementRoll;
    }

    public void setMovementRoll(int roll)
    {
        movementRoll = roll;
    }

    public boolean isGameOver()
    {
        return this.gameOver;
    }

    public void setSuspended(boolean value)
    {
        this.suspended = value;
    }

    public boolean isSuspended()
    {
        return this.suspended;
    }

    public String getGameOverMessage()
    {
        return this.gameOverMessage;
    }

    public void setGameOver(boolean gameOver, String message)
    {
        this.gameOver = gameOver;
        this.gameOverMessage = message;
    }

    public void createEngagement(MasterHex hex, Legion attacker,
        Legion defender)
    {
        this.engagement = new Engagement(hex, attacker, defender);
    }

    public void clearEngagementData()
    {
        this.engagement = null;
    }

    public boolean isEngagementInProgress()
    {
        return this.engagement != null;
    }

    public Engagement getEngagement()
    {
        return this.engagement;
    }

    public Battle getBattle()
    {
        return this.battle;
    }

    public Legion getBattleActiveLegion()
    {
        return battle.getBattleActiveLegion();
    }

    public MasterHex getBattleSite()
    {
        return engagement == null ? null : engagement.getLocation();
    }

    public Legion getDefender()
    {
        if (engagement != null)
        {
            return engagement.getDefendingLegion();
        }
        else
        {
            LOGGER.warning("asking for defender but engagement is null?");
            return null;
        }
    }

    public Legion getAttacker()
    {
        if (engagement != null)
        {
            return engagement.getAttackingLegion();
        }
        else
        {
            LOGGER.warning("asking for attacker but engagement is null?");
            return null;
        }

    }

    /**
     * Return a list of angel types that can be acquired based
     * on the hex in which legion is, when reaching given score threshold,
     * and if they are still available from caretaker
     * @param terrain The terrain in which this legion wants to acquire
     * @param score A acquring threshold, e.g. in Default 100, ..., 400, 500
     * @return list of acquirables
     */
    List<CreatureType> findAvailableEligibleAngels(MasterBoardTerrain terrain,
        int score)
    {
        List<CreatureType> recruits = new ArrayList<CreatureType>();
        List<String> allRecruits = getVariant().getRecruitableAcquirableList(
            terrain, score);
        Iterator<String> it = allRecruits.iterator();
        while (it.hasNext())
        {
            String name = it.next();
            CreatureType creature = getVariant().getCreatureByName(name);
            if (getCaretaker().getAvailableCount(creature) >= 1
                && !recruits.contains(creature))
            {
                recruits.add(creature);
            }
        }
        return recruits;
    }

    /** Return a list of all legions of all players. */
    public List<Legion> getAllLegions()
    {
        List<Legion> list = new ArrayList<Legion>();
        for (Player player : players)
        {
            List<? extends Legion> legions = player.getLegions();
            list.addAll(legions);
        }
        return list;
    }

    public int getNumLivingCreatures(CreatureType type)
    {
        int livingCount = 0;
        for (Player player : players)
        {
            List<? extends Legion> legions = player.getLegions();
            for (Legion legion : legions)
            {
                livingCount += legion.numCreature(type);
            }
        }
        return livingCount;
    }

    public List<Legion> getLegionsByHex(MasterHex masterHex)
    {
        assert masterHex != null : "No hex given to find legions on.";

        List<Legion> result = new ArrayList<Legion>();
        for (Legion legion : getAllLegions())
        {
            if (masterHex.equals(legion.getCurrentHex()))
            {
                result.add(legion);
            }
        }
        return result;
    }

    public int getNumEnemyLegions(MasterHex masterHex, Player player)
    {
        int count = 0;
        for (Legion legion : getEnemyLegions(player))
        {
            if (masterHex.equals(legion.getCurrentHex()))
            {
                count++;
            }
        }
        return count;
    }

    public int getNumLegions(MasterHex masterHex)
    {
        int count = 0;
        for (Legion legion : getAllLegions())
        {
            if (masterHex.equals(legion.getCurrentHex()))
            {
                count++;
            }
        }
        return count;
    }

    public List<Legion> getFriendlyLegions(final MasterHex hex,
        final Player player)
    {
        return CollectionHelper.selectAsList(player.getLegions(),
            new Predicate<Legion>()
            {
                public boolean matches(Legion legion)
                {
                    return legion.getCurrentHex().equals(hex);
                }
            });
    }

    /** Return a list of all legions not belonging to player. */
    public List<Legion> getEnemyLegions(final Player player)
    {
        List<Legion> result = new ArrayList<Legion>();
        for (Player otherPlayer : players)
        {
            if (!otherPlayer.equals(player))
            {
                result.addAll(otherPlayer.getLegions());
            }
        }
        return result;
    }

    public List<Legion> getEnemyLegions(final MasterHex hex,
        final Player player)
    {
        List<Legion> result = new ArrayList<Legion>();
        for (Player otherPlayer : players)
        {
            if (!otherPlayer.equals(player))
            {
                for (Legion legion : otherPlayer.getLegions())
                {
                    if (legion.getCurrentHex().equals(hex))
                    {
                        result.add(legion);
                    }
                }
            }
        }
        return result;
    }

    // TODO decide which one of getFirstFriendlyLegion() to use;
    // the one is from server side, the other from client side.
    // Is the CollectionHelper style more efficient?
    // The simple loop is easier to understand IMHO...
    /*
    public Legion getFirstFriendlyLegion(final MasterHex hex, Player player)
    {
        return CollectionHelper.selectFirst(player.getLegions(),
            new Predicate<Legion>()
            {
                public boolean matches(Legion legion)
                {
                    return legion.getCurrentHex().equals(hex);
                }
            });
    }
    */

    public Legion getFirstFriendlyLegion(MasterHex masterHex, Player player)
    {
        for (Legion legion : player.getLegions())
        {
            if (masterHex.equals(legion.getCurrentHex()))
            {
                return legion;
            }
        }

        // only info. I *think* in recombining illegal split case
        // it might not find anything and that would be totally OK ...
        LOGGER.info("Could not find any friendly legion for player "
            + player.getName() + " in hex " + masterHex);
        return null;
    }

    public boolean isOccupied(MasterHex masterHex)
    {
        for (Legion legion : getAllLegions())
        {
            if (masterHex.equals(legion.getCurrentHex()))
            {
                return true;
            }
        }
        return false;
    }

    public Legion getFirstLegion(MasterHex masterHex)
    {
        for (Legion legion : getAllLegions())
        {
            if (masterHex.equals(legion.getCurrentHex()))
            {
                return legion;
            }
        }
        return null;
    }

    public int getNumFriendlyLegions(MasterHex masterHex, Player player)
    {
        int count = 0;
        List<? extends Legion> legions = player.getLegions();
        for (Legion legion : legions)
        {
            if (masterHex.equals(legion.getCurrentHex()))
            {
                count++;
            }
        }
        return count;
    }

    /**
     * Finds the first legion in a hex not belonging to a certain player.
     *
     * Note that there is no assumption that the player has a legion in that
     * location itself. This method is e.g. used to evaluate moves in the AI.
     *
     * @param masterHex the hex where to look for enemy regions. Not null.
     * @param player the player whose enemies we are looking for. Not null.
     *
     * @return the first legion that is in the specified hex and does not
     *         belong to the given player, null if no such legion exists
     */
    public Legion getFirstEnemyLegion(MasterHex masterHex, Player player)
    {
        assert masterHex != null : "Hex needs to be specified";
        assert player != null : "Player needs to be specified";
        for (Legion legion : getEnemyLegions(player))
        {
            if (masterHex.equals(legion.getCurrentHex()))
            {
                return legion;
            }
        }
        return null;
    }

    /**
     * Return a set of all hexes with engagements.
     *
     * TODO if we can be sure that the activePlayer is set properly, we could
     *      just create a set of all hexes he is on and then check if someone
     *      else occupies any of the same
     */
    // TODO This is the client side version
    public Set<MasterHex> findEngagements()
    {
        Set<MasterHex> result = new HashSet<MasterHex>();
        Map<MasterHex, Player> playersOnHex = new HashMap<MasterHex, Player>();
        for (Player player : players)
        {
            for (Legion legion : player.getLegions())
            {
                MasterHex hex = legion.getCurrentHex();
                if (playersOnHex.get(hex) == null)
                {
                    // no player on that hex found yet, set this one
                    playersOnHex.put(hex, player);
                }
                else
                {
                    if (!playersOnHex.get(hex).equals(player))
                    {
                        // someone else already on the hex -> engagement
                        result.add(hex);
                    }
                }
            }
        }
        return result;
    }

    /** Return set of hexLabels for engagements found. */
    // The GameServerSide version works based on the activePlayer idea...
    // TODO which one to keep?
    /*
    public Set<MasterHex> findEngagements()
    {
        Set<MasterHex> result = new HashSet<MasterHex>();
        Player player = getActivePlayer();

        for (Legion legion : player.getLegions())
        {
            MasterHex hex = legion.getCurrentHex();

            if (getNumEnemyLegions(hex, player) > 0)
            {
                result.add(hex);
            }
        }
        return result;
    }
    */

    public boolean containsOpposingLegions(MasterHex hex)
    {
        Player player = null;
        for (Legion legion : getLegionsByHex(hex))
        {
            if (player == null)
            {
                player = legion.getPlayer();
            }
            else if (legion.getPlayer() != player)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Return a set of all other unengaged legions of the legion's player
     * that have summonables (not sorted in any particular order).
     */
    public List<Legion> findLegionsWithSummonables(Legion summoner)
    {
        List<Legion> result = new ArrayList<Legion>();
        Player player = summoner.getPlayer();
        for (Legion legion : player.getLegions())
        {
            if (!legion.equals(summoner) && legion.hasSummonable()
                && !containsOpposingLegions(legion.getCurrentHex()))
            {
                result.add(legion);
            }
        }
        return result;
    }

    // For making Proposals needed both client and server side
    public Legion getLegionByMarkerId(String markerId)
    {
        LOGGER.severe("getLegionByMarkerId called for markerId " + markerId
            + "in the non-overriden method of game.Game class!!");
        Thread.dumpStack();
        return null;
    }

    /**
     * Set the current turn number. Used only on client side;
     * server side increments directly.
     *
     * @param turn Set this number as current turn number
     */
    public void setTurnNumber(int turn)
    {
        this.turnNumber = turn;
    }

    /**
     * Returns the current turn in the game
     * @return returns the current turn number
     */
    public int getTurnNumber()
    {
        return turnNumber;
    }

    public boolean isPhase(Phase phase)
    {
        return this.phase == phase;
    }

    public void setPhase(Phase phase)
    {
        this.phase = phase;
    }

    public Phase getPhase()
    {
        return phase;
    }

    public boolean isEngagementOngoing()
    {
        if (isPhase(Phase.FIGHT) && engagement != null)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public int getBattleTurnNumber()
    {
        return battle.getBattleTurnNumber();
    }

    public BattleStrike getBattleStrike()
    {
        return battleStrike;
    }
}
