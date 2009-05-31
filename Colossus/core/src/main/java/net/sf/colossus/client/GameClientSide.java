package net.sf.colossus.client;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.Game;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.util.CollectionHelper;
import net.sf.colossus.util.Predicate;
import net.sf.colossus.util.Split;
import net.sf.colossus.variant.IVariantKnower;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


public class GameClientSide extends Game implements IOracle
{
    private Client client;

    /**
     * This is used as a placeholder for activePlayer and battleActivePlayer since they
     * are sometimes accessed when they are not available.
     *
     * TODO this is a hack. Those members should just not be accessed at times where they
     * are not available. It seems to happen during startup (the not yet set case) and in
     * some GUI parts after battles, when battleActivePlayer has been reset already.
     */
    private final PlayerClientSide noone;

    private Player activePlayer;

    private BattleClientSide battle = null;

    public GameClientSide(Variant variant, String[] playerNames,
        IVariantKnower variantKnower)
    {
        super(variant, playerNames, variantKnower);

        // TODO fix this dummy constructor args
        this.noone = new PlayerClientSide(this, "", 0);

        this.activePlayer = noone;

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

    public Player getNoonePlayer()
    {
        return noone;
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


    public void setActivePlayer(Player player)
    {
        activePlayer = player;
    }

    public Player getActivePlayer()
    {
        return activePlayer;
    }


    public void initBattle(MasterHex hex, int battleTurnNumber,
        Player battleActivePlayer, BattlePhase battlePhase, Legion attacker,
        Legion defender)
    {
        this.battle = new BattleClientSide(this, attacker, defender, hex
            .getTerrain());

        battle.init(battleTurnNumber, battleActivePlayer, battlePhase);

    }

    public BattleClientSide getBattle()
    {
        return battle;
    }

    public boolean isBattleOngoing()
    {
        return battle != null;
    }

    public BattlePhase getBattlePhase()
    {
        assert battle != null : "No battle phase if there is no battle!";
        return battle.getBattlePhase();
    }

    public void setBattlePhase(BattlePhase battlePhase)
    {
        battle.setBattlePhase(battlePhase);
    }

    public boolean isBattlePhase(BattlePhase phase)
    {
        return battle.getBattlePhase() == phase;
    }

    public String getBattlePhaseName()
    {
        return battle.getBattlePhaseName();
    }

    public void setBattleActivePlayer(Player battleActivePlayer)
    {
        battle.setBattleActivePlayer(battleActivePlayer);
    }

    public void setBattleTurnNumber(int battleTurnNumber)
    {
        battle.setBattleTurnNumber(battleTurnNumber);
    }

    public int getBattleTurnNumber()
    {
        return battle.getBattleTurnNumber();
    }

    public Legion getBattleActiveLegion()
    {
        return battle.getBattleActiveLegion();
    }

    public Player getBattleActivePlayer()
    {
        if (battle == null)
        {
            return null;
        }
        return battle.getBattleActivePlayer();
    }

    public void cleanupBattle()
    {
        if (battle != null)
        {
            battle.cleanupBattle();
            battle = null;
        }
    }


    /** Return a list of Strings.  Use the proper string for titans and
     *  unknown creatures. */
    // public for IOracle
    public List<String> getLegionImageNames(Legion legion)
    {
        LegionClientSide info = (LegionClientSide)legion;
        if (info != null)
        {
            return info.getImageNames();
        }
        return new ArrayList<String>();
    }

    /** Return a list of Booleans */
    // public for IOracle
    public List<Boolean> getLegionCreatureCertainties(Legion legion)
    {
        LegionClientSide info = (LegionClientSide)legion;
        if (info != null)
        {
            return info.getCertainties();
        }
        else
        {
            // TODO: is this the right thing?
            List<Boolean> l = new ArrayList<Boolean>(10); // just longer then max
            for (int idx = 0; idx < 10; idx++)
            {
                l.add(Boolean.valueOf(true)); // all true
            }
            return l;
        }
    }


}
