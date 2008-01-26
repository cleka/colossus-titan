package net.sf.colossus.client;


import java.util.List;

import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.server.CustomRecruitBase;
import net.sf.colossus.util.Split;
import net.sf.colossus.webcommon.InstanceTracker;


/**
 * This class holds client-side version of a player.
 * 
 * @version $Id$ 
 * @author David Ripton
 */

public final class PlayerClientSide extends Player
{
    private final Client client;

    private PredictSplits predictSplits;

    /** 
     * Two-stage initialization at the moment, only some data here, the rest comes
     * through {@link #update(String)}.
     * 
     * TODO: the object should be properly initialized in the constructor
     */
    PlayerClientSide(Client client, String playerName, int number)
    {
        super(client.getGame(), playerName, number);
        this.client = client;
        CustomRecruitBase.addPlayerInfo(this);
        InstanceTracker.register(this, playerName);
    }

    /** Takes a colon-separated string of form
     *  dead:name:tower:color:elim:legions:markers:creatures:value:titan:score
     */
    void update(String infoString)
    {
        List<String> data = Split.split(":", infoString);
        String buf;

        buf = data.remove(0);
        setDead(Boolean.parseBoolean(buf));

        setName(data.remove(0));

        setStartingTower(client.getGame().getVariant().getMasterBoard()
            .getHexByLabel(data.remove(0)));

        setColor(data.remove(0));

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

    public int numMobileLegions()
    {
        int count = 0;

        for (Legion legion : getLegions())
        {
            if (!legion.hasMoved())
            {
                count++;
            }
        }
        return count;
    }

    public PredictSplits getPredictSplits()
    {
        return predictSplits;
    }

    public void initPredictSplits(Legion rootLegion, List<String> creatureNames)
    {
        this.predictSplits = new PredictSplits(rootLegion.getMarkerId(),
            creatureNames);
    }
}