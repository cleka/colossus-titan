package net.sf.colossus.server;


import javax.swing.*;
import java.util.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.client.MasterBoard;
import net.sf.colossus.client.MasterHex;
import net.sf.colossus.client.BattleMap;


/**
 * Class Legion represents a Titan stack of Creatures and its
 * stack marker.
 * @version $Id$
 * @author David Ripton
 */

final class Legion implements Comparable
{
    private String markerId;    // Bk03, Rd12, etc.
    private String parentId;
    private List critters = new ArrayList();
    private String currentHexLabel;
    private String startingHexLabel;
    private boolean moved;
    private int entrySide = -1;
    private boolean teleported;
    private String recruitName;
    private String playerName;
    private int battleTally;
    private static Map markerNames = new HashMap();
    private Game game;
    private int angelsToAcquire;
    private boolean canAcquireArchangel;

    static
    {
        markerNames.put("Bk01", "Axes");
        markerNames.put("Bk02", "Eye");
        markerNames.put("Bk03", "Die");
        markerNames.put("Bk04", "Feather");
        markerNames.put("Bk05", "Hand");
        markerNames.put("Bk06", "Lightning");
        markerNames.put("Bk07", "Pumpkin");
        markerNames.put("Bk08", "Rose");
        markerNames.put("Bk09", "Scorpion");
        markerNames.put("Bk10", "Skull");
        markerNames.put("Bk11", "Spearhead");
        markerNames.put("Bk12", "Tombstone");

        markerNames.put("Br01", "Antlers");
        markerNames.put("Br02", "Bell");
        markerNames.put("Br03", "Chest");
        markerNames.put("Br04", "Figurehead");
        markerNames.put("Br05", "Hook");
        markerNames.put("Br06", "Hourglass");
        markerNames.put("Br07", "Paw");
        markerNames.put("Br08", "Ram");
        markerNames.put("Br09", "Scroll");
        markerNames.put("Br10", "Spider");
        markerNames.put("Br11", "Tankard");
        markerNames.put("Br12", "Wheel");

        markerNames.put("Bu01", "Anchor");
        markerNames.put("Bu02", "Bat");
        markerNames.put("Bu03", "Candle");
        markerNames.put("Bu04", "Cloud");
        markerNames.put("Bu05", "Egg");
        markerNames.put("Bu06", "Foot");
        markerNames.put("Bu07", "Fountain");
        markerNames.put("Bu08", "Moon");
        markerNames.put("Bu09", "Octopus");
        markerNames.put("Bu10", "Padlock");
        markerNames.put("Bu11", "Tornado");
        markerNames.put("Bu12", "Trident");

        markerNames.put("Gd01", "Caduceus");
        markerNames.put("Gd02", "Claw");
        markerNames.put("Gd03", "Coins");
        markerNames.put("Gd04", "Crown");
        markerNames.put("Gd05", "Horn");
        markerNames.put("Gd06", "Lamp");
        markerNames.put("Gd07", "Pyramid");
        markerNames.put("Gd08", "Rings");
        markerNames.put("Gd09", "Scarab");
        markerNames.put("Gd10", "Scimitars");
        markerNames.put("Gd11", "Sun");
        markerNames.put("Gd12", "Wheat");

        markerNames.put("Gr01", "Cauldron");
        markerNames.put("Gr02", "Dagger");
        markerNames.put("Gr03", "Diamond");
        markerNames.put("Gr04", "Fish");
        markerNames.put("Gr05", "Fleur");
        markerNames.put("Gr06", "Frog");
        markerNames.put("Gr07", "Grapple");
        markerNames.put("Gr08", "Harp");
        markerNames.put("Gr09", "Lobster");
        markerNames.put("Gr10", "Olive");
        markerNames.put("Gr11", "Scales");
        markerNames.put("Gr12", "Snake");

        markerNames.put("Rd01", "Cross");
        markerNames.put("Rd02", "Eagle");
        markerNames.put("Rd03", "Fist");
        markerNames.put("Rd04", "Gong");
        markerNames.put("Rd05", "Heart");
        markerNames.put("Rd06", "Jester");
        markerNames.put("Rd07", "Salamander");
        markerNames.put("Rd08", "Shield");
        markerNames.put("Rd09", "Spiral");
        markerNames.put("Rd10", "Star");
        markerNames.put("Rd11", "Sword");
        markerNames.put("Rd12", "Torch");
    }

