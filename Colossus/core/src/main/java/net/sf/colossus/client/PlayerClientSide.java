package net.sf.colossus.client;


import java.util.List;

import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.server.CustomRecruitBase;
import net.sf.colossus.util.Split;
import net.sf.colossus.webcommon.InstanceTracker;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 * This class holds client-side version of a player.
 * 
 * @version $Id$ 
 * @author David Ripton
 */

public final class PlayerClientSide extends Player
{
    private Client client;

    private int numLegions;
    private int numCreatures;
    private int titanPower;
    private int score;
    private int mulligansLeft;

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

        setStartingTower(data.remove(0));

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

    void setTitanPower(int titanPower)
    {
        this.titanPower = titanPower;
    }

    public int getTitanPower()
    {
        return titanPower;
    }

    void setScore(int score)
    {
        this.score = score;
    }

    public int getScore()
    {
        return score;
    }

    public boolean canTitanTeleport()
    {
        return (score >= TerrainRecruitLoader.getTitanTeleportValue());
    }

    void setMulligansLeft(int mulligansLeft)
    {
        this.mulligansLeft = mulligansLeft;
    }

    public int getMulligansLeft()
    {
        return mulligansLeft;
    }

    boolean hasTeleported()
    {
        for (Legion info : getLegions())
        {
            if (info.hasTeleported())
            {
                return true;
            }
        }
        return false;
    }

    /** Return the number of this player's legions that have moved. */
    public int numLegionsMoved()
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

    public int numMobileLegions()
    {
        return getNumLegions() - numLegionsMoved();
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

    public void setClientNull()
    {
        this.client = null;
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