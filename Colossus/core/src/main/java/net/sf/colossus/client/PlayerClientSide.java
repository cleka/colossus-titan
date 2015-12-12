package net.sf.colossus.client;


import java.util.List;

import net.sf.colossus.game.Game;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.server.CustomRecruitBase;
import net.sf.colossus.util.InstanceTracker;
import net.sf.colossus.util.Split;
import net.sf.colossus.variant.CreatureType;


/**
 * This class holds client-side version of a player.
 *
 * @author David Ripton
 */
public final class PlayerClientSide extends Player
{
    private PredictSplits predictSplits;

    /**
     * Two-stage initialization at the moment, only some data here, the rest comes
     * through {@link #update(String)}.
     *
     * TODO: the object should be properly initialized in the constructor
     */
    PlayerClientSide(Game game, String playerName, int number)
    {
        super(game, playerName, number);
        CustomRecruitBase.addPlayerClientSide(this);
        InstanceTracker.register(this, playerName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<LegionClientSide> getLegions()
    {
        return (List<LegionClientSide>)super.getLegions();
    }

    /** Takes a colon-separated string of form
     *  dead:name:tower:color:elim:legions:markers:creatures:value:titan:score
     *
     *  TODO this is part of the network protocol and should be somewhere in there
     */
    void update(String infoString)
    {
        List<String> data = Split.split(":", infoString);
        String buf;

        buf = data.remove(0);
        setDead(Boolean.parseBoolean(buf));

        setName(data.remove(0));

        // TODO This is a workaround - fix better, init variant at proper
        // time somehow proper way?
        try
        {
            String towerHex = data.remove(0);
            setStartingTower(getGame().getVariant().getMasterBoard()
                .getHexByLabel(towerHex));
        }
        catch (NullPointerException e)
        {
            // When loading a game in remote client, in beginning variant is
            // not loaded yes, so that may cause NPE which we ignore here.
            // After syncOptions variant name is known,
            // and in initBoard it is loaded, so all future updates go fine.
        }

        setColor(PlayerColor.getByName(data.remove(0)));

        setType(data.remove(0));

        setPlayersElim(data.remove(0));

        buf = data.remove(0); // numLegions -- we can calculate that later

        buf = data.remove(0); // numCreatures -- we can calculate that later

        buf = data.remove(0);
        int titanPower = Integer.parseInt(buf);

        buf = data.remove(0);
        setScore(Integer.parseInt(buf));
        assert titanPower == getTitanPower() : "Titan strength inconsistent between client and server";

        buf = data.remove(0);
        setMulligansLeft(Integer.parseInt(buf));

        clearMarkersAvailable();
        for (String markerId : data)
        {
            addMarkerAvailable(markerId);
        }
    }

    public void updateValues(boolean isDead, String eliminatedPlayers,
        int score, int mulligansLeft, String freeMarkers)
    {
        setDead(isDead);
        setPlayersElim(eliminatedPlayers);
        setScore(score);
        setMulligansLeft(mulligansLeft);
        clearMarkersAvailable();
        List<String> markerIds = Split.split(",", freeMarkers);
        if (!freeMarkers.equals(""))
        {
            for (String markerId : markerIds)
            {
                addMarkerAvailable(markerId);
            }
        }
    }

    public PredictSplits getPredictSplits()
    {
        return predictSplits;
    }

    public void initPredictSplits(Legion rootLegion,
        List<CreatureType> creatures)
    {
        this.predictSplits = new PredictSplits(rootLegion.getMarkerId(),
            creatures, getGame().getVariant());
    }

    @Override
    public LegionClientSide getLegionByMarkerId(String markerId)
    {
        return (LegionClientSide)super.getLegionByMarkerId(markerId);
    }
}