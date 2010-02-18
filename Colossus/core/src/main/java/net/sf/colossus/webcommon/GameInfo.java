package net.sf.colossus.webcommon;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;


/**
 *  One object of this this class represents a game for which players/users
 *  have enrolled to play it together.
 *  It starts in state "PROPOSED" as type either instantly or scheduled.
 *  Then its state will change along the sequence of states
 *  PROPOSED, DUE, ACTIVATED, STARTING, READY_TO_CONNECT, RUNNING, ENDING
 *  as denoted in the GameState enum.
 *
 *  The actual running/starting of the game will be handled by different
 *  classes, namely GameOnServer and (to be done) GameOnClient.
 *
 *  The same class is also used at client side, but only part of the data
 *  is used there (e.g. the user has only a name, not a socket).
 *
 *  @author Clemens Katzer
 */
public class GameInfo
{
    private static final Logger LOGGER = Logger.getLogger(GameInfo.class
        .getName());

    private static int nextFreeGameId = 1;

    private String gameId;

    private GameType type;
    private GameState state;
    /** temporary backup during startingAttempt */
    private GameState oldState;
    private User startingUser = null;

    private int portNr = -1;
    private String hostingHost = "";
    private IGameRunner gameRunner;

    private String initiator;
    private String variant;
    private String viewmode;
    private final boolean autosave = true;

    // those 3 should still be added at client side:
    private String eventExpiring;
    private boolean unlimitedMulligans;
    private boolean balancedTowers;

    private int min;
    private int target;
    private int max;
    private int onlineCount;

    // next three only needed for scheduled games
    private long startTime = 0;
    private int duration = 0;
    private String summary = "";
    // private String infoText = "";

    private int enrolledPlayers;

    private ArrayList<User> players = null;

    // used on server side, to create a game proposed by client

    private GameInfo(GameType type)
    {
        this.gameId = String.valueOf(getNextFreeGameId());
        this.type = type;

        this.state = GameState.PROPOSED;

        this.enrolledPlayers = 0;
        this.players = new ArrayList<User>();
    }

    /** Server calls this to set it high enough that existing directories
     *  in games work directory are not overwritten
     *
     *  @param id Next games should have higher number than given id
     */
    public static void setNextFreeGameId(int id)
    {
        nextFreeGameId = id;
    }

    private static int getNextFreeGameId()
    {
        return nextFreeGameId++;
    }

    public GameInfo(String initiator, String variant, String viewmode,
        long startTime, int duration, String summary, String expire,
        boolean unlimitedMulligans, boolean balancedTowers, int min,
        int target, int max)
    {
        this(startTime == -1 ? GameType.INSTANT : GameType.SCHEDULED);

        this.initiator = initiator;
        this.variant = variant;
        this.viewmode = viewmode;
        this.eventExpiring = expire;
        this.unlimitedMulligans = unlimitedMulligans;
        this.balancedTowers = balancedTowers;
        this.min = min;
        this.target = target;
        this.max = max;
        this.onlineCount = 0;

        this.startTime = startTime;
        this.duration = duration;
        this.summary = summary;

        this.enrolledPlayers = 0;
        this.players = new ArrayList<User>();

        LOGGER.log(Level.FINEST,
            "A new potential game was created!! - variant " + variant
                + " viewmode " + viewmode);

        // System.out.println("NEW GameInfo server side, " + this.toString());
    }

    // ================= now the stuff for the client side ===============

    // used on client side, to restore a proposed game sent by server
    public GameInfo(String gameId, boolean onServer)
    {
        this.gameId = gameId;
        if (onServer)
        {
            int intGameId = Integer.parseInt(gameId);
            if (nextFreeGameId <= intGameId)
            {
                nextFreeGameId = intGameId + 1;
            }
        }
        this.players = new ArrayList<User>();
    }

