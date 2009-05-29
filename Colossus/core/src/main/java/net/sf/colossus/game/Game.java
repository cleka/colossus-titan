package net.sf.colossus.game;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.IVariantKnower;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * An ongoing game in Colossus.
 *
 * As opposed to {@link Variant} this class holds information about an ongoing game
 * and its status.
 */
public abstract class Game
{
    private static final Logger LOGGER = Logger
        .getLogger(Game.class.getName());
    /**
     * The variant played in this game.
     */
    private Variant variant;

    /**
     * The state of the different players in the game.
     */
    protected final List<Player> players = new ArrayList<Player>();

    /**
     * The caretaker takes care of managing the available and dead creatures.
     */
    private final Caretaker caretaker;

    /**
     * Some object to ask about current variant, in case / as long as it is
     * not properly passed in right away.
     */
    private final IVariantKnower variantKnower;

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

    /**
     *  If a battle is ongoing, the masterBoard hex, attacker and defender
     */
    private Legion attacker;
    private Legion defender;
    private MasterHex battleSite;


    /**
     * Create a Game object.
     *
     * @param variant The variant object, might right now still be null (game
     *        is created before Client gets/knows the variant name)
     * @param playerNames Names of the players, not used yet
     * @param variantKnower An object to ask for the current variant, will be
     *        called first time someone asks Variant from Game.
     */
    public Game(Variant variant, String[] playerNames,
        IVariantKnower variantKnower)
    {
        // NOTE variant/variantKnower needs to be assigned before caretaker,
        // because caretaker asks Game for the variant
        this.variant = variant;
        this.variantKnower = variantKnower;

        this.caretaker = new Caretaker(this);
    }

    public Variant getVariant()
    {
        if (variant == null)
        {
            // TODO temporary solution until all game creations pass in the
            //      used variant right away
            variant = variantKnower.getTheCurrentVariant();
        }
        return variant;
    }

    public Collection<Player> getPlayers()
    {
        assert players.size() != 0 : "getPlayers called before player info set (size==0)!";
        return Collections.unmodifiableCollection(players);
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
        return gameOver;
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


    public void setEngagementData(MasterHex hex, Legion attacker,
        Legion defender)
    {
        this.battleSite = hex;
        this.attacker = attacker;
        this.defender = defender;
    }

    public MasterHex getBattleSite()
    {
        return battleSite;
    }

    public Legion getDefender()
    {
        return defender;
    }

    public Legion getAttacker()
    {
        return attacker;
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

    public String getPhaseName()
    {
        if (phase != null)
        {
            return phase.toString();
        }
        return "";
    }

}
