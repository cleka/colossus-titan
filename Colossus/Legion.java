import javax.swing.*;
import java.util.*;

/**
 * Class Legion represents a Titan stack of Creatures and its
 * stack marker.
 * @version $Id$
 * @author David Ripton
 */

public final class Legion implements Comparable
{
    private Marker marker;
    private String markerId;    // Bk03, Rd12, etc.
    private String parentId;
    private ArrayList critters = new ArrayList();
    private String currentHexLabel;
    private String startingHexLabel;
    private boolean moved;
    private String recruitName;
    private String playerName;
    private int battleTally;
    private Creature teleportingLord; // XXX Move into HexInfo?
    private static HashMap markerNames = new HashMap();
    private Game game;
    /** Maps hex labels to HexInfo classes. */
    private HashMap hexInfoMap = new HashMap();

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

    // XXX Need to eliminate the duplicated code between the constructors.

    public Legion(String markerId, String parentId, String currentHexLabel,
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

        // Initial legion contents are public; contents of legions created
        // by splits are private.
        if (getHeight() == 8)
        {
            revealAllCreatures();
        }
        else
        {
            // When loading a game, we handle revealing visible creatures
            // after legion creation.
            hideAllCreatures();
        }
    }


    // For AICopy()
    public Legion(String markerId, String parentId, String currentHexLabel,
        String startingHexLabel, ArrayList critters, String playerName,
        Game game)
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

        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Critter myCritter = critter.AICopy(game);
            this.critters.add(myCritter);
        }

        // Initial legion contents are public; contents of legions created
        // by splits are private.
        if (getHeight() == 8)
        {
            revealAllCreatures();
        }
        else
        {
            // When loading a game, we handle revealing visible creatures
            // after legion creation.
            hideAllCreatures();
        }
    }


    public static Legion getStartingLegion(String markerId, String hexLabel,
        String playerName, Game game)
    {
        return new Legion(markerId, null, hexLabel, hexLabel, Creature.titan,
            Creature.angel, Creature.ogre, Creature.ogre,
            Creature.centaur, Creature.centaur, Creature.gargoyle,
            Creature.gargoyle, playerName, game);
    }


    public static Legion getEmptyLegion(String markerId, String parentId,
        String hexLabel, String playerName, Game game)
    {
        return new Legion(markerId, parentId, hexLabel, hexLabel, null,
            null, null, null, null, null, null, null, playerName, game);
    }


    /** deep copy for AI */
    public Legion AICopy(Game game)
    {
        Legion newLegion = new Legion(markerId, parentId, currentHexLabel,
            startingHexLabel, critters, playerName, game);

        newLegion.moved = moved;
        newLegion.recruitName = recruitName;
        newLegion.battleTally = battleTally;
        newLegion.teleportingLord = teleportingLord;
        newLegion.marker = marker;

        // Deep copy hexInfoMap
        Iterator it = hexInfoMap.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry entry = (Map.Entry)it.next();
            String hexLabel = (String)entry.getKey();
            HexInfo hexInfo = (HexInfo)entry.getValue();
            newLegion.hexInfoMap.put(hexLabel, hexInfo.AICopy());
        }

        return newLegion;
    }

    // We could remove this method, and have the various dialogs
    // that use it take game as an argument.
    public Game getGame()
    {
        return game;
    }


    public int getPointValue()
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


    public void addPoints(int points)
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

        ArrayList recruits;

        while (getHeight() < 7 && tmpScore / 100 > (score - points) / 100)
        {
            recruits = game.findEligibleAngels(this, tmpScore / 500 >
                (score - points) / 500 && !didArchangel);
            String angelType = acquireAngel(recruits);
            tmpScore -= 100;
            if (angelType != null && recruits.contains(angelType))
            {
                Creature angel = Creature.getCreatureByName(angelType);
                if (angel != null)
                {
                    addCreature(angel, true);
                    Game.logEvent("Legion " + getLongMarkerName() +
                        " acquires an " + angelType);
                    if (angelType.equals("Archangel"))
                    {
                        didArchangel = true;
                    }
                }
            }
        }
    }


    /** recruits holds the types of angels can can acquire. */
    private String acquireAngel(ArrayList recruits)
    {
        Player player = getPlayer();
        String angelType = null;
        if (player.getOption(Options.autoAcquireAngels))
        {
            angelType = player.aiAcquireAngel(this, recruits, game);
        }
        else
        {
            // Make sure the board is visible.
            JFrame masterFrame = game.getMasterFrame();
            if (masterFrame.getState() == JFrame.ICONIFIED)
            {
                masterFrame.setState(JFrame.NORMAL);
            }
            angelType = AcquireAngel.acquireAngel(masterFrame,
                playerName, recruits);
        }
        return angelType;
    }


    public int getBattleTally()
    {
        return battleTally;
    }


    public void clearBattleTally()
    {
        battleTally = 0;
    }


    public void clearBattleInfo()
    {
        clearBattleTally();

        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            critter.heal();
            critter.addBattleInfo(null, null, null, null, -1);
        }
    }


    public void addToBattleTally(int points)
    {
        battleTally += points;
    }


    public void addBattleTallyToPoints()
    {
        addPoints(battleTally);
        clearBattleTally();
    }


    public String getMarkerId()
    {
        return markerId;
    }


    public String getMarkerName()
    {
        return (String)markerNames.get(markerId);
    }


    public String getLongMarkerName()
    {
        StringBuffer sb = new StringBuffer(markerId);
        sb.append(" (");
        sb.append(getMarkerName());
        sb.append(")");
        return sb.toString();
    }


    public String getParentId()
    {
        return parentId;
    }


    public Legion getParent()
    {
        return getPlayer().getLegionByMarkerId(parentId);
    }


    public String toString()
    {
        return markerId;
    }


    public String getImageName()
    {
        return markerId;
    }


    public Marker getMarker()
    {
        return marker;
    }


    public void setMarker(Marker marker)
    {
        this.marker = marker;
    }


    public boolean canFlee()
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


    public int numCreature(Creature creature)
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

    public int numLords()
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

    public int numRangestrikers()
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

    public boolean hasTitan()
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

    public boolean hasAngel()
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


    public int getHeight()
    {
        return critters.size();
    }

    public String getPlayerName()
    {
        return playerName;
    }

    public Player getPlayer()
    {
        return game.getPlayerByMarkerId(markerId);
    }

    public boolean hasMoved()
    {
        return moved;
    }


    public void setMoved(boolean moved)
    {
        this.moved = moved;
    }


    /** Eliminate this legion. */
    public void remove()
    {
        String hexLabel = currentHexLabel;
        prepareToRemove();

        Player player = getPlayer();
        // XXX The player can be null if the legion has not been fully
        // created.  Fix SplitLegion.
        if (player == null)
        {
            player = game.getActivePlayer();
        }
        player.getLegions().remove(this);

        game.getBoard().alignLegions(hexLabel);
    }


    /** Do the cleanup required before this legion can be removed. */
    public void prepareToRemove()
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
        Game.logEvent(log.toString());

        // Free up the legion marker.
        Player player = getPlayer();
        // XXX The player can be null if the legion has not been fully
        // created.  Fix SplitLegion.
        if (player == null)
        {
            player = game.getActivePlayer();
        }
        player.getMarkersAvailable().add(getMarkerId());
    }


    public void moveToHex(MasterHex hex)
    {
        Player player = getPlayer();
        String hexLabel = hex.getLabel();

        currentHexLabel = hexLabel;
        moved = true;
        player.setLastLegionMoved();

        boolean teleported = getTeleported(hexLabel);

        // If we teleported, no more teleports are allowed this turn.
        if (teleported)
        {
            player.setTeleported(true);
        }

        MasterBoard board = game.getBoard();
        board.alignLegions(currentHexLabel);
        board.alignLegions(startingHexLabel);

        Game.logEvent("Legion " + getLongMarkerName() + " in " +
            getCurrentHex().getDescription() + (teleported ?
            (game.isOccupied(hexLabel) ? " titan teleports " :
            " tower teleports (" + teleportingLord + ") " ) : " moves ") +
            "to " + hex.getDescription());
    }


    public void undoMove()
    {
        if (moved)
        {
            // If this legion teleported, allow teleporting again.
            if (getTeleported(currentHexLabel))
            {
                getPlayer().setTeleported(false);
            }

            String formerHexLabel = currentHexLabel;
            currentHexLabel = startingHexLabel;

            moved = false;
            Game.logEvent("Legion " + getLongMarkerName() +
                " undoes its move");

            MasterBoard board = game.getBoard();
            board.alignLegions(currentHexLabel);
            board.alignLegions(formerHexLabel);
        }
    }


    /** Called at end of player turn. */
    public void commitMove()
    {
        startingHexLabel = currentHexLabel;
        moved = false;
        recruitName = null;
    }


    public boolean hasRecruited()
    {
        return (recruitName != null);
    }

    public String getRecruitName()
    {
        return recruitName;
    }

    public void setRecruitName(String recruitName)
    {
        this.recruitName = recruitName;
    }


    /** hasMoved() is a separate check, so that this function can be used in
     *  battle as well as during the muster phase. */
    public boolean canRecruit()
    {
        if (recruitName != null || getHeight() > 6 || getPlayer().isDead() ||
            game.findEligibleRecruits(this).isEmpty())
        {
            return false;
        }
        return true;
    }


    public void undoRecruit()
    {
        if (recruitName != null)
        {
            Creature creature = Creature.getCreatureByName(recruitName);
            game.getCaretaker().putOneBack(creature);
            removeCreature(creature, false, true);
            recruitName = null;
            Game.logEvent("Legion " + getLongMarkerName() +
                " undoes its recruit");
        }
    }


    /** Return true if this legion can summon an angel or archangel. */
    public boolean canSummonAngel()
    {
        Player player = getPlayer();
        if (getHeight() >= 7 || player.hasSummoned())
        {
            return false;
        }

        Collection legions = player.getLegions();
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion candidate = (Legion)it.next();
            if (candidate != this &&
                (candidate.numCreature(Creature.angel) > 0 ||
                candidate.numCreature(Creature.archangel) > 0) &&
                !game.isEngagement(candidate.getCurrentHexLabel()))
            {
                return true;
            }
        }

        return false;
    }


    public String getCurrentHexLabel()
    {
        return currentHexLabel;
    }

    public MasterHex getCurrentHex()
    {
        MasterBoard board = game.getBoard();
        return board.getHexByLabel(currentHexLabel);
    }

    public String getStartingHexLabel()
    {
        return startingHexLabel;
    }

    public MasterHex getStartingHex()
    {
        MasterBoard board = game.getBoard();
        return board.getHexByLabel(startingHexLabel);
    }


    // Track entry side and teleport information for a MasterHex.
    final class HexInfo
    {
        String hexLabel;
        TreeSet entrySides = new TreeSet();  // Holds Integers.
        boolean teleported;

        HexInfo AICopy()
        {
            HexInfo newHexInfo = new HexInfo();
            // Strings and Integers are immutable.
            newHexInfo.hexLabel = hexLabel;
            newHexInfo.entrySides = (TreeSet)entrySides.clone();
            newHexInfo.teleported = teleported;
            return newHexInfo;
        }

        void setEntrySide(int side)
        {
            if (side == 1 || side == 3 || side ==5)
            {
                entrySides.add(new Integer(side));
            }
        }

        int getNumEntrySides()
        {
            return entrySides.size();
        }

        boolean canEnterViaSide(int side)
        {
            return entrySides.contains(new Integer(side));
        }

        boolean canEnterViaLand()
        {
            return !entrySides.isEmpty();
        }

        /** Return the entry side.  If there are none or more than one,
         *  return -1. */
        int getEntrySide()
        {
            if (entrySides.size() != 1)
            {
                return -1;
            }
            else
            {
                return ((Integer)entrySides.first()).intValue();
            }
        }

        void clearAllEntrySides()
        {
            entrySides.clear();
        }

        boolean getTeleported()
        {
            return teleported;
        }

        void setTeleported(boolean teleported)
        {
            this.teleported = teleported;
        }
    }

    /** If the indicated hex has exactly one entry side, return it.
     *  Otherwise return -1. */
    public int getEntrySide(String hexLabel)
    {
        HexInfo hexInfo = (HexInfo)hexInfoMap.get(hexLabel);
        if (hexInfo == null)
        {
            return -1;
        }
        return hexInfo.getEntrySide();
    }

    public void setEntrySide(String hexLabel, int entrySide)
    {
        HexInfo hexInfo = (HexInfo)hexInfoMap.get(hexLabel);
        if (hexInfo == null)
        {
            hexInfo = new HexInfo();
        }
        hexInfo.setEntrySide(entrySide);
        hexInfoMap.put(hexLabel, hexInfo);
    }

    public int getNumEntrySides(String hexLabel)
    {
        HexInfo hexInfo = (HexInfo)hexInfoMap.get(hexLabel);
        if (hexInfo == null)
        {
            return 0;
        }
        return hexInfo.getNumEntrySides();
    }

    public boolean canEnterViaSide(String hexLabel, int side)
    {
        HexInfo hexInfo = (HexInfo)hexInfoMap.get(hexLabel);
        if (hexInfo == null)
        {
            return false;
        }
        return hexInfo.canEnterViaSide(side);
    }

    public boolean canEnterViaLand(String hexLabel)
    {
        HexInfo hexInfo = (HexInfo)hexInfoMap.get(hexLabel);
        if (hexInfo == null)
        {
            return false;
        }
        return hexInfo.canEnterViaLand();
    }

    public void clearAllEntrySides(String hexLabel)
    {
        HexInfo hexInfo = (HexInfo)hexInfoMap.get(hexLabel);
        if (hexInfo != null)
        {
            hexInfo.clearAllEntrySides();
        }
    }

    public void clearAllHexInfo()
    {
        hexInfoMap.clear();
    }


    public boolean getTeleported(String hexLabel)
    {
        HexInfo hexInfo = (HexInfo)hexInfoMap.get(hexLabel);
        if (hexInfo == null)
        {
            return false;
        }
        return hexInfo.getTeleported();
    }

    public void setTeleported(String hexLabel, boolean teleported)
    {
        HexInfo hexInfo = (HexInfo)hexInfoMap.get(hexLabel);
        if (hexInfo == null)
        {
            hexInfo = new HexInfo();
        }
        hexInfo.setTeleported(teleported);
        hexInfoMap.put(hexLabel, hexInfo);
    }

    /** convenience method for AI */
    public void addCreature(Creature creature)
    {
        addCreature(creature,false);
    }

    /** Add a creature to this legion.  If takeFromStack is true,
        then do this only if such a creature remains in the stacks,
        and decrement the number of this creature type remaining. */
    public void addCreature(Creature creature, boolean takeFromStack)
    {
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
    public Creature removeCreature(int i, boolean returnImmortalToStack,
        boolean disbandIfEmpty)
    {
        Critter critter = (Critter)critters.remove(i);

        // If the creature is a lord or demi-lord, put it back in the stacks.
        if (returnImmortalToStack && critter.isImmortal())
        {
            game.getCaretaker().putOneBack(critter);
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
    public Creature removeCreature(Creature creature, boolean
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
    public Creature prepareToRemoveCritter(Critter critter, boolean
        returnImmortalToStack)
    {
        if (critter == null || !critters.contains(critter))
        {
            return null;
        }
        // If the creature is a lord or demi-lord, put it back in the stacks.
        if (returnImmortalToStack && critter.isImmortal())
        {
            game.getCaretaker().putOneBack(critter);
        }
        return critter;
    }


    public ArrayList getCritters()
    {
        return critters;
    }


    public Creature getCreature(int i)
    {
        Critter critter = getCritter(i);
        return critter.getCreature();
    }


    public Critter getCritter(int i)
    {
        return (Critter)critters.get(i);
    }


    public void setCritter(int i, Critter critter)
    {
        critters.set(i, critter);
        critter.setMarkerId(markerId);
    }

    /** Return the first critter with a matching tag. */
    public Critter getCritterByTag(int tag)
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
    public boolean moveToTop(Critter critter)
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
    public Critter getCritter(Creature creature)
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
    public void sortCritters()
    {
        Collections.sort(critters);
    }


    /** Recombine this legion into another legion. Only remove this
        legion from the Player if remove is true.  If it's false, the
        caller is responsible for removing this legion, which can avoid
        concurrent access problems. The caller needs to call
        MasterBoard.alignLegions() on the remaining legion's hexLabel
        after the recombined legion is actually removed. */
    public void recombine(Legion legion, boolean remove)
    {
        // Sanity check
        if (legion == this)
        {
            Game.logWarn("Tried to recombine a legion with itself!");
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

        Game.logEvent("Legion " + getLongMarkerName() +
            " recombined into legion " + legion.getLongMarkerName());

        sortCritters();
    }


    /**
     * Split off creatures into a new legion using legion marker markerId.
     * (Or the first available marker, if markerId is null.)
     * Return the new legion, or null if there's an error.
     */
    public Legion split(List creatures, String newMarkerId)
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
        if (!player.isMarkerAvailable(newMarkerId))
        {
            return null;
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

        Marker newMarker = new Marker(marker.getBounds().width,
            newMarkerId, game.getMasterFrame(), game);
        newLegion.setMarker(newMarker);

        player.addLegion(newLegion);
        player.setLastLegionSplitOff(newLegion);

        game.updateStatusScreen();
        Game.logEvent(newLegion.getHeight() +
            " creatures are split off from legion " + getLongMarkerName() +
            " into new legion " + newLegion.getLongMarkerName());

        sortCritters();
        newLegion.sortCritters();

        return newLegion;
    }

    public Legion split(List creatures)
    {
        return split(creatures, null);
    }


    public void hideAllCreatures()
    {
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            critter.setVisible(false);
        }
    }


    public void revealAllCreatures()
    {
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            critter.setVisible(true);
        }
    }

    public void revealCreatures(Creature creature, int numberToReveal)
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

    /** Reveal the lord who tower teleported the legion.  Pick one if
     *  necessary. */
    public void revealTeleportingLord(JFrame parentFrame, boolean
        autoPick)
    {
        teleportingLord = null;
        TreeSet lords = new TreeSet();

        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.isLord())
            {
                // Add Creatures rather than Critters to combine angels.
                lords.add(critter.getCreature());
            }
        }

        int lordTypes = lords.size();

        if (lordTypes == 1)
        {
            teleportingLord = (Creature)lords.first();
        }
        else
        {
            if (autoPick)
            {
                teleportingLord = (Creature)lords.first();
            }
            else
            {
                teleportingLord = PickLord.pickLord(parentFrame, this);
            }
        }

        if (teleportingLord != null)
        {
            revealCreatures(teleportingLord, 1);
        }
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
