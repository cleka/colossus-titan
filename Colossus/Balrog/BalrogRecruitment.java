package Balrog;

import java.util.*;

import net.sf.colossus.client.PlayerInfo;
import net.sf.colossus.client.CaretakerInfo;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Caretaker;
import net.sf.colossus.util.Log;

/**
 * Custom class to allow recruitment of Balrog in 6-tower game.
 * @version $Id$
 * @author Romain Dolbeau
 */
public class BalrogRecruitment extends net.sf.colossus.server.CustomRecruitBase
{
    private Map nameToOldScore = Collections.synchronizedMap(new HashMap());
    private final static int balrogValue = 300;
    private final static String balrogPrefix = "Balrog";
    
    public List getAllPossibleSpecialRecruiters(String terrain)
    {
        // Balrog recruited in Tower, where everything recruit anyway.
        return new ArrayList();
    }
        
    public List getAllPossibleSpecialRecruits(String terrain)
    {
        List temp = new ArrayList();
        Iterator it = Creature.getCreatures().iterator();
        while (it.hasNext())
        {
            Creature cre = (Creature)it.next();
            if (cre.getName().startsWith(balrogPrefix))
            {
                temp.add(cre);
            }
        }
        return temp;
    }

    public List getPossibleSpecialRecruiters(String terrain, String hexLabel)
    {
        // Balrog recruited in Tower, where everything recruit anyway.
        return new ArrayList();
    }

    public List getPossibleSpecialRecruits(String terrain, String hexLabel)
    {
        List temp = new ArrayList();
        
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
            temp.add(Creature.getCreatureByName(name));
        }
        return temp;
    }
    
    public int numberOfRecruiterNeeded(String recruiter, String recruit, String terrain, String hexLabel)
    {
        return 0;
    }
    
    protected void changeOfTurn(int newActivePlayer)
    {
        Set towerSet = net.sf.colossus.client.MasterBoard.getTowerSet();
        
        // update all Balrogs, as a lost fight may have given points
        // to a different Player
        Iterator it = towerSet.iterator();
        while (it.hasNext())
        {
            updateBalrogCount((String)it.next());
        }
    }

    private synchronized void updateBalrogCount(String hexLabel)
    {
        String name = balrogPrefix + hexLabel;
        
        PlayerInfo pi = findPlayerWithStartingTower(hexLabel);
        
        if (pi == null)
        {
            Log.debug("CUSTOM: no player info for hex " + hexLabel);
            return;
        }
        
        int oldscore;
        int newscore;
        int alreadyNumber;
        int nowNumber;
        
        synchronized (nameToOldScore)
        {
            Integer score = (Integer)nameToOldScore.remove(pi.getName());
            
            if (score == null)
            {
                oldscore = 0;
            }
            else
            {
                oldscore = score.intValue();
            }
            newscore = pi.getScore();
            
            nameToOldScore.put(pi.getName(), new Integer(newscore));
            
            alreadyNumber = (oldscore / balrogValue);
            nowNumber = (newscore / balrogValue);
        }
        
        if (!Creature.isCreature(name))
        {
            Log.error("CUSTOM: Balrog by the name of " + name + " doesn't exist !");
            return;
        }
        
        Creature cre = Creature.getCreatureByName(name);
        ((CreatureBalrog)cre).setNewMaxCount(nowNumber);
        
        int difference = nowNumber - alreadyNumber;
        int newcount = getCount(name) + difference;
        
        setCount(name, newcount);
        
        if (difference > 0)
        {
            Log.debug("CUSTOM: Pushing the total number of " + name +
                      " from " + alreadyNumber + " to " + nowNumber +
                      " (new available count is: " + newcount +")");
        }
        else if (difference < 0)
        {
            Log.debug("CUSTOM: WARNING: DIMINISHING the total number of " +
                      name + " from " + alreadyNumber + " to " + nowNumber +
                      " (new available count is: " + newcount +")");
        }
    }
    
    private PlayerInfo findPlayerWithStartingTower(String hexLabel)
    {
        Iterator it = allPlayerInfo.iterator();
        while (it.hasNext())
        {
            PlayerInfo pi = (PlayerInfo)it.next();
            String towerLabel = pi.getTower();
            
            if (towerLabel.equals(hexLabel))
                return pi;
        }
        return null;
    }

    protected void resetInstance()
    {
        Log.debug("CUSTOM: resetting " + getClass().getName());
        nameToOldScore.clear();
    }
}
