import java.util.*;
import java.io.*;

/**
 * Class Player holds the data for one player in a Titan game.
 * @version $Id$
 * @author David Ripton
 */

public final class Player implements Comparable
{
    private Game game;
    private String name;
    private String color;              // Black, Blue, Brown, Gold, Green, Red
    private int startingTower;         // 1-6
    private double score;              // track half-points, then round
    private boolean summoned;
    private boolean teleported;
    private String playersEliminated;  // RdBkGr
    private int mulligansLeft = 1;
    private int movementRoll;          // 0 if movement has not been rolled.
    private ArrayList legions = new ArrayList();
    private boolean dead;
    private boolean titanEliminated;
    private Legion donor;
    private Legion mover;
    private Legion lastLegionSummonedFrom;
    private MarkerComparator markerComparator = new MarkerComparator();
    private TreeSet markersAvailable = new TreeSet(markerComparator);

    /** Stack of legions, to allow multiple levels of undo for splits,
     *  moves, and recruits. */
    private LinkedList undoStack = new LinkedList();
    private Properties options = new Properties();
    private AI ai = new SimpleAI();  // TODO Allow pluggable AIs.


    public Player(String name, Game game)
    {
        this.name = name;
        this.game = game;
    }

    /**
     *  Deep copy for AI; preserves game state but ignores UI state
     */
    public Player AICopy(Game game)
    {
        Player newPlayer = new Player(name, game);
        newPlayer.color = color;       // Black, Blue, Brown, Gold, Green, Red
        newPlayer.startingTower = startingTower;         // 1-6
        newPlayer.score = score;       // track half-points, then round
        newPlayer.summoned = summoned;
        newPlayer.teleported = teleported;
        newPlayer.playersEliminated = playersEliminated;  // RdBkGr
        newPlayer.mulligansLeft = mulligansLeft;
        newPlayer.movementRoll = movementRoll;
        newPlayer.dead = dead;
        newPlayer.ai = ai;
        newPlayer.titanEliminated = titanEliminated;
        for (int i = 0; i < legions.size(); i++)
        {
            newPlayer.legions.add(i, ((Legion)legions.get(i)).AICopy(game));
        }
        // Strings are immutable, so a shallow copy == a deep copy
        newPlayer.markersAvailable = (TreeSet) markersAvailable.clone();
        return newPlayer;
    }


    public boolean isDead()
    {
        return dead;
    }

    public void setDead(boolean dead)
    {
        this.dead = dead;
    }


    public String getColor()
    {
        return color;
    }

    public void setColor(String color)
    {
        this.color = color;
    }


    public void initMarkersAvailable()
    {
        for (int i = 1; i <= 9; i++)
        {
            addLegionMarker(getShortColor() + '0' + Integer.toString(i));
        }
        for (int i = 10; i <= 12; i++)
        {
            addLegionMarker(getShortColor() + Integer.toString(i));
        }
    }


    public String getShortColor()
    {
        if (color == null)
        {
            return null;
        }
        else if (color.equals("Black"))
        {
            return "Bk";
        }
        else if (color.equals("Blue"))
        {
            return "Bu";
        }
        else if (color.equals("Brown"))
        {
            return "Br";
        }
        else if (color.equals("Gold"))
        {
            return "Gd";
        }
        else if (color.equals("Green"))
        {
            return "Gr";
        }
        else if (color.equals("Red"))
        {
            return "Rd";
        }
        else
        {
            return "XX";
        }
    }


    public void setTower(int startingTower)
    {
        this.startingTower = startingTower;
    }

    public int getTower()
    {
        return startingTower;
    }


    /** Players are sorted in order of decreasing starting tower.
        This is inconsistent with equals(). */
    public int compareTo(Object object)
    {
        if (object instanceof Player)
        {
            Player other = (Player)object;
            return (other.getTower() - this.getTower());
        }
        else
        {
            throw new ClassCastException();
        }
    }


    public int getScore()
    {
        return (int) score;
    }


    public void setScore(int score)
    {
        this.score = score;
    }


