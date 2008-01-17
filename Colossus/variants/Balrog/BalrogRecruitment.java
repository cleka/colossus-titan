package Balrog;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.PlayerClientSide;
import net.sf.colossus.game.Player;
import net.sf.colossus.server.CreatureTypeServerSide;
import net.sf.colossus.server.CustomRecruitBase;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.variant.CreatureType;


/**
 * Custom class to allow recruitment of Balrog in 6-tower game.
 * @version $Id$
 * @author Romain Dolbeau
 */
public class BalrogRecruitment extends CustomRecruitBase
{
    private static final Logger LOGGER = Logger
        .getLogger(BalrogRecruitment.class.getName());

    /**
     * Maps a player to their previous points count.
     */
    private final Map<Player, Integer> playerToOldScore = Collections
        .synchronizedMap(new HashMap<Player, Integer>());

    private final static int balrogValue = 300;
    private final static String balrogPrefix = "Balrog";

    @Override
    public List<CreatureTypeServerSide> getAllPossibleSpecialRecruiters(String terrain)
    {
        // Balrog recruited in Tower, where everything recruit anyway.
        return new ArrayList<CreatureTypeServerSide>();
    }

    @Override
    public List<CreatureTypeServerSide> getAllPossibleSpecialRecruits(String terrain)
    {
        List<CreatureTypeServerSide> temp = new ArrayList<CreatureTypeServerSide>();
        Iterator<CreatureType> it = VariantSupport.getCurrentVariant()
            .getCreatureTypes().iterator();
        while (it.hasNext())
        {
            CreatureTypeServerSide cre = (CreatureTypeServerSide)it.next();
            if (cre.getName().startsWith(balrogPrefix))
            {
                temp.add(cre);
            }
        }
        return temp;
    }

    @Override
    public List<CreatureTypeServerSide> getPossibleSpecialRecruiters(String terrain,
        String hexLabel)
    {
        // Balrog recruited in Tower, where everything recruit anyway.
        return new ArrayList<CreatureTypeServerSide>();
    }

    @Override
    public List<CreatureTypeServerSide> getPossibleSpecialRecruits(String terrain,
        String hexLabel)
    {
        List<CreatureTypeServerSide> temp = new ArrayList<CreatureTypeServerSide>();

        if (hexLabel == null)
        {
            return temp;
        }

        // need to update, as we might have earned points in the Engagement
        // phase and recruit in the Recruit phase
        updateBalrogCount(hexLabel);

        String name = balrogPrefix + hexLabel;

        if (getCount(name) > 0)
        {
            temp.add((CreatureTypeServerSide)VariantSupport.getCurrentVariant()
                .getCreatureByName(name));
        }
        return temp;
    }

    @Override
    public int numberOfRecruiterNeeded(String recruiter, String recruit,
        String terrain, String hexLabel)
    {
        return 0;
    }

    @Override
    protected void changeOfTurn(int newActivePlayer)
    {
        Set<String> towerSet = VariantSupport.getCurrentVariant()
            .getMasterBoard().getTowerSet();

        // update all Balrogs, as a lost fight may have given points
        // to a different Player
        Iterator<String> it = towerSet.iterator();
        while (it.hasNext())
        {
            updateBalrogCount(it.next());
        }
    }

    private synchronized void updateBalrogCount(String hexLabel)
    {
        String name = balrogPrefix + hexLabel;

        PlayerClientSide pi = findPlayerWithStartingTower(hexLabel);

        if (pi == null)
        {
            LOGGER.log(Level.FINEST, "CUSTOM: no player info for hex "
                + hexLabel);
            return;
        }

        int oldscore;
        int newscore;
        int alreadyNumber;
        int nowNumber;

        synchronized (playerToOldScore)
        {
            Integer score = playerToOldScore.remove(pi);

            if (score == null)
            {
                oldscore = 0;
            }
            else
            {
                oldscore = score.intValue();
            }
            newscore = pi.getScore();

            playerToOldScore.put(pi, new Integer(newscore));

            alreadyNumber = (oldscore / balrogValue);
            nowNumber = (newscore / balrogValue);
        }

        if (!VariantSupport.getCurrentVariant().isCreature(name))
        {
            LOGGER.log(Level.SEVERE, "CUSTOM: Balrog by the name of " + name
                + " doesn't exist !");
            return;
        }

        CreatureTypeServerSide cre = (CreatureTypeServerSide)VariantSupport.getCurrentVariant()
            .getCreatureByName(name);
        ((CreatureBalrog)cre).setNewMaxCount(nowNumber);

        int difference = nowNumber - alreadyNumber;
        int newcount = getCount(name) + difference;

        setCount(name, newcount);

        if (difference > 0)
        {
            LOGGER.log(Level.FINEST, "CUSTOM: Pushing the total number of "
                + name + " from " + alreadyNumber + " to " + nowNumber
                + " (new available count is: " + newcount + ")");
        }
        else if (difference < 0)
        {
            LOGGER.log(Level.FINEST,
                "CUSTOM: WARNING: DIMINISHING the total number of " + name
                    + " from " + alreadyNumber + " to " + nowNumber
                    + " (new available count is: " + newcount + ")");
        }
    }

    private PlayerClientSide findPlayerWithStartingTower(String hexLabel)
    {
        Iterator<PlayerClientSide> it = allPlayerInfo.iterator();
        while (it.hasNext())
        {
            PlayerClientSide pi = it.next();
            String towerLabel = pi.getTower();

            if (towerLabel.equals(hexLabel))
            {
                return pi;
            }
        }
        return null;
    }

    @Override
    protected void resetInstance()
    {
        LOGGER.log(Level.FINEST, "CUSTOM: resetting " + getClass().getName());
        playerToOldScore.clear();
    }
}