    public static GameInfo fromString(String[] tokens,
        HashMap<String, GameInfo> games, boolean fromFile)
    {
        GameInfo gi;

        // tokens[0] is the command
        String gameId = tokens[1];
        String key = gameId;

        if (games.containsKey(gameId))
        {
            // use the object webclient has created earlier
            gi = games.get(key);
            // System.out.println("Found already, updating");
        }
        else
        {
            gi = new GameInfo(gameId, fromFile);
            games.put(key, gi);
            // System.out.println("Creating a new one GameInfo ");
        }

        int j = 2;
        gi.type = GameType.valueOf(tokens[j++]);
        gi.state = GameState.valueOf(tokens[j++]);
        gi.initiator = tokens[j++];

        // System.out.println("fromString, state=" + gi.state + ")");
        // System.out.println("tokens: " + tokens.toString());

        gi.variant = tokens[j++];
        gi.viewmode = tokens[j++];
        gi.startTime = Long.parseLong(tokens[j++]);
        gi.duration = Integer.parseInt(tokens[j++]);
        gi.summary = tokens[j++];
        gi.eventExpiring = tokens[j++];
        gi.unlimitedMulligans = Boolean.valueOf(tokens[j++]).booleanValue();
        gi.balancedTowers = Boolean.valueOf(tokens[j++]).booleanValue();
        gi.min = Integer.parseInt(tokens[j++]);
        gi.target = Integer.parseInt(tokens[j++]);
        gi.max = Integer.parseInt(tokens[j++]);
        gi.onlineCount = Integer.parseInt(tokens[j++]);

        int lastIndex = j;
        gi.enrolledPlayers = Integer.parseInt(tokens[lastIndex]);

        ArrayList<User> players = new ArrayList<User>();
        int i = 1;
        while (i <= gi.enrolledPlayers)
        {
            String name = tokens[lastIndex + i];
            User user = new User(name);
            players.add(user);
            i++;
        }

        gi.players = players;
        return gi;
    }

    public String toString(String sep)
    {
        StringBuilder playerList = new StringBuilder();
        Iterator<User> it = players.iterator();
        while (it.hasNext())
        {
            playerList.append(sep);
            User user = it.next();
            playerList.append(user.getName());
        }

        String message = gameId + sep + type.toString() + sep
            + state.toString() + sep + initiator + sep + variant + sep
            + viewmode + sep + startTime + sep + duration + sep + summary
            + sep + eventExpiring + sep + unlimitedMulligans + sep
            + balancedTowers + sep + min + sep + target + sep + max + sep
            + onlineCount + sep + enrolledPlayers + playerList.toString();

        return message;
    }

    public void setState(GameState state)
    {
        this.state = state;
    }

    public GameState getGameState()
    {
        return this.state;
    }

    public boolean isScheduledGame()
    {
        boolean isSch = (this.type == GameType.SCHEDULED);
        return isSch;
    }

    public String getStateString()
    {
        return this.state.toString();
    }

    public String getGameId()
    {
        return this.gameId;
    }

    public void setGameId(String val)
    {
        this.gameId = val;
    }

    public void setGameRunner(IGameRunner gr)
    {
        this.gameRunner = gr;
    }

    public IGameRunner getGameRunner()
    {
        return gameRunner;
    }

    public int getPort()
    {
        return this.portNr;
    }

    public void setPort(int nr)
    {
        this.portNr = nr;
    }

    public void setHostingHost(String host)
    {
        hostingHost = host;
    }

    public String getHostingHost()
    {
        return hostingHost;
    }

    public String getInitiator()
    {
        return initiator;
    }

    public void setInitiator(String val)
    {
        initiator = val;
    }

    public Long getStartTime()
    {
        return Long.valueOf(startTime);
    }

    public void setStartTime(String val)
    {
        startTime = Long.parseLong(val);
    }

    public Integer getDuration()
    {
        return Integer.valueOf(duration);
    }

    public void setDuration(String val)
    {
        duration = Integer.parseInt(val);
    }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary(String val)
    {
        summary = val;
    }

    public String getVariant()
    {
        return variant;
    }