    public String getPlayersElim()
    {
        return playersEliminated;
    }


    public void setPlayersElim(String playersEliminated)
    {
        this.playersEliminated = playersEliminated;
    }


    public void addPlayerElim(Player player)
    {
        if (playersEliminated == null)
        {
            playersEliminated = player.getShortColor();
        }
        else
        {
            playersEliminated = playersEliminated + player.getShortColor();
        }
    }


    public boolean canTitanTeleport()
    {
        return (score >= 400 && !teleported);
    }


    public boolean hasSummoned()
    {
        return summoned;
    }


    public void setSummoned(boolean summoned)
    {
        this.summoned = summoned;
    }


    public boolean hasTeleported()
    {
        return teleported;
    }


    public void setTeleported(boolean teleported)
    {
        this.teleported = teleported;
    }


    public Legion getLastLegionSummonedFrom()
    {
        return lastLegionSummonedFrom;
    }


    public void disbandEmptyDonor()
    {
        if (lastLegionSummonedFrom != null &&
            lastLegionSummonedFrom.getHeight() == 0)
        {
            lastLegionSummonedFrom.remove();
            lastLegionSummonedFrom = null;
        }
    }


    public void setLastLegionSummonedFrom(Legion legion)
    {
        lastLegionSummonedFrom = legion;
    }


    public int getTitanPower()
    {
        return (int) (6 + (getScore() / 100));
    }


    public int getNumLegions()
    {
        return legions.size();
    }


    public Legion getLegion(int i)
    {
        return (Legion)legions.get(i);
    }


