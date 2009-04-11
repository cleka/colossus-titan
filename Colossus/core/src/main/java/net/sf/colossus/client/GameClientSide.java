package net.sf.colossus.client;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.colossus.game.Game;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.util.CollectionHelper;
import net.sf.colossus.util.Predicate;
import net.sf.colossus.util.Split;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


public class GameClientSide extends Game
{
    private Client client;

    public GameClientSide(Variant variant, String[] playerNames)
    {
        super(variant, playerNames);
    }

    public void setClient(Client client)
    {
        this.client = client;
    }

    public PlayerClientSide initPlayerInfo(List<String> infoStrings,
        String searchedName)
    {
        int numPlayers = infoStrings.size();
        PlayerClientSide owningPlayer = null;

        // first time we get the player infos, store them locally and set our
        // own, too -- which has been a fake until now
        for (int i = 0; i < numPlayers; i++)
        {
            List<String> data = Split.split(":", infoStrings.get(i));
            String playerName = data.get(1);
            PlayerClientSide info = new PlayerClientSide(this, playerName, i);
            players.add(info);
            if (playerName.equals(searchedName))
            {
                owningPlayer = info;
            }
        }
        return owningPlayer;
    }

    public void updatePlayerInfo(List<String> infoStrings)
    {
        for (int i = 0; i < infoStrings.size(); i++)
        {
            PlayerClientSide player = (PlayerClientSide)players.get(i);
            player.update(infoStrings.get(i));
        }
    }

    // TODO not needed right now on client side - SCT compares to
    // "null" instead. Check whether to remove when pulling up?
    /**
     * Resolve playerName into Player object. Name might be null,
     * then returns null.
     * @param playerName
     * @return The player object for given player name, null if name was null
     */
    Player getPlayerByNameIgnoreNull(String playerName)
    {
        if (playerName == null)
        {
            return null;
        }
        else
        {
            return getPlayerByName(playerName);
        }
    }

    /**
     * Resolve playerName into Player object.
     * Name must not be null. If no player for given name found,
     * it would throw IllegalArgumentException
     * @param playerName
     * @return Player object for given name.
     */
    Player getPlayerByName(String playerName)
    {
        assert playerName != null : "Name for player to find must not be null!";

        for (Player player : players)
        {
            if (player.getName().equals(playerName))
            {
                return player;
            }
        }
        throw new IllegalArgumentException("No player object found for name '"
            + playerName + "'");
    }

    private Player getPlayerUsingColor(String shortColor)
    {
        assert this.players.size() > 0 : "Client side player list not yet initialized";
        assert shortColor != null : "Parameter must not be null";

        // Stage 1: See if the player who started with this color is alive.
        for (Player info : players)
        {
            if (shortColor.equals(info.getShortColor()) && !info.isDead())
            {
                return info;
            }
        }

        // Stage 2: He's dead.  Find who killed him and see if he's alive.
        for (Player info : players)
        {
            if (info.getPlayersElim().indexOf(shortColor) != -1)
            {
                // We have the killer.
                if (!info.isDead())
                {
                    return info;
                }
                else
                {
                    return getPlayerUsingColor(info.getShortColor());
                }
            }
        }
        return null;
    }

    public Player getPlayerByMarkerId(String markerId)
    {
        assert markerId != null : "Parameter must not be null";

        String shortColor = markerId.substring(0, 2);
        return getPlayerUsingColor(shortColor);
    }

    protected List<Legion> getEnemyLegions(final Player player)
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

    public Legion getFirstEnemyLegion(MasterHex hex, Player player)
    {
        for (Player otherPlayer : players)
        {
            if (!otherPlayer.equals(player))
            {
                for (Legion legion : otherPlayer.getLegions())
                {
                    if (legion.getCurrentHex().equals(hex))
                    {
                        return legion;
                    }
                }
            }
        }
        return null;
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

    public List<LegionClientSide> getLegionsByHex(MasterHex hex)
    {
        assert hex != null : "No hex given to find legions on.";
        List<LegionClientSide> legions = new ArrayList<LegionClientSide>();
        for (Player player : players)
        {
            for (LegionClientSide legion : ((PlayerClientSide)player)
                .getLegions())
            {
                if (hex.equals(legion.getCurrentHex()))
                {
                    legions.add(legion);
                }
            }
        }
        return legions;
    }

    boolean isOccupied(MasterHex hex)
    {
        return !getLegionsByHex(hex).isEmpty();
    }

    /** Return the average point value of all legions in the game. */
    public int getAverageLegionPointValue()
    {
        int totalValue = 0;
        int totalLegions = 0;

        for (Player player : players)
        {
            totalLegions += player.getLegions().size();
            totalValue += player.getTotalPointValue();
        }
        return (int)(Math.round((double)totalValue / totalLegions));
    }

    /**
     * Return a set of all hexes with engagements.
     *
     * TODO if we can be sure that the activePlayer is set properly, we could
     *      just create a set of all hexes he is on and then check if someone
     *      else occupies any of the same
     */
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

    boolean isEngagement(MasterHex hex)
    {
        List<LegionClientSide> legions = getLegionsByHex(hex);
        if (legions.size() == 2)
        {
            Legion info0 = legions.get(0);
            Player player0 = info0.getPlayer();

            Legion info1 = legions.get(1);
            Player player1 = info1.getPlayer();

            return !player0.equals(player1);
        }
        return false;
    }

    // TODO: move method from Client to here, or even to game.Game?
    @Override
    public Legion getLegionByMarkerId(String markerId)
    {
        return client.getLegion(markerId);
    }

}
