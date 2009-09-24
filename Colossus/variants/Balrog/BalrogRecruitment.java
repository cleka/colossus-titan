package Balrog;


import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Player;
import net.sf.colossus.server.CustomRecruitBase;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;


/**
 * Custom class to allow recruitment of Balrog in 6-tower game.
 *
 * One becomes available in a Player home Tower for every 300 points earned
 * by the Player.
 *
 * @author Romain Dolbeau
 */
public class BalrogRecruitment extends CustomRecruitBase
{
    private static final Logger LOGGER = Logger
        .getLogger(BalrogRecruitment.class.getName());

    private final static int balrogValue = 300;
    private final static String balrogPrefix = "Balrog";

    @Override
    public List<CreatureType> getAllPossibleSpecialRecruiters(
        MasterBoardTerrain terrain)
    {
        // Balrog recruited in Tower, where everything recruit anyway.
        return new ArrayList<CreatureType>();
    }

    /** This one is called from an inner loop in the TerrainRecruitLoader,
     * BEFORE the current Variant becomes available through VariantSupport.
     * This means we can't enumerate all CreatureType here!!!!
     */
    @Override
    public List<CreatureType> getAllPossibleSpecialRecruits(
        MasterBoardTerrain terrain)
    {
        return CreatureBalrog.getAllBalrogs();
    }

    @Override
    public List<CreatureType> getPossibleSpecialRecruiters(
         MasterHex hex)
    {
        // Balrog recruited in Tower, where everything recruit anyway.
        return new ArrayList<CreatureType>();
    }

    @Override
    public List<CreatureType> getPossibleSpecialRecruits(
         MasterHex hex)
    {
        List<CreatureType> temp = new ArrayList<CreatureType>();

        if (hex == null)
        {
            return temp;
        }

        // need to update, as we might have earned points in the Engagement
        // phase and recruit in the Recruit phase
        updateBalrogCount3(hex);

        String name = balrogPrefix + hex.getLabel();
        List<CreatureType> allBalrogs = CreatureBalrog.getAllBalrogs();
        CreatureType balrogType = null;
        for (CreatureType bt : allBalrogs)
        {
            if (bt.getName().equals(name))
            {
                balrogType = bt;
            }
        }

        if (getCount(balrogType) > 0)
        {
            temp.add(balrogType);
        }
        return temp;
    }

    @Override
    public int numberOfRecruiterNeeded(CreatureType recruiter,
        CreatureType recruit, MasterHex hex)
    {
        LOGGER.finest("Called with recruiter " + recruiter + " and recruit " + recruit);
        if ((recruit == null) || (recruit instanceof CreatureBalrog))
            return 1;
        LOGGER.finest("Recruit " + recruit + " isn't a Balrog");
        return Constants.BIGNUM;
    }

    @Override
    protected synchronized void initCustomVariant()
    {
        changeOfTurn(0);
    }

    @Override
    protected void changeOfTurn(int newActivePlayer)
    {
        Set<MasterHex> towerSet = VariantSupport.getCurrentVariant()
            .getMasterBoard().getTowerSet();

        // update all Balrogs, as a lost fight may have given points
        // to a different Player
        for (MasterHex tower : towerSet)
        {
            updateBalrogCount3(tower);
        }
    }

    private void updateBalrogCount3(MasterHex tower)
    {
        for (int i = 0; i < 3; i++)
        {
          try
          {
                updateBalrogCount(tower);
                return;
          }
          catch (ConcurrentModificationException e)
          {
             // TODO Fix this properly
             LOGGER.info("ConcurrentModificationException while "
                + "doing updateBalrogCount() - ignoring it.");
             /*
              * This is just a workaround to prevent the game crashing/hanging.
              * To fix this properly would involve lot of changes at various
              * places for which I right now simply do not have the time.
              * Or do not want to spend time.
              *
              * See the bug report 2855208: "Balrog exception in V0.9.2"
              * for details.
              */
          }
        }
        return;
    }
    /** The magic function that add more Balrogs to the Caretaker when
     *  players score points goes up.
     */
    private void updateBalrogCount(MasterHex tower)
    {
        Player player = findPlayerWithStartingTower(tower);
        if (player == null)
        {
            LOGGER.finest("CUSTOM: no player info for hex " + tower);
            return;
        }

        if (player.isDead())
        {
            LOGGER.finest("INIT CUSTOM: player " + player.getName()
                + " is dead - doing nothing for hex " + tower);
            return;
        }

        String creatureName = balrogPrefix + tower.getLabel();
        if (!VariantSupport.getCurrentVariant().isCreature(creatureName))
        {
            LOGGER.severe("CUSTOM: Balrog by the name of " + creatureName
                + " doesn't exist !");
            return;
        }

        CreatureType type = VariantSupport.getCurrentVariant()
            .getCreatureByName(creatureName);

        int score = player.getScore();
        int maxCountShouldBe = (score / balrogValue);

        type.setMaxCount(maxCountShouldBe);
        adjustAvailableCount(type);
    }

    private Player findPlayerWithStartingTower(MasterHex tower)
    {
        //LOGGER.finest("Finding player for tower " + tower);
        for (Player player : allPlayers)
        {
            //LOGGER.finest("Finding tower for player " + player);
            MasterHex pst = player.getStartingTower();
            //LOGGER.finest("Found tower " + pst + " for player " + player);
            if ((pst != null) && (pst.equals(tower)))
            {
                return player;
            }
        }
        return null;
    }

    @Override
    protected void resetInstance()
    {
        LOGGER.finest("CUSTOM: resetting instance " + getClass().getName());
        resetAllBalrogCounts();
    }

    protected void resetAllBalrogCounts()
    {
        LOGGER.finest("CUSTOM: Resetting all Balrog counts");
        Set<MasterHex> towerSet = VariantSupport.getCurrentVariant()
            .getMasterBoard().getTowerSet();
        for (MasterHex tower : towerSet)
        {
            resetBalrogCount(tower);
        }
    }

    private void resetBalrogCount(MasterHex tower)
    {
        String name = balrogPrefix + tower.getLabel();
        CreatureBalrog cre = (CreatureBalrog)VariantSupport
            .getCurrentVariant().getCreatureByName(name);
        LOGGER.info("Setting count for " + name + " to 0.");
        cre.setMaxCount(0);
        setCount(cre, 0, true);
    }
}