    /**
     * TMJF sez: these are but I think they are only for
     * testing purposes
     */

    Legion(String markerId, String parentId, String currentHexLabel,
        String startingHexLabel, Creature creature0, Creature creature1,
        Creature creature2, Creature creature3, Creature creature4,
        Creature creature5, Creature creature6, Creature creature7,
        String playerName, Game game)
    {
        this.markerId = markerId;
        this.parentId = parentId;
        // Sanity check
        if (parentId != null && parentId.equals(markerId))
        {
            parentId = null;
        }
        this.currentHexLabel = currentHexLabel;
        this.startingHexLabel = startingHexLabel;
        this.playerName = playerName;
        this.game = game;

        if (creature0 != null)
        {
            critters.add(new Critter(creature0, false, markerId, game));
        }
        if (creature1 != null)
        {
            critters.add(new Critter(creature1, false, markerId, game));
        }
        if (creature2 != null)
        {
            critters.add(new Critter(creature2, false, markerId, game));
        }
        if (creature3 != null)
        {
            critters.add(new Critter(creature3, false, markerId, game));
        }
        if (creature4 != null)
        {
            critters.add(new Critter(creature4, false, markerId, game));
        }
        if (creature5 != null)
        {
            critters.add(new Critter(creature5, false, markerId, game));
        }
        if (creature6 != null)
        {
            critters.add(new Critter(creature6, false, markerId, game));
        }
        if (creature7 != null)
        {
            critters.add(new Critter(creature7, false, markerId, game));
        }

        initCreatureVisibility();
    }


    //* For AICopy() */
    Legion(String markerId, String parentId, String currentHexLabel,
        String startingHexLabel, List critters, String playerName,
        Game game)
    {
        this(markerId, parentId, currentHexLabel, startingHexLabel, null,
            null, null, null, null, null, null, null, playerName, game);

        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Critter myCritter = critter.AICopy(game);
            this.critters.add(myCritter);
        }

