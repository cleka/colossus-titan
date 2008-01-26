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

    private int numLegions;
    private int numCreatures;
    private int titanPower;

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

        buf = data.remove(0);
        setNumLegions(Integer.parseInt(buf));

        buf = data.remove(0);
        setNumCreatures(Integer.parseInt(buf));

        buf = data.remove(0);
        setTitanPower(Integer.parseInt(buf));

        buf = data.remove(0);
        setScore(Integer.parseInt(buf));

        buf = data.remove(0);
        setMulligansLeft(Integer.parseInt(buf));

        clearMarkersAvailable();
        for (String markerId : data)
        {
            addMarkerAvailable(markerId);
        }
    }

    void setNumLegions(int numLegions)
    {
        this.numLegions = numLegions;
    }

    public int getNumLegions()
    {
        return numLegions;
    }

    void setNumCreatures(int numCreatures)
    {
        this.numCreatures = numCreatures;
    }

    @Override
    public int getNumCreatures()
    {
        return numCreatures;
    }

    private void setTitanPower(int titanPower)
    {
        this.titanPower = titanPower;
    }

    // TODO can't we just use the calculated version from the base class?
    @Override
    public int getTitanPower()
    {
        return titanPower;
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

    @Override
    public List<LegionClientSide> getLegions()
    {
        return client.getLegionsByPlayer(this);
    }

    void removeAllLegions()
    {
        for (Legion legion : getLegions())
        {
            client.removeLegion(legion);
        }
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