    public void setVariant(String val)
    {
        variant = val;
    }

    public String getViewmode()
    {
        return viewmode;
    }

    public boolean getAutosave()
    {
        return autosave;
    }

    public void setViewmode(String val)
    {
        viewmode = val;
    }

    public String getEventExpiring()
    {
        return eventExpiring;
    }

    public void setEventExpiring(String val)
    {
        eventExpiring = val;
    }

    public boolean getUnlimitedMulligans()
    {
        return unlimitedMulligans;
    }

    public void setUnlimitedMulligans(boolean val)
    {
        unlimitedMulligans = val;
    }

    public boolean getBalancedTowers()
    {
        return balancedTowers;
    }

    public void setBalancedTowers(boolean val)
    {
        balancedTowers = val;
    }

    /**
     * Have enough players enrolled (at least "min")
     * @return
     */
    public boolean hasEnoughPlayers()
    {
        return enrolledPlayers >= min;
    }

    /**
     * Have enough players enrolled (at least "min")
     * @return
     */
    public boolean allEnrolledOnline()
    {
        return onlineCount >= enrolledPlayers;
    }

    /**
     * Has the scheduled time come?
     * @return true if the game can be started according to schedule
     */
    public boolean isDue()
    {
        long now = new Date().getTime();
        return now > startTime;
    }

    public Integer getMin()
    {
        return Integer.valueOf(min);
    }

    public void setMin(Integer val)
    {
        min = val.intValue();
    }

    public Integer getTargetInteger()
    {
        return Integer.valueOf(target);
    }

    public int getTarget()
    {
        return target;
    }

    public void setTarget(Integer val)
    {
        target = val.intValue();
    }

    public Integer getMax()
    {
        return Integer.valueOf(max);
    }

    public void setMax(Integer val)
    {
        max = val.intValue();
    }

    public Integer getEnrolledCount()
    {
        return Integer.valueOf(enrolledPlayers);
    }

    public int getOnlineCount()
    {
        return onlineCount;
    }

    public void setOnlineCount(int count)
    {
        onlineCount = count;
    }

    public void setEnrolledCount(Integer val)
    {
        enrolledPlayers = val.intValue();
    }

    public ArrayList<User> getPlayers()
    {
        return this.players;
    }

    public String getPlayerListAsString()
    {
        if (players == null)
        {
            LOGGER.warning("Tried to get player list as string for gameId "
                + gameId + ", but player list == null");
            return "<none>";
        }

        StringBuilder playerList = new StringBuilder("");
        if (players.size() == 0)
        {
            return "<none>";
        }

        for (User u : players)
        {
            if (playerList.length() != 0)
            {
                playerList.append(", ");
            }
            playerList.append(u.getName());
        }

        return playerList.substring(0);
    }

    public boolean reEnrollIfNecessary(User newUser)
    {
        String newName = newUser.getName();
        Iterator<User> it = players.iterator();
        boolean found = false;
        while (!found && it.hasNext())
        {
            User user = it.next();
            String name = user.getName();
            if (newName.equals(name))
            {
                it.remove();
                found = true;
            }
        }
        it = null;
        if (found)
        {
            players.add(newUser);
            LOGGER.finest("Re-Enrolled user " + newName + " to game "
                + getGameId());
            LOGGER.finest("Players now: " + getPlayerListAsString());
        }
        else
        {
            LOGGER.finest("User " + newName + " not in game " + getGameId()
                + " - nothing to do.");
        }

        return found;
    }

    public boolean isEnrolled(String searchName)
    {
        boolean found = false;
        for (User u : players)
        {
            if (searchName.equals(u.getName()))
            {
                found = true;
                break;
            }
        }
        return found;
    }

    public void setPlayerList(ArrayList<User> playerlist)
    {
        players = playerlist;
    }