        initCreatureVisibility();
    }

    private void initCreatureVisibility()
    {
        // Initial legion contents are public; contents of legions created
        // by splits are private.
        // When loading a game, we handle revealing visible creatures
        // after legion creation.
        if (getHeight() == 8)
        {
            revealAllCreatures();
        }
    }

    static Legion getStartingLegion(String markerId, String hexLabel,
        String playerName, Game game)
    {
        Creature[] startCre = game.getStartingCreatures();
        Legion oLegion = 
            new Legion(markerId, null, hexLabel, hexLabel,
                       Creature.getCreatureByName("Titan"),
                       Creature.getCreatureByName("Angel"),
                       startCre[2],
                       startCre[2],
                       startCre[0],
                       startCre[0],
                       startCre[1],
                       startCre[1],
                       playerName, game);
        return oLegion;
    }


    static Legion getEmptyLegion(String markerId, String parentId,
        String hexLabel, String playerName, Game game)
    {
        Legion oLegion =
            new Legion(markerId, parentId, hexLabel, hexLabel, null,
                       null, null, null, null, null, null, null, 
                       playerName, game);
        return oLegion;
    }


    /** deep copy for AI */
    Legion AICopy(Game game)
    {
        Legion newLegion = new Legion(markerId, parentId, currentHexLabel,
            startingHexLabel, critters, playerName, game);

        newLegion.moved = moved;
        newLegion.recruitName = recruitName;
        newLegion.battleTally = battleTally;
        newLegion.entrySide = entrySide;
        newLegion.teleported = teleported;

        return newLegion;
    }

    // We could remove this method, and have the various dialogs
    // that use it take game as an argument.
    Game getGame()
    {
        return game;
    }


    int getPointValue()
    {
        int pointValue = 0;
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            pointValue += critter.getPointValue();
        }
        return pointValue;
    }


    void addPoints(int points)
    {
        if (game == null)
        {
            return;
        }
        Player player = getPlayer();
        player.addPoints(points);
        int score = player.getScore();
        int tmpScore = score;

        // It's practically impossible to get more than one archangel
        // from a single battle.
        boolean didArchangel = false;

        List recruits;

        while (getHeight() < 7 && tmpScore / 100 > (score - points) / 100)
        {
            angelsToAcquire++;
            boolean archangel = !didArchangel && (tmpScore / 500 >
                (score - points) / 500);
            if (archangel)
            {
                canAcquireArchangel = true;
                didArchangel = true;
            }
            recruits = game.findEligibleAngels(this, archangel);
            if (!recruits.isEmpty())
            {
                game.askAcquireAngel(getPlayerName(), getMarkerId(), recruits);
            }
            tmpScore -= 100;
        }
    }

    void addAngel(String angelType)
    {
        if (angelsToAcquire <= 0) 
        {
            return;
        }

        if (angelType == null)
        {
            // Declined to acquire.
            angelsToAcquire--;
        }
        else if (angelType.equals("Archangel") && !canAcquireArchangel)
        {
            return;
        }
        else
        {
            Creature angel = Creature.getCreatureByName(angelType);
            if (angel != null)
            {
                addCreature(angel, true);
                Log.event("Legion " + getLongMarkerName() + " acquires an " + 
                    angelType);
                angelsToAcquire--;
                if (angelType.equals("Archangel"))
                {
                    canAcquireArchangel = false;
                }
            }
        }

        if (angelsToAcquire == 0)
        {
            game.doneAcquiringAngels();
        }
    }


    int getBattleTally()
    {
        return battleTally;
    }


    void clearBattleTally()
    {
        battleTally = 0;
    }


    void clearBattleInfo()
    {
        clearBattleTally();

        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            critter.heal();
            critter.addBattleInfo(null, null, null);
        }
    }


    void addToBattleTally(int points)
    {
        battleTally += points;
    }


    void addBattleTallyToPoints()
    {
        addPoints(battleTally);
        clearBattleTally();
    }


    String getMarkerId()
    {
        return markerId;
    }


    String getMarkerName()
    {
        return (String)markerNames.get(markerId);
    }


    String getLongMarkerName()
    {
        StringBuffer sb = new StringBuffer(markerId);
        sb.append(" (");
        sb.append(getMarkerName());
        sb.append(")");
        return sb.toString();
    }


    String getParentId()
    {
        return parentId;
    }


    Legion getParent()
    {
        return getPlayer().getLegionByMarkerId(parentId);
    }


    public String toString()
    {
        return markerId;
    }


    /** Return a list of imageNames for all critters in this legion.
     *  Unless showAll is true, return "Unknown" for critters that
     *  are not visible.  Put all the Unknowns at the end of the
     *  list to avoid giving out too much information. */
    List getImageNames(boolean showAll)
    {
        sortCritters();
        List imageNames = new ArrayList();
        Iterator it = getCritters().iterator();
        int unknowns = 0;
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (showAll || critter.isVisible())
            {
                imageNames.add(critter.getImageName(false));
            }
            else
            {
                unknowns++;
            }
        }
        for (int i = 0; i < unknowns; i++)
        {
            imageNames.add("Unknown");
        }
        return imageNames;
    }


    /** Return a list of unique imageNames for all lords in this legion. */
    List getUniqueLordImageNames()
    {
        sortCritters();
        List imageNames = new ArrayList();
        Iterator it = getCritters().iterator();
        int unknowns = 0;
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.isLord()) 
            {
                String imageName = critter.getImageName(true);
                if (!imageNames.contains(imageName))
                {
                    imageNames.add(critter.getImageName(false));
                }
            }
        }
        return imageNames;
    }


    String getImageName()
    {
        return markerId;
    }


    boolean canFlee()
    {
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.isLord())
            {
                return false;
            }
        }
        return true;
    }


    int numCreature(Creature creature)
    {
        int count = 0;
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.getName().equals(creature.getName()))
            {
                count++;
            }
        }
        return count;
    }

    int numLords()
    {
        int count = 0;
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.isLord())
            {
                count++;
            }
        }
        return count;
    }

    int numRangestrikers()
    {
        int count = 0;
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.isRangestriker())
            {
                count++;
            }
        }
        return count;
    }

    boolean hasTitan()
    {
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.isTitan())
            {
                return true;
            }
        }
        return false;
    }

    boolean hasAngel()
    {
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.isAngel())
            {
                return true;
            }
        }
        return false;
    }


    int getHeight()
    {
        return critters.size();
    }

    String getPlayerName()
    {
        return playerName;
    }

    Player getPlayer()
    {
        return game.getPlayerByMarkerId(markerId);
    }

    boolean hasMoved()
    {
        return moved;
    }


    void setMoved(boolean moved)
    {
        this.moved = moved;
    }


    /** Eliminate this legion. */
    void remove()
    {
        String hexLabel = currentHexLabel;
        prepareToRemove();

        Player player = getPlayer();
        player.getLegions().remove(this);

        game.getServer().allRemoveMarker(markerId);
        game.getServer().allAlignLegions(hexLabel);
    }


    /** Do the cleanup required before this legion can be removed. */
    void prepareToRemove()
    {
        StringBuffer log = new StringBuffer("Legion ");
        log.append(getLongMarkerName());
        log.append(" ");
        if (getHeight() > 0)
        {
            log.append("[");
            // Return lords and demi-lords to the stacks.
            Iterator it = critters.iterator();
            while (it.hasNext())
            {
                Critter critter = (Critter)it.next();
                log.append(critter.getName());
                if (it.hasNext())
                {
                    log.append(", ");
                }
                if (critter.isImmortal())
                {
                    game.getCaretaker().putOneBack(critter);
                }
            }
            log.append("] ");
        }
        log.append("is eliminated");
        Log.event(log.toString());

        // Free up the legion marker.
        game.getServer().allRemoveMarker(markerId);
        Player player = getPlayer();
        player.addLegionMarker(getMarkerId());
    }


    void moveToHex(MasterHex hex, String entrySide, boolean teleported,
        String teleportingLord)
    {
        Player player = getPlayer();
        String hexLabel = hex.getLabel();

        currentHexLabel = hexLabel;
        moved = true;

        setEntrySide(entrySide);

        // If we teleported, no more teleports are allowed this turn.
        if (teleported)
        {
            setTeleported(true);
            player.setTeleported(true);
        }

        Set set = new HashSet();
        set.add(currentHexLabel);
        set.add(startingHexLabel);
        game.getServer().allAlignLegions(set);

        Log.event("Legion " + getLongMarkerName() + " in " +
            getStartingHexLabel() + (teleported ?
            (game.getNumEnemyLegions(hexLabel, game.getPlayer(playerName)) > 0
            ? " titan teleports "
            : " tower teleports (" + teleportingLord + ") " ) : " moves ") +
            "to " + hex.getDescription() + " entering on " + entrySide);
    }


    boolean hasConventionalMove()
    {
        return !game.listNormalMoves(this, getCurrentHex(),
            getPlayer().getMovementRoll(), false).isEmpty();
    }


    void undoMove()
    {
        if (moved)
        {
            // If this legion teleported, allow teleporting again.
            if (getTeleported())
            {
                getPlayer().setTeleported(false);
            }

            String formerHexLabel = currentHexLabel;
            currentHexLabel = startingHexLabel;

            moved = false;
            Log.event("Legion " + getLongMarkerName() + " undoes its move");

            Set set = new HashSet();
            set.add(currentHexLabel);
            set.add(formerHexLabel);
            game.getServer().allAlignLegions(set);
        }
    }


    /** Called at end of player turn. */
    void commitMove()
    {
        startingHexLabel = currentHexLabel;
        moved = false;
        recruitName = null;
    }


    boolean hasRecruited()
    {
        return (recruitName != null);
    }

    String getRecruitName()
    {
        return recruitName;
    }

    void setRecruitName(String recruitName)
    {
        this.recruitName = recruitName;
    }


    /** hasMoved() is a separate check, so that this function can be used in
     *  battle as well as during the muster phase. */
    boolean canRecruit()
    {
        return (recruitName == null && getHeight() <= 6 && 
            !getPlayer().isDead() &&
            !(game.findEligibleRecruits(
                getMarkerId(), getCurrentHexLabel()).isEmpty()));
    }


    void undoRecruit()
    {
        if (recruitName != null)
        {
            Creature creature = Creature.getCreatureByName(recruitName);
            game.getCaretaker().putOneBack(creature);
            removeCreature(creature, false, true);
            recruitName = null;
            Log.event("Legion " + getLongMarkerName() +
                " undoes its recruit");
        }
    }


    /** Return true if this legion can summon an angel or archangel. */
    boolean canSummonAngel()
    {
        Player player = getPlayer();
        if (getHeight() >= 7 || player.hasSummoned())
        {
            return false;
        }

        return angelAvailable() || archangelAvailable();
    }

    /** Return true if this legion can summon an angel (only). */
    boolean angelAvailable()
    {
        Player player = getPlayer();
        Collection legions = player.getLegions();
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion candidate = (Legion)it.next();
            if (candidate != this &&
                (candidate.numCreature(Creature.getCreatureByName("Angel")) 
                > 0) && !game.isEngagement(candidate.getCurrentHexLabel()))
            {
                return true;
            }
        }

        return false;
    }

    /** Return true if this legion can summon an archangel. */
    boolean archangelAvailable()
    {
        Player player = getPlayer();
        Collection legions = player.getLegions();
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion candidate = (Legion)it.next();
            if (candidate != this &&
                (candidate.numCreature(
                Creature.getCreatureByName("Archangel")) > 0) &&
                !game.isEngagement(candidate.getCurrentHexLabel()))
            {
                return true;
            }
        }

        return false;
    }


    String getCurrentHexLabel()
    {
        return currentHexLabel;
    }

    MasterHex getCurrentHex()
    {
        return MasterBoard.getHexByLabel(currentHexLabel);
    }

    String getStartingHexLabel()
    {
        return startingHexLabel;
    }

    MasterHex getStartingHex()
    {
        return MasterBoard.getHexByLabel(startingHexLabel);
    }


    void setEntrySide(int entrySide)
    {
        this.entrySide = entrySide;
    }

    void setEntrySide(String entrySide)
    {
        this.entrySide = BattleMap.entrySideNum(entrySide);
    }

    int getEntrySide()
    {
        return entrySide;
    }

    boolean getTeleported()
    {
        return teleported;
    }

    void setTeleported(boolean teleported)
    {
        this.teleported = teleported;
    }


    /** convenience method for AI */
    void addCreature(Creature creature)
    {
        addCreature(creature,false);
    }

    /** Add a creature to this legion.  If takeFromStack is true,
        then do this only if such a creature remains in the stacks,
        and decrement the number of this creature type remaining. */
    void addCreature(Creature creature, boolean takeFromStack)
    {
        if (getHeight() >= 7)
        {
            Log.error("Tried to add to 7-high legion!");
            return;
        }
        if (takeFromStack)
        {
            Caretaker caretaker = game.getCaretaker();
            if (caretaker.getCount(creature) > 0)
            {
                caretaker.takeOne(creature);
            }
            else
            {
                return;
            }
        }

        // Newly added critters are visible.
        critters.add(new Critter(creature, true, markerId, game));
    }

    /** Remove the creature in position i in the legion.  Return the
        removed creature. Put immortal creatures back on the stack
        if returnImmortalToStack is true. */
    Creature removeCreature(int i, boolean returnImmortalToStack,
        boolean disbandIfEmpty)
    {
        Critter critter = (Critter)critters.remove(i);

        // If the creature is a lord or demi-lord, put it back in the stacks.
        if (returnImmortalToStack)
        {
            if(critter.isImmortal())
            {
                game.getCaretaker().putOneBack(critter);
            }
        }

        // If there are no critters left, disband the legion.
        if (disbandIfEmpty && getHeight() == 0)
        {
            remove();
        }

        return critter;
    }


    /** Remove the first creature matching the passed creature's type
        from the legion.  Return the removed creature. */
    Creature removeCreature(Creature creature, boolean
        returnImmortalToStack, boolean disbandIfEmpty)
    {
        // indexOf wants the same object, not just the same type.
        // So use getCritter() to get the correct object.
        Critter critter = getCritter(creature);
        if (critter == null)
        {
            return null;
        }
        else
        {
            int i = critters.indexOf(critter);
            return removeCreature(i, returnImmortalToStack, disbandIfEmpty);
        }
    }


    /** Do the cleanup associated with removing the critter from this
     *  legion.  Do not actually remove it, to prevent comodification
     *  errors.  Do not disband the legion if empty, since the critter
     *  has not actually been removed. Return the critter if present. */
    Creature prepareToRemoveCritter(Critter critter, boolean
        returnImmortalToStack)
    {
        if (critter == null || !critters.contains(critter))
        {
            return null;
        }
        // If the creature is a lord or demi-lord, put it back in the stacks.
        if (returnImmortalToStack)
        {
            if(critter.isImmortal())
            {
                game.getCaretaker().putOneBack(critter);
            }
        }
        return critter;
    }


    List getCritters()
    {
        return critters;
    }


    Creature getCreature(int i)
    {
        Critter critter = getCritter(i);
        return critter.getCreature();
    }


    Critter getCritter(int i)
    {
        return (Critter)critters.get(i);
    }


    void setCritter(int i, Critter critter)
    {
        critters.set(i, critter);
        critter.setMarkerId(markerId);
    }

    /** Return the first critter with a matching tag. */
    Critter getCritterByTag(int tag)
    {
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (tag == critter.getTag())
            {
                return critter;
            }
        }
        return null;
    }

    /** Move critter to the first position in the critters list.
     *  Return true if it was moved. */
    boolean moveToTop(Critter critter)
    {
        int i = critters.indexOf(critter);
        if (i <= 0)
        {
            // Not found, or already first in the list.
            return false;
        }
        else
        {
            critters.remove(i);
            critters.add(0, critter);
            return true;
        }
    }


    /** Gets the first critter in this legion with the same creature
        type as the passed creature. */
    Critter getCritter(Creature creature)
    {
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.getName().equals(creature.getName()))
            {
                return critter;
            }
        }
        return null;
    }


    /** Sort critters into descending order of importance. */
    void sortCritters()
    {
        Collections.sort(critters);
    }


    /** Recombine this legion into another legion. Only remove this
        legion from the Player if remove is true.  If it's false, the
        caller is responsible for removing this legion, which can avoid
        concurrent access problems. The caller needs to call
        MasterBoard.alignLegions() on the remaining legion's hexLabel
        after the recombined legion is actually removed. */
    void recombine(Legion legion, boolean remove)
    {
        // Sanity check
        if (legion == this)
        {
            Log.warn("Tried to recombine a legion with itself!");
            return;
        }
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();

            legion.addCreature(critter, false);

            // Keep removeLegion() from returning lords to stacks.
            if (critter.isLord())
            {
                game.getCaretaker().takeOne(critter);
            }
        }
        if (remove)
        {
            remove();
        }
        else
        {
            prepareToRemove();
        }

        Log.event("Legion " + getLongMarkerName() +
            " recombined into legion " + legion.getLongMarkerName());

        sortCritters();
    }


    /**
     * Split off creatures into a new legion using legion marker markerId.
     * (Or the first available marker, if markerId is null.)
     * Return the new legion, or null if there's an error.
     */
    Legion split(List creatures, String newMarkerId)
    {
        Player player = getPlayer();
        if (newMarkerId == null)
        {
            newMarkerId = player.getFirstAvailableMarker();
            if (newMarkerId == null)
            {
                return null;
            }
        }

        player.selectMarkerId(newMarkerId);
        Legion newLegion = Legion.getEmptyLegion(newMarkerId, markerId,
            currentHexLabel, playerName, game);

        Iterator it = creatures.iterator();
        while (it.hasNext())
        {
            Creature creature = (Creature)it.next();
            creature = removeCreature(creature, false, false);
            if (creature == null)
            {
                // Abort the split.
                newLegion.recombine(this, true);
                return null;
            }
            newLegion.addCreature(creature);
        }

        game.getServer().allAddMarker(newMarkerId);

        player.addLegion(newLegion);

        game.getServer().allUpdateStatusScreen();
        Log.event(newLegion.getHeight() +
            " creatures are split off from legion " + getLongMarkerName() +
            " into new legion " + newLegion.getLongMarkerName());

        sortCritters();
        newLegion.sortCritters();

        return newLegion;
    }

    Legion split(List creatures)
    {
        return split(creatures, null);
    }


    void hideAllCreatures()
    {
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            critter.setVisible(false);
        }
    }


    void revealAllCreatures()
    {
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            critter.setVisible(true);
        }
    }

    void revealCreatures(Creature creature, int numberToReveal)
    {
        int numberAlreadyRevealed = 0;

        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.getCreature().getName().equals(
                creature.getName()) && critter.isVisible())
            {
                numberAlreadyRevealed++;
            }
        }

        int excess = numberAlreadyRevealed + numberToReveal -
            numCreature(creature);
        if (excess > 0)
        {
            numberToReveal -= excess;
        }

        it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.getCreature().getName().equals(
                creature.getName()) && !critter.isVisible())
            {
                critter.setVisible(true);
                numberToReveal--;
                if (numberToReveal == 0)
                {
                    return;
                }
            }
        }
    }

    /** List the lords eligible to teleport this legion to hexLabel,
     *  as strings. */
    List listTeleportingLords(String hexLabel)
    {
        // Needs to be a List not a Set so that it can be passed as
        // an imageList.
        List lords = new ArrayList();

        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.isLord())
            {
                String name = critter.getName();
                if (!lords.contains(name))
                {
                    lords.add(name);
                }
            }
        }

        return lords;
    }


    private String[] convertCritterListToStringArray(List oCritterList)
    {
        int nSize = oCritterList.size();
        String[] strArray = new String[nSize];
        int i = 0;
        Iterator it = oCritterList.iterator();
        while(it.hasNext())
        {
            Critter oCritter = (Critter) it.next();
            String strName = oCritter.getName();
            strArray[i++] = strName;
        }

        return strArray;
    }

    private static Creature getCreatureByName(String strName)
    {
        return strName != null ? Creature.getCreatureByName(strName) : null;
    }


    /** Legions are sorted in descending order of total point value,
        with the titan legion always coming first.  This is inconsistent
        with equals(). */
    public int compareTo(Object object)
    {
        if (object instanceof Legion)
        {
            Legion other = (Legion)object;
            if (hasTitan())
            {
                return Integer.MIN_VALUE;
            }
            else if (other.hasTitan())
            {
                return Integer.MAX_VALUE;
            }
            else
            {
                return (other.getPointValue() - this.getPointValue());
            }
        }
        else
        {
            throw new ClassCastException();
        }
    }
}