    public Legion getLegionByMarkerId(String markerId)
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (legion.getMarkerId().equals(markerId))
            {
                return legion;
            }
        }
        return null;
    }


    public ArrayList getLegions()
    {
        return legions;
    }


    /** Sort legions into order of descending importance.  Titan legion
     *  first, then others by point value. */
    public void sortLegions()
    {
        Collections.sort(legions);
    }


    public MarkerComparator getMarkerComparator()
    {
        return markerComparator;
    }


    /** Move legion to the first position in the legions list.
     *  Return true if it was moved. */
    public boolean moveToTop(Legion legion)
    {
        int i = legions.indexOf(legion);
        if (i <= 0)
        {
            // Not found, or already first in the list.
            return false;
        }
        else
        {
            legions.remove(i);
            legions.add(0, legion);
            return true;
        }
    }


    /** Return the number of this player's legions that have moved. */
    public int legionsMoved()
    {
        int count = 0;

        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (legion.hasMoved())
            {
                count++;
            }
        }
        return count;
    }


    /** Return the number of this player's legions that have legal
        non-teleport moves remaining. */
    public int countMobileLegions()
    {
        int count = 0;
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (game.countConventionalMoves(legion) > 0)
            {
                count++;
            }
        }

        return count;
    }


    public void commitMoves()
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            legion.commitMove();
        }
    }


    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String toString()
    {
        return name;
    }


    public int getMovementRoll()
    {
        return movementRoll;
    }

    public void setMovementRoll(int movementRoll)
    {
        this.movementRoll = movementRoll;
    }


    public int getMulligansLeft()
    {
        return mulligansLeft;
    }

    public void setMulligansLeft(int number)
    {
        mulligansLeft = number;
    }


    public void resetTurnState()
    {
        summoned = false;
        teleported = false;
        movementRoll = 0;

        clearUndoStack();

        // Make sure that all legions are allowed to move and recruit.
        commitMoves();

        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();

            // Hide all stack contents from other players.
            legion.hideAllCreatures();

            // Clear old entry side and teleport information.
            legion.clearAllHexInfo();
        }
    }

    /** Clear entry side and teleport information from all of this
     *  player's legions. */
    public void clearAllHexInfo()
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            legion.clearAllHexInfo();
        }
    }


    public void rollMovement()
    {
        // Only allow rolling if it hasn't already been done.
        if (movementRoll != 0)
        {
            Game.logWarn("Called rollMovement() illegally");
            return;
        }

        if (game.getOption(Options.chooseMovement))
        {
            movementRoll = PickRoll.pickRoll(game.getMasterFrame(),
                "Pick movement roll");
            Game.logEvent(getName() + " chooses a " + movementRoll +
                " for movement");
        }
        else
        {
            movementRoll = Game.rollDie();
            Game.logEvent(getName() + " rolls a " + movementRoll +
                " for movement");
        }
        game.showMovementRoll(movementRoll);
    }


    public void takeMulligan()
    {
        if (mulligansLeft > 0)
        {
            undoAllMoves();
            Game.logEvent(getName() + " takes a mulligan");
            mulligansLeft--;
            movementRoll = 0;

            Iterator it = legions.iterator();
            while (it.hasNext())
            {
                ((Legion)it.next()).clearAllHexInfo();
            }
        }
    }


    /** Clear the legion undo stack.  This should be called at the
     *  beginning of each phase. */
    public void clearUndoStack()
    {
        undoStack.clear();
    }


    public void setLastLegionMoved()
    {
        undoStack.addFirst(mover);
        mover = null;
    }


    public void setLastLegionSplitOff(Legion legion)
    {
        undoStack.addFirst(legion);
    }


    public void undoLastMove()
    {
        if (!undoStack.isEmpty())
        {
            Legion legion = (Legion)undoStack.removeFirst();
            legion.undoMove();
        }
    }


    public void undoAllMoves()
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            legion.undoMove();
        }
    }


    /** Return true if two or more of this player's legions share
     *  a hex and they have a legal non-teleport move. */
    public boolean splitLegionHasForcedMove()
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            String hexLabel = legion.getCurrentHexLabel();
            if (game.getNumFriendlyLegions(hexLabel, this) > 1 &&
                game.countConventionalMoves(legion) > 0)
            {
                return true;
            }
        }
        return false;
    }


    public void undoLastRecruit()
    {
        if (!undoStack.isEmpty())
        {
            Legion legion = (Legion)undoStack.removeFirst();
            legion.undoRecruit();
        }

        // Update number of creatures in status window.
        game.updateStatusScreen();
    }


    public void setLastLegionRecruited(Legion legion)
    {
        undoStack.addFirst(legion);
    }


    public void undoAllRecruits()
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            legion.undoRecruit();
        }

        // Update number of creatures in status window.
        game.updateStatusScreen();
    }


    public void highlightTallLegions()
    {
        HashSet set = new HashSet();

        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (legion.getHeight() >= 7)
            {
                MasterHex hex = legion.getCurrentHex();
                set.add(hex.getLabel());
            }
        }

        game.getBoard().selectHexesByLabels(set);
    }


    public void undoLastSplit()
    {
        if (!undoStack.isEmpty())
        {
            Legion splitoff = (Legion)undoStack.removeFirst();
            String hexLabel = splitoff.getCurrentHexLabel();
            splitoff.recombine(splitoff.getParent(), true);
            game.getBoard().alignLegions(hexLabel);
            highlightTallLegions();
        }
    }


    public void undoAllSplits()
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            Legion parent = legion.getParent();
            if (parent != null && parent != legion &&
                parent.getCurrentHex() == legion.getCurrentHex())
            {
                legion.recombine(parent, false);
                it.remove();
                game.getBoard().alignLegions(parent.getCurrentHexLabel());
            }
        }

        highlightTallLegions();
    }


    public int getNumCreatures()
    {
        int count = 0;

        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            count += legion.getHeight();
        }

        return count;
    }


    public void addLegion(Legion legion)
    {
        legions.add(legion);
        game.getBoard().alignLegions(legion.getCurrentHexLabel());
    }


    public void setDonor(Legion donor)
    {
        this.donor = donor;
    }


    public Legion getDonor()
    {
        return donor;
    }


    public void setMover(Legion mover)
    {
        this.mover = mover;
    }


    public Legion getMover()
    {
        return mover;
    }


    public int getNumMarkersAvailable()
    {
        return markersAvailable.size();
    }


    public Collection getMarkersAvailable()
    {
        return markersAvailable;
    }


    public boolean isMarkerAvailable(String markerId)
    {
        return (markersAvailable.contains(markerId));
    }


    public String getFirstAvailableMarker()
    {
        if (markersAvailable.size() == 0)
        {
            return null;
        }
        return (String)markersAvailable.first();
    }


    /** Removes the selected marker from the list of those available.
     *  Returns the markerId if it was present, or null if it was not. */
    public String selectMarkerId(String markerId)
    {
        if (markersAvailable.remove(markerId))
        {
            return markerId;
        }
        else
        {
            return null;
        }
    }


    public void addLegionMarker(String markerId)
    {
        markersAvailable.add(markerId);
    }


    public void addLegionMarkers(Player player)
    {
        Collection newMarkers = player.getMarkersAvailable();
        markersAvailable.addAll(newMarkers);
    }


    /** Add points to this player's score.  Update the status window
     *  to reflect the addition. */
    public void addPoints(double points)
    {
        if (points > 0)
        {
            score += points;
            if (game != null)
            {
                game.updateStatusScreen();
            }

            Game.logEvent(getName() + " earns " + points + " points");
        }
    }


    /** Remove half-points. */
    public void truncScore()
    {
        score = Math.floor(score);
    }


    public void die(Player slayer, boolean checkForVictory)
    {
        // Engaged legions give half points to the player they're
        // engaged with.  All others give half points to slayer,
        // if non-null.

        HashSet hexLabelsToAlign = new HashSet();
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            String hexLabel = legion.getCurrentHexLabel();
            Legion enemyLegion = game.getFirstEnemyLegion(hexLabel, this);
            double halfPoints = legion.getPointValue() / 2.0;

            Player scorer;

            if (enemyLegion != null)
            {
                scorer = enemyLegion.getPlayer();
            }
            else
            {
                scorer = slayer;
            }
            if (scorer != null)
            {
                scorer.addPoints(halfPoints);
            }

            // Call the iterator's remove() method rather than
            // removeLegion() to avoid concurrent modification problems.
            hexLabelsToAlign.add(legion.getCurrentHexLabel());
            legion.prepareToRemove();
            it.remove();
        }

        // Truncate every player's score to an integer value.
        Collection players = game.getPlayers();
        it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            player.truncScore();
        }

        // Mark this player as dead.
        dead = true;

        // Record the slayer and give him this player's legion markers.
        if (slayer != null)
        {
            slayer.addPlayerElim(this);
            slayer.addLegionMarkers(this);
        }

        game.updateStatusScreen();

        MasterBoard board = game.getBoard();
        it = hexLabelsToAlign.iterator();
        while (it.hasNext())
        {
            String hexLabel = (String)it.next();
            board.alignLegions(hexLabel);
        }

        Game.logEvent(getName() + " dies");

        // See if the game is over.
        if (checkForVictory)
        {
            game.checkForVictory();
        }
    }


    public void eliminateTitan()
    {
        titanEliminated = true;
    }


    public boolean isTitanEliminated()
    {
        return titanEliminated;
    }


    /** Return the value of the boolean option given by name. Default
     *  to false if there is no such option. */
    public boolean getOption(String optname)
    {
        // If autoPlay is set, all per-player options return true.
        String value = options.getProperty(Options.autoPlay);
        if (value != null && value.equals("true"))
        {
            return true;
        }

        value = options.getProperty(optname);
        if (value != null && value.equals("true"))
        {
            return true;
        }
        else
        {
            return false;
        }
    }


    /** Set option name to (a string version of) the given boolean value. */
    public void setOption(String optname, boolean value)
    {
        options.setProperty(optname, String.valueOf(value));
        // TODO Add some triggers so that if autoPlay or autoSplit is set
        // during this player's split phase, the appropriate action
        // is called.
    }


    /** Save player options to a file.  The current format is standard
     *  java.util.Properties keyword=value. */
    public void saveOptions()
    {
        final String optionsFile = Game.optionsPath + Game.optionsSep +
            name + Game.optionsExtension;
        try
        {
            FileOutputStream out = new FileOutputStream(optionsFile);
            options.store(out, Game.configVersion);
            out.close();
        }
        catch (IOException e)
        {
            Game.logError("Couldn't write options to " + optionsFile);
        }
    }


    /** Load game options from a file. The current format is standard
     *  java.util.Properties keyword=value */
    public void loadOptions()
    {
        final String optionsFile = Game.optionsPath + Game.optionsSep +
            name + Game.optionsExtension;
        try
        {
            FileInputStream in = new FileInputStream(optionsFile);
            options.load(in);
        }
        catch (IOException e)
        {
            Game.logError("Couldn't read player options from " + optionsFile);
        }
    }


    public void clearAllOptions()
    {
        options.clear();
    }


    /** Ensure that Player menu checkboxes reflect the correct state. */
    public void syncCheckboxes()
    {
        Iterator it = Options.getPerPlayerOptions().iterator();
        while (it.hasNext())
        {
            String optname = (String)it.next();
            boolean value = getOption(optname);
            game.getBoard().twiddleOption(optname, value);
        }
    }


    public void aiSplit()
    {
        if (getOption(Options.autoSplit))
        {
            ai.split(game);
        }
    }

    public void aiMasterMove()
    {
        if (getOption(Options.autoMasterMove))
        {
            ai.masterMove(game);
        }
    }

    public void aiRecruit()
    {
        if (getOption(Options.autoRecruit))
        {
            ai.muster(game);
        }
    }

    public Creature aiReinforce(Legion legion)
    {
        if (getOption(Options.autoRecruit))
        {
            return ai.reinforce(legion, game);
        }
        return null;
    }

    public boolean aiFlee(Legion legion, Legion enemy)
    {
        if (getOption(Options.autoFlee))
        {
            return ai.flee(legion, enemy, game);
        }
        return false;
    }

    public boolean aiConcede(Legion legion, Legion enemy)
    {
        if (getOption(Options.autoFlee))
        {
            return ai.concede(legion, enemy, game);
        }
        return false;
    }

    public void aiStrike(Legion legion, Battle battle, boolean fakeDice,
        boolean forced)
    {
        if (forced || getOption(Options.autoStrike))
        {
            ai.strike(legion, battle, game, fakeDice);
        }
    }

    public boolean aiChooseStrikePenalty(Critter critter, Critter target,
        Critter carryTarget, Battle battle)
    {
        if (getOption(Options.autoStrike))
        {
            return ai.chooseStrikePenalty(critter, target, carryTarget,
                battle, game);
        }
        return false;
    }

    public void aiBattleMove()
    {
        if (getOption(Options.autoBattleMove))
        {
            ai.battleMove(game);
        }
    }

    public int aiPickEntrySide(String hexLabel, Legion legion)
    {
        if (getOption(Options.autoPickEntrySide))
        {
            return ai.pickEntrySide(hexLabel, legion, game);
        }
        return -1;
    }

    public String aiPickEngagement()
    {
        if (getOption(Options.autoPickEngagement))
        {
            return ai.pickEngagement(game);
        }
        return null;
    }

    public String aiAcquireAngel(Legion legion, ArrayList recruits, Game game)
    {
        if (getOption(Options.autoAcquireAngels))
        {
            return ai.acquireAngel(legion, recruits, game);
        }
        return null;
    }


    /** Comparator that forces this player's legion markers to come
     *  before captured markers in sort order. */
    final class MarkerComparator implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            if (!(o1 instanceof String) || !(o2 instanceof String))
            {
                throw new ClassCastException();
            }
            String s1 = (String)o1;
            String s2 = (String)o2;
            String myPrefix = getShortColor();
            boolean mine1 = s1.startsWith(myPrefix);
            boolean mine2 = s2.startsWith(myPrefix);
            if (mine1 && !mine2)
            {
                return -1;
            }
            else if (mine2 && !mine1)
            {
                return 1;
            }
            else
            {
                return s1.compareTo(s2);
            }
        }
    }
}