    /**
     * When a user logged in or out, this is called for every GameInfo to update
     * how many of the enrolled players are currently online.
     *
     * @return true if the count of online users was changed i.e. GameInfo
     * needs to be updated to all clients
     */
    public boolean updateOnline()
    {
        boolean changed = false;

        int found = 0;
        for (User u : players)
        {
            if (User.isUserOnline(u))
            {
                found++;
            }
        }
        if (found != onlineCount)
        {
            onlineCount = found;
            changed = true;
        }
        return changed;
    }

    /*
     * return reason why fail, or null if ok
     */
    public String enroll(User user)
    {
        String reason = null;

        if (enrolledPlayers < max)
        {
            synchronized (players)
            {
                players.add(user);
                enrolledPlayers++;
            }
        }
        else
        {
            reason = "Game is full";
        }

        return reason;
    }

    public String unenroll(User user)
    {
        String reason = null;

        synchronized (players)
        {
            int index = players.indexOf(user);
            if (index != -1)
            {
                players.remove(index);
                enrolledPlayers--;
                if (players.size() != enrolledPlayers)
                {
                    LOGGER.log(Level.SEVERE,
                        "players.size() != enrolledPlayers!!");
                }
            }
            else
            {
                reason = "Player " + user.getName()
                    + " to unenroll not found in game " + gameId;
            }
        }

        return reason;
    }

    public void storeToOptionsObject(Options gameOptions,
        String localPlayerName, boolean noAIs)
    {
        if (this.portNr == -1)
        {
            this.portNr = Constants.defaultPort;
        }
        // XXX get port from gameinfo
        gameOptions.setOption(Options.serveAtPort, this.portNr);

        // XXX get host from gameinfo
        // XXX set host in gameinfo, send to server/client

        gameOptions.setOption(Options.variant, getVariant());
        gameOptions.setOption(Options.viewMode, getViewmode());
        gameOptions.setOption(Options.autosave, getAutosave());
        gameOptions.setOption(Options.eventExpiring, getEventExpiring());
        gameOptions.setOption(Options.unlimitedMulligans,
            getUnlimitedMulligans());
        gameOptions.setOption(Options.balancedTowers, getBalancedTowers());

        // gameOptions.setOption(Options.autoQuit, true);
        String name;
        String type;
        Iterator<User> it = getPlayers().iterator();

        int numPlayers = getTarget();
        if (noAIs)
        {
            numPlayers = getPlayers().size();
        }

        for (int i = 0; i < numPlayers; i++)
        {
            if (it.hasNext())
            {
                User u = it.next();

                name = u.getName();
                if (localPlayerName != null && name.equals(localPlayerName))
                {
                    type = Constants.human;
                    // use user real name;
                }
                else
                {
                    type = Constants.network;
                    name = Constants.byClient;

                }
            }
            else
            {
                type = Constants.anyAI;
                name = Constants.byColor;
            }
            gameOptions.setOption(Options.playerName + i, name);
            gameOptions.setOption(Options.playerType + i, type);
        }

        gameOptions.setOption(Options.autoStop, true);

        return;
    }

    public boolean relevantForSaving()
    {
        return state.equals(GameState.PROPOSED) || state.equals(GameState.DUE);
    }

    public boolean isStartable()
    {
        return state.equals(GameState.PROPOSED) || state.equals(GameState.DUE);
    }

    public void markStarting(User starter)
    {
        this.startingUser = starter;
        this.oldState = state;
        this.state = GameState.ACTIVATED;
    }

    public boolean isStarting()
    {
        return startingUser != null;
    }

    public void cancelStarting()
    {
        this.state = oldState;
        this.startingUser = null;
    }

    public User getStartingUser()
    {
        return startingUser;
    }

    /**
     *  Enum for the possible TYPES of a game
     *  (scheduled or instant, perhaps later also template?)
     */
    public static enum GameType
    {
        SCHEDULED, INSTANT;
        // TEMPLATE ?
    }

    /**
     *  Enum for the possible states of a game:
     */
    public static enum GameState
    {
        PROPOSED, DUE, ACTIVATED, STARTING, READY_TO_CONNECT, RUNNING, ENDING;
    }

}
