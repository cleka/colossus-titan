import java.io.*;
import java.util.*;

/**
 * Simple implementation of a Titan AI
 * @version $Id$
 * @author Bruce Sherrod
 * @author David Ripton
 */


class SimpleAI implements AI
{
    private Minimax minimax = new Minimax();


    public String pickColor(Set colors, List favoriteColors)
    {
        Iterator it = favoriteColors.iterator();
        while (it.hasNext())
        {
            String preferredColor = (String)it.next();
            if (colors.contains(preferredColor))
            {
                return preferredColor;
            }
        }
        // Can't have one of our favorites, so take what's there.
        it = colors.iterator();
        if (it.hasNext())
        {
            return (String)it.next();
        }
        return null;
    }


    public String pickMarker(Collection markerIds)
    {
        Iterator it = markerIds.iterator();
        if (it.hasNext())
        {
            return (String)it.next();
        }
        return null;
    }


    public void muster(Game game)
    {
        // Do not recruit if this legion is a scooby snack.
        double scoobySnackFactor = 0.15;
        int minimumSizeToRecruit = (int)(scoobySnackFactor
                * game.getAverageLegionPointValue());
        Player player = game.getActivePlayer();
        List legions = player.getLegions();
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();

            if (legion.hasMoved() && legion.canRecruit() &&
                (legion.hasTitan() || legion.getPointValue() >=
                minimumSizeToRecruit))
            {
                Creature recruit = chooseRecruit(game, legion,
                        legion.getCurrentHex());

                if (recruit != null)
                {
                    game.doRecruit(recruit, legion);
                }
            }
        }

        game.getServer().allUnselectAllHexes();
        game.getServer().allUpdateStatusScreen();
    }


    public Creature reinforce(Legion legion, Game game)
    {
        return chooseRecruit(game, legion, legion.getCurrentHex());
    }


    private static Creature chooseRecruit(Game game, Legion legion,
        MasterHex hex)
    {
        List recruits = game.findEligibleRecruits(legion, hex.getLabel());

        if (recruits.size() == 0)
        {
            return null;
        }

        // pick the last creature in the list (which is the
        // best/highest recruit)
        Creature recruit = (Creature)recruits.get(recruits.size() - 1);

        // take third cyclops in brush
        if (recruit == Creature.gorgon && recruits.contains(Creature.cyclops)
            && legion.numCreature(Creature.behemoth) == 0
            && legion.numCreature(Creature.cyclops) == 2)
        {
            recruit = Creature.cyclops;
        }
        // take a fourth cyclops in brush
        // (so that we can split out a cyclops and still keep 3)
        else if (recruit == Creature.gorgon
                 && recruits.contains(Creature.cyclops)
                 && legion.getHeight() == 6
                 && legion.numCreature(Creature.behemoth) == 0
                 && legion.numCreature(Creature.gargoyle) == 0)
        {
            recruit = Creature.cyclops;
        }
        // prefer warlock over guardian (should be built in now)
        else if (recruits.contains(Creature.guardian)
                 && recruits.contains(Creature.warlock))
        {
            recruit = Creature.warlock;
        }
        // take a third lion/troll if we've got at least 1 way to desert/swamp
        // from here and we're not about to be attacked
        else if (recruits.contains(Creature.lion)
                 && recruits.contains(Creature.ranger)
                 && legion.numCreature(Creature.lion) == 2
                 && getNumberOfWaysToTerrain(legion, hex, 'D') > 0)
        {
            recruit = Creature.lion;
        }
        else if (recruits.contains(Creature.troll)
                 && recruits.contains(Creature.ranger)
                 && legion.numCreature(Creature.troll) == 2
                 && getNumberOfWaysToTerrain(legion, hex, 'S') > 0)
        {
            recruit = Creature.troll;
        }
        // tower creature selection:
        else if (recruits.contains(Creature.ogre) &&
            recruits.contains(Creature.centaur) &&
            recruits.contains(Creature.gargoyle) && recruits.size() == 3)
        {
            // if we have 2 centaurs or ogres, take a third
            if (legion.numCreature(Creature.ogre) == 2)
            {
                recruit = Creature.ogre;
            }
            else if (legion.numCreature(Creature.centaur) == 2)
            {
                recruit = Creature.centaur;
                // else if we have 1 of a tower creature, take a matching one
            }
            else if (legion.numCreature(Creature.gargoyle) == 1)
            {
                recruit = Creature.gargoyle;
            }
            else if (legion.numCreature(Creature.ogre) == 1)
            {
                recruit = Creature.ogre;
            }
            else if (legion.numCreature(Creature.centaur) == 1)
            {
                recruit = Creature.centaur;
                // else if there's cyclops left and we don't have 2
                // gargoyles, take a gargoyle
            }
            else if (game.getCaretaker().getCount(Creature.cyclops) > 6
                     && legion.numCreature(Creature.gargoyle) < 2)
            {
                recruit = Creature.gargoyle;
                // else if there's trolls left and we don't have 2 ogres,
                // take an ogre
            }
            else if (game.getCaretaker().getCount(Creature.troll) > 6
                     && legion.numCreature(Creature.ogre) < 2)
            {
                recruit = Creature.ogre;
                // else if there's lions left and we don't have 2 lions,
                // take a centaur
            }
            else if (game.getCaretaker().getCount(Creature.lion) > 6
                     && legion.numCreature(Creature.centaur) < 2)
            {
                recruit = Creature.centaur;
                // else we don't really care; take anything
            }
        }

        return recruit;
    }


    public void split(Game game)
    {
        Player player = game.getActivePlayer();

        // XXX Using a for loop instead of a ListIterator to get around
        // a pesky ConcurrentModificationException.
        outer: for (int i = player.getNumLegions() - 1; i >= 0; i--)
        {
            Legion legion = player.getLegion(i);

            if (legion.getHeight() < 7)
            {
                continue;
            }

            // Do not split if we're likely to be forced to attack and lose
            // Do not split if we're likely to want to fight and we need to
            //     be 7 high.
            // Do not split if there's no upwards recruiting or angel
            //     acquiring potential.

            // TODO: Don't split if we're about to be attacked and we
            // need the muscle

            // Only consider this if we're not doing initial game split
            if (legion.getHeight() == 7)
            {
                int forcedToAttack = 0;
                boolean goodRecruit = false;
                legion.sortCritters();

                for (int roll = 1; roll <= 6; roll++)
                {
                    Set moves = game.listMoves(legion, true,
                        legion.getCurrentHex(), roll, false, false);
                    int safeMoves = 0;
                    Iterator moveIt = moves.iterator();
                    while (moveIt.hasNext())
                    {
                        String hexLabel = (String)moveIt.next();
                        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);

                        if (game.getNumEnemyLegions(hexLabel, player) == 0)
                        {
                            safeMoves++;
                            if (!goodRecruit && couldRecruitUp(legion,
                                hexLabel, null, game))
                            {
                                goodRecruit = true;
                            }
                        }
                        else
                        {
                            Legion enemy = game.getFirstEnemyLegion(hexLabel,
                                player);
                            int result = estimateBattleResults(legion, true,
                                enemy, hex);

                            if (result == WIN_WITH_MINIMAL_LOSSES)
                            {
                                Log.debug(
                                    "We can safely split AND attack with "
                                    + legion);
                                safeMoves++;

                                // Also consider acquiring angel.
                                if (!goodRecruit && couldRecruitUp(legion,
                                    hexLabel, enemy, game))
                                {
                                    goodRecruit = true;
                                }
                            }

                            int result2 = estimateBattleResults(legion, false,
                                enemy, hex);

                            if (result2 == WIN_WITH_MINIMAL_LOSSES
                                && result != WIN_WITH_MINIMAL_LOSSES
                                && roll <= 4)
                            {
                                // don't split so that we can attack!
                                Log.debug("Not splitting " + legion +
                                    " because we want the muscle to attack");

                                forcedToAttack = 999;
                                continue outer;
                            }
                        }
                    }

                    if (safeMoves == 0)
                    {
                        forcedToAttack++;
                    }
                    // If we'll be forced to attack on 2 or more rolls,
                    // don't split.
                    if (forcedToAttack >= 2)
                    {
                        continue outer;
                    }
                }
                if (!goodRecruit)
                {
                    // No point in splitting, since we can't improve.
                    Log.debug("Not splitting " + legion +
                        " because it can't improve from here");
                    continue outer;
                }
            }

            // create the new legion
            Legion newLegion = legion.split(chooseCreaturesToSplitOut(legion,
                game.getNumPlayers()));

            // Hide all creatures in both legions.
            legion.hideAllCreatures();
            newLegion.hideAllCreatures();
        }
    }

    /** Return true if the legion could recruit or acquire something
     *  better than its worst creature in hexLabel. */
    private boolean couldRecruitUp(Legion legion, String hexLabel,
        Legion enemy, Game game)
    {
        legion.sortCritters();
        Critter weakest = legion.getCritter(legion.getHeight() - 1);

        // Consider recruiting.
        ArrayList recruits = game.findEligibleRecruits(legion, hexLabel);
        if (!recruits.isEmpty())
        {
            Creature bestRecruit = (Creature)recruits.get(recruits.size() - 1);
            if (bestRecruit != null && bestRecruit.getPointValue() >
                weakest.getPointValue())
            {
                return true;
            }
        }

        // Consider acquiring angels.
        if (enemy != null)
        {
            int pointValue = enemy.getPointValue();
            boolean wouldFlee = flee(enemy, legion, game);
            if (wouldFlee)
            {
                pointValue /= 2;
            }
            int currentScore = legion.getPlayer().getScore();
            Creature bestRecruit = null;
            Caretaker caretaker = game.getCaretaker();
            if ((currentScore + pointValue) / 500 > currentScore / 500 &&
                caretaker.getCount(Creature.archangel) >= 1)
            {
                bestRecruit = Creature.archangel;
            }
            else if ((currentScore + pointValue) / 100 > currentScore / 100 &&
                caretaker.getCount(Creature.angel) >= 1)
            {
                bestRecruit = Creature.angel;
            }
            if (bestRecruit != null && bestRecruit.getPointValue() >
                weakest.getPointValue())
            {
                return true;
            }
        }

        return false;
    }


    /**
     * Decide how to split this legion, and return a list of
     * Creatures to remove.
     */
    private static List chooseCreaturesToSplitOut(Legion legion,
        int numPlayers)
    {
        //
        // split a 7 or 8 high legion somehow
        //
        // idea: pick the 2 weakest creatures and kick them
        // out. if there are more than 2 weakest creatures,
        // prefer a pair of matching ones.
        //
        // For an 8-high starting legion, call helper
        // method doInitialGameSplit()
        //
        // TODO: keep 3 cyclops if we don't have a behemoth
        // (split out a gorgon instead)
        //
        // TODO: prefer to split out creatures that have no
        // recruiting value (e.g. if we have 1 angel, 2
        // centaurs, 2 gargoyles, and 2 cyclops, split out the
        // gargoyles)
        //
        if (legion.getHeight() == 8)
        {
            return doInitialGameSplit(legion.getCurrentHexLabel(), numPlayers);
        }

        Critter weakest1 = null;
        Critter weakest2 = null;

        for (Iterator critterIt = legion.getCritters().iterator();
             critterIt.hasNext(); )
        {
            Critter critter = (Critter)critterIt.next();

            // Never split out the titan.
            if (critter.isTitan())
            {
                continue;
            }

            if (weakest1 == null)
            {
                weakest1 = critter;
            }
            else if (weakest2 == null)
            {
                weakest2 = critter;
            }
            else if (critter.getPointValue() < weakest1.getPointValue())
            {
                weakest1 = critter;
            }
            else if (critter.getPointValue() < weakest2.getPointValue())
            {
                weakest2 = critter;
            }
            else if (critter.getPointValue() == weakest1.getPointValue()
                     && critter.getPointValue() == weakest2.getPointValue())
            {
                if (critter.getName().equals(weakest1.getName()))
                {
                    weakest2 = critter;
                }
                else if (critter.getName().equals(weakest2.getName()))
                {
                    weakest1 = critter;
                }
            }
        }

        ArrayList creaturesToRemove = new ArrayList();

        creaturesToRemove.add(weakest1);
        creaturesToRemove.add(weakest2);

        return creaturesToRemove;
    }


    private static List chooseCreaturesToSplitOut(Legion legion)
    {
        return chooseCreaturesToSplitOut(legion, 2);
    }


    // From Hugh Moore:
    //
    // It really depends on how many players there are and how good I
    // think they are.  In a 5 or 6 player game, I will pretty much
    // always put my gargoyles together in my Titan group. I need the
    // extra strength, and I need it right away.  In 3-4 player
    // games, I certainly lean toward putting my gargoyles together.
    // If my opponents are weak, I sometimes split them for a
    // challenge.  If my opponents are strong, but my situation looks
    // good for one reason or another, I may split them.  I never
    // like to split them when I am in tower 3 or 6, for obvious
    // reasons. In two player games, I normally split the gargoyles,
    // but two player games are fucked up.
    //

    /** Return a list of exactly four creatures (including one lord) to
     *  split out. */
    private static List doInitialGameSplit(String label, int numPlayers)
    {
        // in CMU style splitting, we split centaurs in even towers,
        // ogres in odd towers.
        final boolean oddTower = "100".equals(label) || "300".equals(label)
                || "500".equals(label);
        final Creature splitCreature = oddTower ? Creature.ogre
                : Creature.centaur;
        final Creature nonsplitCreature = oddTower ? Creature.centaur
                : Creature.ogre;

        // if lots of players, keep gargoyles with titan (we need the muscle)
        if (numPlayers > 4)
        {
            return CMUsplit(true, splitCreature, nonsplitCreature);
        }
        // don't split gargoyles in tower 3 or 6 (because of the extra jungles)
        else if ("300".equals(label) || "600".equals(label))
        {
            return CMUsplit(false, splitCreature, nonsplitCreature);
        }
        //
        // new idea due to David Ripton: split gargoyles in tower 2 or
        // 5, because on a 5 we can get to brush and jungle and get 2
        // gargoyles.  I'm not sure if this is really better than recruiting
        // a cyclops and leaving the other group in the tower, but it's
        // interesting so we'll try it.
        //
        else if ("200".equals(label) || "500".equals(label))
        {
            return MITsplit(true, splitCreature, nonsplitCreature);
        }
        //
        // otherwise, mix it up for fun
        else
        {
            if (Game.rollDie() <= 2)
            {
                return MITsplit(true, splitCreature, nonsplitCreature);
            }
            else
            {
                return CMUsplit(true, splitCreature, nonsplitCreature);
            }
        }
    }


    // Keep the gargoyles together.
    private static List CMUsplit(boolean favorTitan, Creature splitCreature,
            Creature nonsplitCreature)
    {
        LinkedList splitoffs = new LinkedList();

        if (favorTitan)
        {
            if (Game.rollDie() <= 3)
            {
                splitoffs.add(Creature.titan);
                splitoffs.add(Creature.gargoyle);
                splitoffs.add(Creature.gargoyle);
                splitoffs.add(splitCreature);
            }
            else
            {
                splitoffs.add(Creature.angel);
                splitoffs.add(nonsplitCreature);
                splitoffs.add(nonsplitCreature);
                splitoffs.add(splitCreature);
            }
        }
        else
        {
            if (Game.rollDie() <= 3)
            {
                splitoffs.add(Creature.titan);
            }
            else
            {
                splitoffs.add(Creature.angel);
            }

            if (Game.rollDie() <= 3)
            {
                splitoffs.add(Creature.gargoyle);
                splitoffs.add(Creature.gargoyle);
                splitoffs.add(splitCreature);
            }
            else
            {
                splitoffs.add(nonsplitCreature);
                splitoffs.add(nonsplitCreature);
                splitoffs.add(splitCreature);
            }
        }

        return splitoffs;
    }


    // Split the gargoyles.
    private static List MITsplit(boolean favorTitan, Creature splitCreature,
        Creature nonsplitCreature)
    {
        LinkedList splitoffs = new LinkedList();

        if (favorTitan)
        {
            if (Game.rollDie() <= 3)
            {
                splitoffs.add(Creature.titan);
                splitoffs.add(nonsplitCreature);
                splitoffs.add(nonsplitCreature);
                splitoffs.add(Creature.gargoyle);
            }
            else
            {
                splitoffs.add(Creature.angel);
                splitoffs.add(splitCreature);
                splitoffs.add(splitCreature);
                splitoffs.add(Creature.gargoyle);
            }
        }
        else
        {
            if (Game.rollDie() <= 3)
            {
                splitoffs.add(Creature.titan);
            }
            else
            {
                splitoffs.add(Creature.angel);
            }

            if (Game.rollDie() <= 3)
            {
                splitoffs.add(nonsplitCreature);
                splitoffs.add(nonsplitCreature);
                splitoffs.add(Creature.gargoyle);
            }
            else
            {
                splitoffs.add(splitCreature);
                splitoffs.add(splitCreature);
                splitoffs.add(Creature.gargoyle);
            }
        }

        return splitoffs;
    }


    public void masterMove(Game game)
    {
        if (true)
        {
            simpleMove(game);
        }
        else
        {
            // Need to handle mulligans somehow; just use the existing
            // simple hack for now.
            // TODO Extend the DiceMove concept to deal with mulligans.
            handleMulligans(game, game.getActivePlayer());
            PlayerMove playermove = (PlayerMove)minimax.search(
                new MasterBoardPosition(game, game.getActivePlayerNum()), 1);

            // apply the PlayerMove
            Iterator it = playermove.moves.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry entry = (Map.Entry)it.next();
                Legion legion = (Legion)entry.getKey();
                MasterHex hex = (MasterHex)entry.getValue();
                game.doMove(legion.getMarkerId(), hex.getLabel());
            }
        }
    }


    /** little helper to store info about possible moves */
    private class MoveInfo
    {
        final Legion legion;
        /** hex to move to.  if hex == null, then this means sit still. */
        final MasterHex hex;
        final int value;
        final int difference;       // difference from sitting still

        MoveInfo(Legion legion, MasterHex hex, int value, int difference)
        {
            this.legion = legion;
            this.hex = hex;
            this.value = value;
            this.difference = difference;
        }
    }

    public void simpleMove(Game game)
    {
        Player player = game.getActivePlayer();

        // consider mulligans
        handleMulligans(game, player);

        /** cache all places enemies can move to, for use in risk analysis. */
        HashMap[] enemyAttackMap = buildEnemyAttackMap(game, player);

        // A mapping from Legion to ArrayList of MoveInfo objects,
        // listing all moves that we've evaluated.  We use this if
        // we're forced to move.
        HashMap moveMap = new HashMap();

        // Sort legions into order of decreasing importance, so that
        // the important ones get the first chance to move to good hexes.
        player.sortLegions();

        handleVoluntaryMoves(game, player, moveMap, enemyAttackMap);

        // make sure we move splits (when forced)
        handleForcedSplitMoves(game, player, moveMap);

        // make sure we move at least one legion
        if (player.legionsMoved() == 0)
        {
            handleForcedSingleMove(game, player, moveMap);

            // Perhaps that forced move opened up a good move for another
            // legion, or forced a split move.  So iterate again.
            handleVoluntaryMoves(game, player, moveMap, enemyAttackMap);
            handleForcedSplitMoves(game, player, moveMap);
        }
    }

    private static void handleMulligans(Game game, Player player)
    {
        // TODO: This is really stupid.  Do something smart here.
        if (player.getMulligansLeft() > 0 && (player.getMovementRoll() == 2 ||
            player.getMovementRoll() == 5))
        {
            player.takeMulligan();
            player.rollMovement();
            // Necessary to update the movement roll in the title bar.
            game.getServer().allSetupMoveMenu();
        }
    }

    private void handleVoluntaryMoves(Game game, Player player,
        HashMap moveMap, HashMap [] enemyAttackMap)
    {
        boolean moved;
        List legions = player.getLegions();

        // Each time we move a legion, that may open up a
        // previously blocked move for a higher-priority
        // legion.  So when we move something, we should
        // break out of the move loop and start over.
        do
        {
            moved = false;
            for (Iterator it = legions.iterator(); it.hasNext();)
            {
                Legion legion = (Legion)it.next();
                if (legion.hasMoved())
                {
                    continue;
                }

                // compute the value of sitting still
                ArrayList moveList = new ArrayList();
                moveMap.put(legion, moveList);

                MoveInfo sitStillMove = new MoveInfo(legion, null,
                    evaluateMove(game, legion, legion.getCurrentHex(), false,
                    enemyAttackMap), 0);
                moveList.add(sitStillMove);

                // find the best move (1-ply search)
                MasterHex bestHex = null;
                int bestValue = Integer.MIN_VALUE;
                Set set = game.listMoves(legion, true, false, false);

                for (Iterator moveIterator = set.iterator();
                    moveIterator.hasNext();)
                {
                    final String hexLabel = (String)moveIterator.next();
                    final MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
                    final int value = evaluateMove(game, legion, hex, true,
                        enemyAttackMap);

                    if (value > bestValue || bestHex == null)
                    {
                        bestValue = value;
                        bestHex = hex;
                    }
                    MoveInfo move = new MoveInfo(legion, hex, value,
                        value - sitStillMove.value);
                    moveList.add(move);
                }

                // if we found a move that's better than sitting still, move
                if (bestValue > sitStillMove.value)
                {
                    if (game.doMove(legion.getMarkerId(), bestHex.getLabel()))
                    {
                        moved = true;
                        // Break out of the move loop and start over with
                        // the highest-priority unmoved legion.
                        break;
                    }
                    else
                    {
                        Log.debug("game.doMove() failed!");
                    }
                }
            }
        }
        while (moved);
    }

    private static void handleForcedSplitMoves(Game game, Player player,
        HashMap moveMap)
    {
        List legions = player.getLegions();
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            String hexLabel = legion.getCurrentHexLabel();
            List friendlyLegions = game.getFriendlyLegions(hexLabel, player);

            outer: while (friendlyLegions.size() > 1
                   && game.countConventionalMoves(legion) > 0)
            {
                // Pick the legion in this hex whose best move has the
                // least difference with its sitStillValue, scaled by
                // the point value of the legion, and force it to move.
                Log.debug("Ack! forced to move a split group");

                // first, concatenate all the moves for all the
                // legions that are here, and sort them by their
                // difference from sitting still multiplied by
                // the value of the legion.
                ArrayList allmoves = new ArrayList();
                Iterator friendlyLegionIt = friendlyLegions.iterator();
                while (friendlyLegionIt.hasNext())
                {
                    Legion friendlyLegion = (Legion)friendlyLegionIt.next();
                    ArrayList moves = (ArrayList)moveMap.get(friendlyLegion);
                    allmoves.addAll(moves);
                }

                Collections.sort(allmoves, new Comparator()
                {
                    public int compare(Object o1, Object o2)
                    {
                        MoveInfo m1 = (MoveInfo)o1;
                        MoveInfo m2 = (MoveInfo)o2;
                        return m2.difference * m2.legion.getPointValue() -
                            m1.difference * m1.legion.getPointValue();
                    }
                });

                // now, one at a time, try applying moves until we
                // have handled our split problem.
                Iterator moveIt = allmoves.iterator();
                while (moveIt.hasNext())
                {
                    MoveInfo move = (MoveInfo)moveIt.next();
                    if (move.hex == null)
                    {
                        continue;       // skip the sitStill moves
                    }
                    Log.debug("forced to move split legion " + move.legion
                            + " to " + move.hex + " taking penalty "
                            + move.difference
                            + " in order to handle illegal legion " + legion);
                    game.doMove(move.legion.getMarkerId(),
                        move.hex.getLabel());

                    // check again if this legion is ok; if so, break
                    friendlyLegions = game.getFriendlyLegions(hexLabel,
                        player);
                    if (friendlyLegions.size() > 1
                        && game.countConventionalMoves(legion) > 0)
                    {
                        continue;
                    }
                    else
                    {
                        break outer;    // legion is all set
                    }
                }
            }
        }
    }

    /** Return true if something was moved. */
    private void handleForcedSingleMove(Game game, Player player,
        HashMap moveMap)
    {
        Log.debug("Ack! forced to move someone");

        // Pick the legion whose best move has the least
        // difference with its sitStillValue, scaled by the
        // point value of the legion, and force it to move.

        // first, concatenate all the moves all legions, and
        // sort them by their difference from sitting still

        ArrayList allmoves = new ArrayList();
        List legions = player.getLegions();
        Iterator friendlyLegionIt = legions.iterator();
        while (friendlyLegionIt.hasNext())
        {
            Legion friendlyLegion = (Legion)friendlyLegionIt.next();
            ArrayList moves = (ArrayList)moveMap.get(friendlyLegion);

            allmoves.addAll(moves);
        }

        Collections.sort(allmoves, new Comparator()
        {
            public int compare(Object o1, Object o2)
            {
                MoveInfo m1 = (MoveInfo)o1;
                MoveInfo m2 = (MoveInfo)o2;
                return m2.difference * m2.legion.getPointValue() -
                    m1.difference * m1.legion.getPointValue();
            }
        });

        // now, one at a time, try applying moves until we
        // have moved a legion
        Iterator moveIt = allmoves.iterator();
        while (moveIt.hasNext() && player.legionsMoved() == 0
               && player.countMobileLegions() > 0)
        {
            MoveInfo move = (MoveInfo)moveIt.next();

            if (move.hex == null)
            {
                continue;       // skip the sitStill moves
            }

            Log.debug("forced to move " + move.legion + " to " + move.hex
                    + " taking penalty " + move.difference
                    + " in order to handle illegal legion " + move.legion);
            game.doMove(move.legion.getMarkerId(), move.hex.getLabel());
        }
    }

    private static HashMap[] buildEnemyAttackMap(Game game, Player player)
    {
        HashMap[] enemyMap = new HashMap[7];

        for (int i = 1; i <= 6; i++)
        {
            enemyMap[i] = new HashMap();
        }

        // for each enemy player
        Collection players = game.getPlayers();
        Iterator playerIt = players.iterator();
        while (playerIt.hasNext())
        {
            Player enemyPlayer = (Player)playerIt.next();

            if (enemyPlayer == player)
            {
                continue;
                // for each legion that player controls
            }

            List legions = enemyPlayer.getLegions();
            Iterator legionIt = legions.iterator();
            while (legionIt.hasNext())
            {
                Legion legion = (Legion)legionIt.next();

                // Log.debug("checking where " + legion +
                //    " can attack next turn..");
                // for each movement roll he might make
                for (int roll = 1; roll <= 6; roll++)
                {
                    // count the moves he can get to
                    Set set = game.listMoves(legion, false,
                            legion.getCurrentHex(), roll, false, false);
                    Iterator moveIt = set.iterator();
                    while (moveIt.hasNext())
                    {
                        String hexlabel = (String)moveIt.next();

                        for (int effectiveRoll = roll; effectiveRoll <= 6;
                             effectiveRoll++)
                        {
                            // legion can attack to hexlabel on a effectiveRoll
                            ArrayList list = (ArrayList)enemyMap[effectiveRoll]
                                .get(hexlabel);

                            if (list == null)
                            {
                                list = new ArrayList();
                            }

                            if (list.contains(legion))
                            {
                                continue;
                            }

                            list.add(legion);
                            // Log.debug("" + legion + " can attack "
                            // + hexlabel + " on a " + effectiveRoll);
                            enemyMap[effectiveRoll].put(hexlabel, list);
                        }
                    }
                }
            }
        }

        // Log.debug("built map");
        return enemyMap;
    }

    //
    // cheap, inaccurate evaluation function.  Returns a value for
    // moving this legion to this hex.  The value defines a distance
    // metric over the set of all possible moves.
    //
    // TODO: should be parameterized with weights
    //
    private static int evaluateMove(Game game, Legion legion, MasterHex hex,
        boolean canRecruitHere, HashMap[] enemyAttackMap)
    {
        // Avoid using MIN_VALUE and MAX_VALUE because of possible overflow.
        final int WIN_GAME = Integer.MAX_VALUE / 2;
        final int LOSE_GAME = Integer.MIN_VALUE / 2;
        final int LOSE_LEGION = -10000;

        int value = 0;
        // consider making an attack
        final Legion enemyLegion = game.getFirstEnemyLegion(hex.getLabel(),
            legion.getPlayer());

        if (enemyLegion != null)
        {
            final int enemyPointValue = enemyLegion.getPointValue();
            final int result = estimateBattleResults(legion, enemyLegion, hex);

            switch (result)
            {

                case WIN_WITH_MINIMAL_LOSSES:
                    Log.debug("legion " + legion + " can attack " + enemyLegion
                            + " in " + hex + " and WIN_WITH_MINIMAL_LOSSES");

                    // we score a fraction of an angel
                    value += (24 * enemyPointValue) / 100;
                    // plus a fraction of a titan strength
                    value += (6 * enemyPointValue) / 100;
                    // plus some more for killing a group (this is arbitrary)
                    value += (10 * enemyPointValue) / 100;

                    // TODO: if enemy titan, we also score half points
                    // (this may make the AI unfairly gun for your titan)
                    break;

                case WIN_WITH_HEAVY_LOSSES:
                    Log.debug("legion " + legion + " can attack " + enemyLegion
                            + " in " + hex + " and WIN_WITH_HEAVY_LOSSES");
                    // don't do this with our titan unless we can win the game
                    Player player = legion.getPlayer();
                    List legions = player.getLegions();
                    boolean haveOtherAngels = false;
                    Iterator it = legions.iterator();
                    while (it.hasNext())
                    {
                        Legion l = (Legion)it.next();

                        if (l == legion)
                        {
                            continue;
                        }

                        if (l.numCreature(Creature.angel) == 0)
                        {
                            continue;
                        }

                        haveOtherAngels = true;

                        break;
                    }

                    if (legion.hasTitan())
                    {
                        // unless we can win the game with this attack
                        if (enemyLegion.hasTitan() &&
                            game.getNumLivingPlayers() == 2)
                        {
                            // do it and win the game
                            value += enemyPointValue;
                        }
                        else
                        {
                            // ack! we'll fuck up our titan group
                            value += LOSE_LEGION + 10;
                        }
                    }
                    // don't do this if we'll lose our only angel group
                    // and won't score enough points to make up for it
                    else if (legion.numCreature(Creature.angel) > 0
                             &&!haveOtherAngels && enemyPointValue < 88)
                    {
                        value += LOSE_LEGION + 5;
                    }
                    else
                    {
                        // we score a fraction of an angel & titan strength
                        value += (30 * enemyPointValue) / 100;
                        // but we lose this group
                        value -= (20 * legion.getPointValue()) / 100;
                        // TODO: if we have no other angels, more penalty here
                        // TODO: if enemy titan, we also score half points
                        // (this may make the AI unfairly gun for your titan)
                    }
                    break;

                case DRAW:
                    Log.debug("legion " + legion + " can attack " + enemyLegion
                            + " in " + hex + " and DRAW");

                    // If this is an unimportant group for us, but
                    // is enemy titan, do it.  This might be an
                    // unfair use of information for the AI
                    if (legion.numLords() == 0 && enemyLegion.hasTitan())
                    {
                        // Arbitrary value for killing a player but
                        // scoring no points: it's worth a little
                        // If there are only 2 players, we should do this.
                        if (game.getNumPlayers() == 2)
                        {
                            value = WIN_GAME;
                        }
                        else
                        {
                            value += enemyPointValue / 6;
                        }
                    }
                    else
                    {
                        // otherwise no thanks
                        value += LOSE_LEGION + 2;
                    }
                    break;

                case LOSE_BUT_INFLICT_HEAVY_LOSSES:
                    Log.debug("legion " + legion + " can attack " + enemyLegion
                            + " in " + hex
                            + " and LOSE_BUT_INFLICT_HEAVY_LOSSES");

                    // TODO: how important is it that we damage his group?
                    value += LOSE_LEGION + 1;
                    break;

                case LOSE:
                    Log.debug("legion " + legion + " can attack " + enemyLegion
                            + " in " + hex + " and LOSE");

                    value += LOSE_LEGION;
                    break;
            }
        }

        // consider what we can recruit
        Creature recruit = null;

        if (canRecruitHere)
        {
            recruit = chooseRecruit(game, legion, hex);

            if (recruit != null)
            {
                int oldval = value;

                if (legion.getHeight() < 6)
                {
                    value += recruit.getPointValue();
                }
                else
                {
                    // if we're 6-high, then the value of a recruit is
                    // equal to the improvement in the value of the
                    // pieces that we'll have after splitting.
                    // TODO this should call our splitting code to see
                    // what split decision we would make

                    // This special case was overkill.  A 6-high stack
                    // with 3 lions, or a 6-high stack with 3 clopses,
                    // sometimes refused to go to a safe desert/jungle
                    // because the value of the recruit was toned down
                    // too much. So the effect has been reduced by half.
                    Log.debug("--- 6-HIGH SPECIAL CASE");

                    Critter weakest1 = null;
                    Critter weakest2 = null;

                    for (Iterator critterIt = legion.getCritters().iterator();
                         critterIt.hasNext(); )
                    {
                        Critter critter = (Critter)critterIt.next();

                        if (weakest1 == null)
                        {
                            weakest1 = critter;
                        }
                        else if (weakest2 == null)
                        {
                            weakest2 = critter;
                        }
                        else if (critter.getPointValue()
                                 < weakest1.getPointValue())
                        {
                            weakest1 = critter;
                        }
                        else if (critter.getPointValue()
                                 < weakest2.getPointValue())
                        {
                            weakest2 = critter;
                        }
                        else if (critter.getPointValue()
                                 == weakest1.getPointValue()
                                 && critter.getPointValue()
                                    == weakest2.getPointValue())
                        {
                            if (critter.getName().equals(weakest1.getName()))
                            {
                                weakest2 = critter;
                            }
                            else if (critter.getName().equals(
                                weakest2.getName()))
                            {
                                weakest1 = critter;
                            }
                        }
                    }

                    int minCreaturePV = Math.min(weakest1.getPointValue(),
                            weakest2.getPointValue());
                    int maxCreaturePV = Math.max(weakest1.getPointValue(),
                            weakest2.getPointValue());
                    // point value of my best 5 pieces right now
                    int oldPV = legion.getPointValue() - minCreaturePV;
                    // point value of my best 5 pieces after adding this
                    // recruit and then splitting off my 2 weakest
                    int newPV = legion.getPointValue()
                        - weakest1.getPointValue()
                        - weakest2.getPointValue()
                        + Math.max(maxCreaturePV, recruit.getPointValue());

                    value += (((newPV - oldPV) + recruit.getPointValue()) / 2);
                }

                Log.debug("--- if " + legion + " moves to " + hex
                        + " then recruit " + recruit.toString() + " (adding "
                        + (value - oldval) + ")");
            }
        }

        // consider what we might be able to recruit next turn, from here
        int nextTurnValue = 0;

        for (int roll = 1; roll <= 6; roll++)
        {
            Set moves = game.listMoves(legion, true, hex, roll, false, true);
            int bestRecruitVal = 0;
            Creature bestRecruit = null;

            for (Iterator nextMoveIt = moves.iterator();
                 nextMoveIt.hasNext(); )
            {
                String nextLabel = (String)nextMoveIt.next();
                MasterHex nextHex = MasterBoard.getHexByLabel(nextLabel);
                // if we have to fight in that hex and we can't
                // WIN_WITH_MINIMAL_LOSSES, then assume we can't
                // recruit there.  IDEA: instead of doing any of this
                // work, perhaps we could recurse here to get the
                // value of being in _that_ hex next turn... and then
                // maximize over choices and average over die rolls.
                // this would be essentially minimax but ignoring the
                // others players ability to move.
                Legion enemy = game.getFirstEnemyLegion(nextHex.getLabel(),
                    legion.getPlayer());

                if (enemy != null
                    && estimateBattleResults(legion, enemy, nextHex)
                       != WIN_WITH_MINIMAL_LOSSES)
                {
                    continue;
                }

                List nextRecruits = game.findEligibleRecruits(legion,
                    nextLabel);

                if (nextRecruits.size() == 0)
                {
                    continue;
                }

                Creature nextRecruit =
                    (Creature)nextRecruits.get(nextRecruits.size() - 1);
                int val = nextRecruit.getSkill() * nextRecruit.getPower();

                if (val > bestRecruitVal)
                {
                    bestRecruitVal = val;
                    bestRecruit = nextRecruit;
                }
            }

            nextTurnValue += bestRecruitVal;
        }

        nextTurnValue /= 6;     // 1/6 chance of each happening
        value += nextTurnValue;

        // consider risk of being attacked
        if (enemyAttackMap != null)
        {
            if (canRecruitHere)
            {
                Log.debug("considering risk of moving " + legion + " to " + hex);
            }
            else
            {
                Log.debug("considering risk of leaving " + legion + " in " +
                    hex);
            }

            HashMap[] enemiesThatCanAttackOnA = enemyAttackMap;
            int roll;

            for (roll = 1; roll <= 6; roll++)
            {
                List enemies =
                    (List)enemiesThatCanAttackOnA[roll].get(hex.getLabel());

                if (enemies == null)
                {
                    continue;
                }

                Log.debug("got enemies that can attack on a " + roll + " :"
                        + enemies);

                Iterator it = enemies.iterator();
                while (it.hasNext())
                {
                    Legion enemy = (Legion)it.next();
                    final int result = estimateBattleResults(enemy, false,
                            legion, hex, recruit);

                    if (result == WIN_WITH_MINIMAL_LOSSES ||
                        result == DRAW && legion.hasTitan())
                    {
                        break;
                        // break on the lowest roll from which we can
                        // be attacked and killed
                    }
                }
            }

            if (roll < 7)
            {
                final double chanceToAttack = (7.0 - roll) / 6.0;
                final double risk;

                if (legion.hasTitan())
                {
                    risk = LOSE_LEGION * chanceToAttack;
                }
                else
                {
                    risk = -legion.getPointValue() / 2 * chanceToAttack;
                }

                value += risk;
            }
        }

        // TODO: consider mobility.  e.g., penalty for suckdown
        // squares, bonus if next to tower or under the top
        // TODO: consider what we can attack next turn from here
        // TODO: consider nearness to our other legions
        // TODO: consider being a scooby snack (if so, everything
        // changes: we want to be in a location with bad mobility, we
        // want to be at risk of getting killed, etc)
        // TODO: consider risk of being scooby snacked (this might be inherent)
        // TODO: consider splitting up our good recruitment rolls
        // (i.e. if another legion has warbears under the top that
        // recruit on 1,3,5, and we have a behemoth with choice of 3/5
        // to jungle or 4/6 to jungle, prefer the 4/6 location).
        Log.debug("EVAL " + legion
                + (canRecruitHere ? " move to " : " stay in ") + hex + " = "
                + value);

        return value;
    }


    private static final int WIN_WITH_MINIMAL_LOSSES = 0;
    private static final int WIN_WITH_HEAVY_LOSSES = 1;
    private static final int DRAW = 2;
    private static final int LOSE_BUT_INFLICT_HEAVY_LOSSES = 3;
    private static final int LOSE = 4;

    private static int estimateBattleResults(Legion attacker, Legion defender,
        MasterHex hex)
    {
        return estimateBattleResults(attacker, false, defender, hex, null);
    }

    private static int estimateBattleResults(Legion attacker,
        boolean attackerSplitsBeforeBattle, Legion defender, MasterHex hex)
    {
        return estimateBattleResults(attacker, attackerSplitsBeforeBattle,
            defender, hex, null);
    }

    private static int estimateBattleResults(Legion attacker,
        boolean attackerSplitsBeforeBattle, Legion defender,
        MasterHex hex, Creature recruit)
    {
        char terrain = hex.getTerrain();
        double attackerPointValue = getCombatValue(attacker, terrain);

        if (attackerSplitsBeforeBattle)
        {
            // remove PV of the split
            List creaturesToRemove = chooseCreaturesToSplitOut(attacker);
            Iterator creatureIt = creaturesToRemove.iterator();
            while (creatureIt.hasNext())
            {
                Creature creature = (Creature)creatureIt.next();

                attackerPointValue -= getCombatValue(creature, terrain);
            }
        }

        if (recruit != null)
        {
            // Log.debug("adding in recruited " + recruit +
            // " when evaluating battle");
            attackerPointValue += getCombatValue(recruit, terrain);
        }
        // TODO: add angel call

        double defenderPointValue = getCombatValue(defender, terrain);
        // TODO: add in enemy's most likely turn 4 recruit

        if (hex.getTerrain() == 'T')
        {
            // defender in the tower!  ouch!
            defenderPointValue *= 1.2;
        }

        // TODO: adjust for entry side

        // really dumb estimator
        double ratio = (double)attackerPointValue / (double)defenderPointValue;

        if (ratio >= 1.30)
        {
            return WIN_WITH_MINIMAL_LOSSES;
        }
        else if (ratio >= 1.15)
        {
            return WIN_WITH_HEAVY_LOSSES;
        }
        else if (ratio >= 0.85)
        {
            return DRAW;
        }
        else if (ratio >= 0.70)
        {
            return LOSE_BUT_INFLICT_HEAVY_LOSSES;
        }
        else    // ratio less than 0.70
        {
            return LOSE;
        }
    }

    private static int getNumberOfWaysToTerrain(Legion legion, MasterHex hex,
        char terrainType)
    {
        // if moves[i] is true, then a roll of i can get us to terrainType.
        boolean[] moves = new boolean[7];
        // consider normal moves
        int block = Game.ARCHES_AND_ARROWS;

        for (int j = 0; j < 6; j++)
        {
            if (hex.getExitType(j) == MasterHex.BLOCK)
            {
                // Only this path is allowed.
                block = j;
            }
        }

        findNormalMovesToTerrain(legion, legion.getPlayer(), hex, 6, block,
            Game.NOWHERE, terrainType, moves);

        // consider tower teleport
        if (hex.getTerrain() == 'T' && legion.numLords() > 0
            && moves[6] == false)
        {
            // hack: assume that we can always tower teleport to the terrain we
            // want to go to.
            // TODO: check to make sure there's an open terrain to teleport to.
            // (probably want a lookup table for this)
            moves[6] = true;
        }

        int count = 0;
        for (int i = 0; i < moves.length; i++)
        {
            if (moves[i])
            {
                count++;
            }
        }
        return count;
    }

    private static void findNormalMovesToTerrain(Legion legion, Player player,
        MasterHex hex, int roll, int block, int cameFrom, char terrainType,
        boolean[] moves)
    {
        // If there are enemy legions in this hex, mark it
        // as a legal move and stop recursing.  If there is
        // also a friendly legion there, just stop recursing.
        String hexLabel = hex.getLabel();
        Game game = legion.getGame();
        if (game.getNumEnemyLegions(hexLabel, player) > 0)
        {
            if (game.getNumFriendlyLegions(hexLabel, player) == 0)
            {
                // we can move to here
                if (hex.getTerrain() == terrainType)
                {
                    moves[roll] = true;
                }
            }

            return;
        }

        if (roll == 0)
        {
            // This hex is the final destination.  Mark it as legal if
            // it is unoccupied by friendly legions.
            for (int i = 0; i < player.getNumLegions(); i++)
            {
                // Account for spin cycles.
                if (player.getLegion(i).getCurrentHex() == hex
                    && player.getLegion(i) != legion)
                {
                    return;
                }
            }

            if (hex.getTerrain() == terrainType)
            {
                moves[roll] = true;
            }

            return;
        }

        if (block >= 0)
        {
            findNormalMovesToTerrain(legion, player, hex.getNeighbor(block),
                    roll - 1, Game.ARROWS_ONLY, (block + 3) % 6, terrainType,
                    moves);
        }
        else if (block == Game.ARCHES_AND_ARROWS)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i) >= MasterHex.ARCH && i != cameFrom)
                {
                    findNormalMovesToTerrain(legion, player,
                            hex.getNeighbor(i), roll - 1, Game.ARROWS_ONLY,
                            (i + 3) % 6, terrainType, moves);
                }
            }
        }
        else if (block == Game.ARROWS_ONLY)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i) >= MasterHex.ARROW && i != cameFrom)
                {
                    findNormalMovesToTerrain(legion, player,
                            hex.getNeighbor(i), roll - 1, Game.ARROWS_ONLY,
                            (i + 3) % 6, terrainType, moves);
                }
            }
        }

        return;
    }

    public int pickEntrySide(String hexLabel, Legion legion, Game game)
    {
        // This is a really dumb placeholder.  TODO Make it smarter.
        for (int i = 1; i <= 5; i += 2)
        {
            if (legion.canEnterViaSide(hexLabel, i))
            {
                return i;
            }
        }
        return -1;
    }

    public String pickEngagement(Game game)
    {
        Set hexLabels = game.findEngagements();

        // Bail out early if we have no real choice.
        int numChoices = hexLabels.size();
        if (numChoices == 0)
        {
            return null;
        }
        if (numChoices == 1)
        {
            return (String)hexLabels.iterator().next();
        }

        String bestChoice = null;
        int bestScore = Integer.MIN_VALUE;

        Iterator it = hexLabels.iterator();
        while (it.hasNext())
        {
            String hexLabel = (String)it.next();
            int score = evaluateEngagement(hexLabel, game);
            if (score > bestScore)
            {
                bestScore = score;
                bestChoice = hexLabel;
            }
        }
        return bestChoice;
    }

    private int evaluateEngagement(String hexLabel, Game game)
    {
        // Fight losing battles last, so that we don't give away
        //    points while they may be used against us this turn.
        // Fight battles with angels first, so that those angels
        //    can be summoned out.
        // Try not to lose potential angels and recruits by having
        //    scooby snacks flee to 7-high stacks (or 6-high stacks
        //    that could recruit later this turn) and push them
        //    over 100-point boundaries.

        Player player = game.getActivePlayer();
        Legion attacker = game.getFirstFriendlyLegion(hexLabel, player);
        Legion defender = game.getFirstEnemyLegion(hexLabel, player);
        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
        int value = 0;

        final int result = estimateBattleResults(attacker, defender, hex);

        // The worse we expect to do, the more we want to put off this
        // engagement, either to avoid strengthening an enemy titan that
        // we may fight later this turn, or to increase our chances of
        // being able to call an angel.
        value -= result;

        // Avoid losing angels and recruits.
        boolean wouldFlee = flee(defender, attacker, game);
        if (wouldFlee)
        {
            int currentScore = player.getScore();
            int fleeValue = defender.getPointValue() / 2;
            if ((currentScore + fleeValue) / 100 > currentScore / 100)
            {
                if (attacker.getHeight() == 7 || attacker.getHeight() == 6 &&
                    attacker.canRecruit())
                {
                    value -= 10;
                }
                else
                {
                    // Angels go best in Titan legions.
                    if (attacker.hasTitan())
                    {
                        value += 6;
                    }
                    else
                    {
                        // A bird in the hand...
                        value += 2;
                    }
                }
            }
        }

        // Fight early with angel legions, so that others can summon.
        if (result <= WIN_WITH_HEAVY_LOSSES && attacker.hasAngel())
        {
            value += 5;
        }

        return value;
    }


    public boolean flee(Legion legion, Legion enemy, Game game)
    {
        // TODO Make this smarter.
        char terrain = legion.getCurrentHex().getTerrain();
        if (getCombatValue(legion, terrain) < 0.7 * getCombatValue(enemy,
            terrain))
        {
            return true;
        }
        return false;
    }


    public boolean concede(Legion legion, Legion enemy, Game game)
    {
        // Wimpy legions should concede if it costs the enemy an
        // angel or good recruit.
        char terrain = legion.getCurrentHex().getTerrain();
        int height = enemy.getHeight();
        if (getCombatValue(legion, terrain) < 0.5 * getCombatValue(enemy,
            terrain) && height >= 6)
        {
            int currentScore = enemy.getPlayer().getScore();
            int pointValue = legion.getPointValue();
            boolean canAcquireAngel = ((currentScore + pointValue) / 100 >
                currentScore / 100);
            // Can't use Legion.getRecruit() because it checks for
            // 7-high legions.
            boolean canRecruit = !game.findEligibleRecruits(enemy).isEmpty();
            if (height == 7 && (canAcquireAngel || canRecruit))
            {
                return true;
            }
            if (canAcquireAngel && canRecruit) // know height == 6
            {
                return true;
            }
        }
        return false;
    }


    /** Just a placeholder that always fights. */
    public static NegotiationResults negotiate()
    {
        return new NegotiationResults(null, null, true, false, null, null);
    }


    public String acquireAngel(Legion legion, ArrayList recruits, Game game)
    {
        // This is a dumb placeholder.
        // TODO If the legion is 6 high and can recruit something better,
        // or if the legion is a tiny scooby snack that's about to get
        // smooshed, turn down the angel.
        if (recruits.contains("Archangel"))
        {
            return "Archangel";
        }
        if (recruits.contains("Angel"))
        {
            return "Angel";
        }
        return null;
    }

    /** Return a string of form angeltype:donorId, or null. */
    public String summonAngel(Legion summoner, Game game)
    {
        Set set = game.findSummonableAngels(summoner);

        // Always summon the biggest possible angel, from the least
        // important legion that has one.  TODO Make this smarter.

        Legion bestLegion = null;
        Creature bestAngel = null;

        Iterator it = set.iterator();
        while (it.hasNext())
        {
            String hexLabel = (String)it.next();
            Legion legion = game.getFirstLegion(hexLabel);
            if (bestAngelType(legion).equals("Archangel"))
            {
                if (bestAngel == null || bestAngel != Creature.archangel)
                {
                    bestLegion = legion;
                    bestAngel = Creature.archangel;
                }
                else
                {
                    if (legion.compareTo(bestLegion) > 0)
                    {
                        bestLegion = legion;
                    }
                }
            }
            else  // Angel
            {
                if (bestAngel == null)
                {
                    bestLegion = legion;
                    bestAngel = Creature.angel;
                }
                else if (bestAngel == Creature.angel)
                {
                    if (legion.compareTo(bestLegion) > 0)
                    {
                        bestLegion = legion;
                    }
                }
            }
        }
        if (bestLegion == null)
        {
            return null;
        }
        return bestAngel.getName() + ":" + bestLegion.getMarkerId();
    }

    public static String bestAngelType(Legion legion)
    {
        if (legion.numCreature(Creature.archangel) >= 1)
        {
            return "Archangel";
        }
        if (legion.numCreature(Creature.angel) >= 1)
        {
            return "Angel";
        }
        return null;
    }

    public void strike(Legion legion, Battle battle, Game game,
                       boolean fakeDice)
    {
        // Repeat until no attackers with valid targets remain.
        while (!battle.isOver() && battle.findCrittersWithTargets().size() > 0)
        {
            doOneStrike(legion, battle, fakeDice);
        }
    }


    private void doOneStrike(Legion legion, Battle battle, boolean fakeDice)
    {
        // Simple one-ply group strike algorithm.
        // First make forced strikes, including rangestrikes for
        // rangestrikers with only one target.
        battle.makeForcedStrikes(true);

        // Then create a map containing each target and the likely number
        // of hits it would take if all possible creatures attacked it.
        HashMap map = new HashMap();
        Iterator it = legion.getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Set set = battle.findStrikes(critter, true);
            Iterator it2 = set.iterator();
            while (it2.hasNext())
            {
                String hexLabel = (String)it2.next();
                Critter target = battle.getCritter(hexLabel);
                int dice = critter.getDice(target);
                int strikeNumber = critter.getStrikeNumber(target);
                double h = Probs.meanHits(dice, strikeNumber);

                if (map.containsKey(target))
                {
                    double d = ((Double)map.get(target)).doubleValue();
                    h += d;
                }

                map.put(target, new Double(h));
            }
        }

        // Pick the most important target that can likely be killed this
        // turn.  If none can, pick the most important target.
        boolean canKillSomething = false;
        Critter bestTarget = null;
        char terrain = battle.getTerrain();

        it = map.keySet().iterator();
        while (it.hasNext())
        {
            Critter target = (Critter)it.next();
            double h = ((Double)map.get(target)).doubleValue();

            if (h + target.getHits() >= target.getPower())
            {
                // We can probably kill this target.
                if (bestTarget == null ||!canKillSomething
                    || getKillValue(target, terrain)
                       > getKillValue(bestTarget, terrain))
                {
                    bestTarget = target;
                    canKillSomething = true;
                }
            }
            else
            {
                // We probably can't kill this target.
                if (bestTarget == null
                    || (!canKillSomething
                        && getKillValue(target, terrain)
                           > getKillValue(bestTarget, terrain)))
                {
                    bestTarget = target;
                }
            }
        }

        if (bestTarget == null)
        {
            return;
        }

        Log.debug("Best target is " + bestTarget.getDescription());

        // Having found the target, pick an attacker.  The
        // first priority is finding one that does not need
        // to worry about carry penalties to hit this target.
        // The second priority is using the weakest attacker,
        // so that more information is available when the
        // stronger attackers strike.
        Critter bestAttacker = null;

        it = legion.getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.canStrike(bestTarget))
            {
                if (critter.possibleStrikePenalty(bestTarget))
                {
                    if (bestAttacker == null ||
                        (bestAttacker.possibleStrikePenalty(bestTarget)
                            && getCombatValue(critter, terrain) <
                                getCombatValue(bestAttacker, terrain)))
                    {
                        bestAttacker = critter;
                    }
                }
                else
                {
                    if (bestAttacker == null ||
                        bestAttacker.possibleStrikePenalty(bestTarget) ||
                        getCombatValue(critter, terrain) <
                            getCombatValue(bestAttacker, terrain))
                    {
                        bestAttacker = critter;
                    }
                }
            }
        }

        Log.debug("Best attacker is " + bestAttacker.getDescription());
        // Having found the target and attacker, strike.
        // Take a carry penalty if there is still a 95%
        // chance of killing this target.
        bestAttacker.strike(bestTarget, fakeDice);

        // If there are any carries, apply them first to
        // the biggest creature that could be killed with
        // them, then to the biggest creature.
        while (battle.getCarryDamage() > 0)
        {
            bestTarget = null;

            Set set = battle.findCarryTargets();

            it = set.iterator();
            while (it.hasNext())
            {
                String hexLabel = (String)it.next();
                Critter target = battle.getCritter(hexLabel);

                if (target.wouldDieFrom(battle.getCarryDamage()))
                {
                    if (bestTarget == null ||
                        !bestTarget.wouldDieFrom(battle.getCarryDamage()) ||
                        getKillValue(target, terrain)
                           > getKillValue(bestTarget, terrain))
                    {
                        bestTarget = target;
                    }
                }
                else
                {
                    if (bestTarget == null ||
                        (!bestTarget.wouldDieFrom(battle.getCarryDamage()) &&
                        getKillValue(target, terrain) >
                        getKillValue(bestTarget, terrain)))
                    {
                        bestTarget = target;
                    }
                }

                Log.debug("Best carry target is " + bestTarget.getDescription());
                battle.applyCarries(bestTarget);
            }
        }
    }


    public boolean chooseStrikePenalty(Critter critter, Critter target,
            Critter carryTarget, Battle battle, Game game)
    {
        // If we still have a 95% chance to kill target even after
        // taking the penalty to carry to carryTarget, return true.
        final double carryThreshold = 0.95;

        int dice = Math.min(critter.getDice(target),
                critter.getDice(carryTarget));
        int strikeNumber = Math.max(critter.getStrikeNumber(target),
                critter.getStrikeNumber(carryTarget));
        int hitsNeeded = target.getPower() - target.getHits();

        return (Probs.probHitsOrMore(dice, strikeNumber, hitsNeeded) >=
            carryThreshold);
    }


    public static int getCombatValue(Creature creature, char terrain)
    {
        int val = creature.getPointValue();

        if (creature.isFlier())
        {
            val++;
        }

        if (creature.isRangestriker())
        {
            val++;
        }

        if (MasterHex.isNativeCombatBonus(creature, terrain))
        {
            val++;
        }

        // Weak titans can't risk fighting effectively.
        if (creature.isTitan())
        {
            int power = creature.getPower();

            if (power < 9)
            {
                val -= (6 + 2 * (9 - power));
            }
        }

        return val;
    }


    public static int getCombatValue(Legion legion, char terrain)
    {
        int val = 0;
        Collection critters = legion.getCritters();
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();

            val += getCombatValue(critter, terrain);
        }

        return val;
    }


    private static int getKillValue(Creature creature, char terrain)
    {
        int val = creature.getPointValue();
        if (creature.isFlier())
        {
            val++;
        }
        if (creature.isRangestriker())
        {
            val++;
        }
        if (MasterHex.isNativeCombatBonus(creature, terrain))
        {
            val++;
        }
        if (creature.isTitan())
        {
            val += 100;
        }
        return val;
    }


    ////////////////////////////////////////////////////////////////
    // minimax stuff
    ////////////////////////////////////////////////////////////////
    class MasterBoardPosition implements Minimax.GamePosition
    {
        // AICopy() of the game
        Game game;
        // the player for whom we're doing the evaluation.
        // note, this is NOT the same as Game.getActivePlayerNum()
        int AIPlayerNum;
        HashMap[] enemyAttackMap;

        public MasterBoardPosition(Game game, int AIPlayerNum)
        {
            this.game = game.AICopy();
            this.AIPlayerNum = AIPlayerNum;
            enemyAttackMap = buildEnemyAttackMap(game,
                game.getPlayer(AIPlayerNum));
        }

        public MasterBoardPosition(MasterBoardPosition position)
        {
            this.game = position.game.AICopy();
            this.AIPlayerNum = position.AIPlayerNum;
            enemyAttackMap = buildEnemyAttackMap(game,
                game.getPlayer(AIPlayerNum));
        }

        public int maximize()
        {
            if (AIPlayerNum < 0)
            {
                return Minimax.AVERAGE;
            }
            else if (game.getActivePlayerNum() == AIPlayerNum)
            {
                return Minimax.MAXIMIZE;
            }
            else
            {
                return Minimax.MINIMIZE;
            }
        }

        public int evaluation()
        {
            Log.debug("evaluating game position");

            // TODO: need to correct for the fact that more material
            // is not always better.
            // idea: score for legion markers available?
            final Player activePlayer =
                game.getPlayer(Math.abs(AIPlayerNum));

            // check for loss
            if (activePlayer.isDead())
            {
                Log.debug("evaluation: loss! " + Integer.MIN_VALUE);

                return Integer.MIN_VALUE;
            }

            // check for victory
            {
                int playersRemaining = game.getNumPlayersRemaining();
                switch (playersRemaining)
                {
                    case 0:
                        Log.debug("evaluation: draw! " + 0);
                        return 0;

                    case 1:
                        Log.debug("evaluation: win! " + Integer.MAX_VALUE);
                        return Integer.MAX_VALUE;
                }
            }

            int value = 0;

            for (Iterator playerIt = game.getPlayers().iterator();
                 playerIt.hasNext(); )
            {
                Player player = (Player)playerIt.next();
                if (player == activePlayer)
                {
                    for (Iterator it = player.getLegions().iterator();
                         it.hasNext(); )
                    {
                        Legion legion = (Legion)it.next();
                        value += evaluateMove(game, legion,
                            legion.getCurrentHex(), legion.hasMoved(),
                            enemyAttackMap);
                    }
                    // TODO: add additional value for player having
                    // stacks near each other
                }
                else
                {
                    for (Iterator it = player.getLegions().iterator();
                         it.hasNext(); )
                    {
                        Legion legion = (Legion)it.next();
                        value += evaluateMove(game, legion,
                            legion.getCurrentHex(), legion.hasMoved(), null);
                    }
                    // TODO: add additional value for player having
                    // his stacks near each other
                }
            }
            Log.debug("evaluation: " + value);
            return value;
        }


        public Iterator generateMoves()
        {
            Log.debug("generating moves..");

            // check for loss
            final Player activePlayer =
                game.getPlayer(Math.abs(AIPlayerNum));

            if (activePlayer.isDead())                  // oops! we lost
            {
                return new ArrayList().iterator();      // no moves
            }

            // check for victory
            {
                int playersRemaining = game.getNumPlayersRemaining();
                if (playersRemaining < 2)               // draw or win
                {
                    return new ArrayList().iterator();  // no moves
                }
            }

            // dice moves
            if (AIPlayerNum < 0)
            {
                // dice moves
                int playernum = 0 - game.getActivePlayerNum();
                ArrayList moves = new ArrayList(6);

                for (int i = 1; i <= 6; i++)
                {
                    moves.add(new DiceMove(i, this));
                }
                return moves.iterator();
            }

            // enumerate moves for player i

            // Friendly legions can block one another's moves, so to
            // really see all possible moves we need to have listMoves
            // ignore friendly legions.  Then to make certain moves
            // we need to move legions in the right order.

            // Not moving is also a move, though at least one legion
            // has to move, and split legions must be separated if possible.

            // Not moving, and a spin cycle on a 6 in some hexes, are
            // different moves with the same destination, so we need to
            // distinguish somehow.

            // Get the superset of all possible legion moves, and then
            // find all permutations of legion moves to get player moves,
            // and then worry about eliminating the illegal ones.  (Two
            // legions ending up in the same hex, unless both started
            // there and can't move.  Also two legions teleporting.)

            ArrayList allmoves = new ArrayList();
            Iterator it = game.getActivePlayer().getLegions().iterator();
            while (it.hasNext())
            {
                Legion legion = (Legion)it.next();

                Iterator it2 = game.listMoves(legion, true, false,
                    true).iterator();
                while (it2.hasNext())
                {
                    String hexLabel = (String)it2.next();
                    MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
                    HashMap moves = new HashMap();
                    moves.put(legion, hex);
                    PlayerMove move = new PlayerMove(moves, this);
                    allmoves.add(move);
                }
            }
            Log.debug("considering " + allmoves.size() + " possible moves ");
            return allmoves.iterator();
        }

        public Minimax.Move generateNullMove()
        {
            return new PlayerMove(null, this);
        }

        public Minimax.GamePosition applyMove(Minimax.Move move)
        {
            Log.debug("applying moves..");

            if (move instanceof DiceMove)
            {
                Log.debug("applying dice move");

                // apply dice rolling
                DiceMove dicemove = (DiceMove)move;
                MasterBoardPosition position =
                    new MasterBoardPosition(dicemove.position);

                position.AIPlayerNum = Math.abs(AIPlayerNum);
                int roll = dicemove.roll;
                position.game.getActivePlayer().setMovementRoll(roll);
                return position;
            }
            else if (move instanceof PlayerMove)
            {
                Log.debug("applying player move");

                PlayerMove playermove = (PlayerMove)move;
                MasterBoardPosition position =
                    new MasterBoardPosition(playermove.position);

                // apply the PlayerMove moves
                for (Iterator it = playermove.moves.entrySet().iterator();
                     it.hasNext(); )
                {
                    Map.Entry entry = (Map.Entry)it.next();
                    Legion legion = (Legion)entry.getKey();
                    MasterHex hex = (MasterHex)entry.getValue();

                    Log.debug("applymove: try " + legion + " to " + hex);
                    game.doMove(legion.getMarkerId(), hex.getLabel());
                }

                // advance phases until we reach the next move phase
                game.advancePhase(game.getPhase());

                while (game.getPhase() != Game.MOVE)
                {
                    switch (game.getPhase())
                    {
                        case Game.FIGHT:
                            // fake resolution for all fights
                            // TODO: need more accurate fight estimator
                            Player player = game.getActivePlayer();

                            for (int i = 0; i < player.getNumLegions(); i++)
                            {
                                Legion legion = player.getLegion(i);
                                String hexLabel= legion.getCurrentHexLabel();
                                Legion enemy = game.getFirstEnemyLegion(
                                    hexLabel, player);

                                if (enemy == null)
                                {
                                    continue;
                                }

                                Player enemyPlayer = enemy.getPlayer();
                                int myPV = legion.getPointValue();
                                int enemyPV = enemy.getPointValue();
                                boolean myTitan = legion.hasTitan();
                                boolean enemyTitan = enemy.hasTitan();

                                if (myPV * 0.8 > enemyPV)
                                {
                                    // i win
                                    enemy.remove();
                                    player.addPoints(enemyPV);

                                    if (enemyTitan)
                                    {
                                        enemyPlayer.die(player.getName(),
                                            false);
                                    }
                                }
                                else if (enemyPV * 0.8 > myPV)
                                {
                                    // enemy wins
                                    legion.remove();
                                    enemyPlayer.addPoints(myPV);

                                    if (myTitan)
                                    {
                                        player.die(enemyPlayer.getName(),
                                            false);
                                    }
                                }
                                else
                                {
                                    // both groups destroyed
                                    legion.remove();
                                    enemy.remove();
                                }
                            }
                            break;

                        case Game.SPLIT:
                            split(game);
                            break;

                        case Game.MUSTER:
                            muster(game);
                            break;
                    }

                    // now advance again until we get to MOVE phase
                    game.advancePhase(game.getPhase());
                }

                // set activePlayer negative so that we average over dice rolls
                position.AIPlayerNum = -1 * Math.abs(AIPlayerNum);

                return position;
            }
            else
            {
                throw new RuntimeException("ack! bad move type");
            }
        }
    }

    class MasterBoardPositionMove implements Minimax.Move
    {
        protected MasterBoardPosition position;
        private int value;

        public void setValue(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }
    }

    class DiceMove extends MasterBoardPositionMove
    {
        int roll;

        public DiceMove(int roll, MasterBoardPosition position)
        {
            this.position = position;
            this.roll = roll;
        }
    }

    class PlayerMove extends MasterBoardPositionMove
    {
        HashMap moves;

        public PlayerMove(HashMap moves, MasterBoardPosition position)
        {
            this.position = position;
            this.moves = moves;
        }
    }




    ////////////////////////////////////////////////////////////////
    // Battle move stuff
    ////////////////////////////////////////////////////////////////

    /*
     Battles are 2-player games within the multiplayer titan game.
     They must be evaluated within that context.  So not all
     winning positions are equally good, and not all losing
     positions are equally bad, since the surviving contents of
     the winning stack matter. All results that kill the last
     enemy titan while leaving ours alive are equally good, though,
     and all results that get our titan killed are equally bad.

     We can greatly simplify analysis by assuming that every strike
     will score the average number of hits.  This may or may not
     be good enough.  In particular, exposing a titan in a situation
     where a slightly above average number of hits will kill it is
     probably unwise, so we need to hack in some extra caution for
     titans.

     There are 27 hexes on each battle map.  A fast creature starting
     near the middle of the map can move to all of them, terrain and
     other creatures permitting.  So the possible number of different
     positions after one move is huge.  So we can't really iterate over
     all possible moves.  We need to consider one creature at a time.
     But we also need to use team tactics.

     When finding all possible moves, we need to take into account
     that friendly creatures can block one another's moves.  That
     gets really complex, so instead assume that they don't when
     computing all possible moves, and then try to ensure that
     less important creatures get out of the way of more important
     ones.
     */


    public void battleMove(Game game)
    {
        Log.debug("Called battleMove()");

        // Consider one critter at a time, in order of importance.
        // Examine all possible moves for that critter not already
        // taken by a more important one.

        // TODO Handle summoned/recruited critters, in particular
        // getting stuff out of the way so that a reinforcement
        // has room to enter.

        ArrayList bestOrder = findBattleMoves(game);

        // Now that critters are sorted into the order in which they
        // should move, start moving them for real.  If the preferred
        // move fails, fall back to the critter's remaining moves.

        Battle battle = game.getBattle();
        final char terrain = battle.getTerrain();
        Legion legion = battle.getActiveLegion();

        if (bestOrder != null)
        {
            Iterator it = bestOrder.iterator();
            while (it.hasNext())
            {
                ArrayList moveList = (ArrayList)it.next();
                Iterator it2 = moveList.iterator();
                while (it2.hasNext())
                {
                    CritterMove cm = (CritterMove)it2.next();
                    Critter fakeCritter = cm.getCritter();
                    // Get the critter in this legion with the same tag.
                    Critter critter = legion.getCritterByTag(
                        fakeCritter.getTag());

                    BattleHex hex = cm.getEndingHex(terrain);
                    Log.debug("applymove: try " + critter + " to " +
                        hex.getLabel());
                    if (battle.doMove(critter, hex))
                    {
                        // The move was okay, so continue to the next critter.
                        break;
                    }
                }
            }
        }

        Log.debug("Done with battleMove");
    }

    /** Compute a set of CritterMoves for the game's active legion.
     *  Return an ArrayList containing one ArrayList of CritterMoves
     *  for each Critter. */
    public ArrayList findBattleMoves(Game realGame)
    {
        // Consider one critter at a time, in order of importance.
        // Examine all possible moves for that critter not already
        // taken by a more important one.

        // TODO Handle summoned/recruited critters, in particular
        // getting stuff out of the way so that a reinforcement
        // has room to enter.

        // Work on a copy of the game state.  The caller is responsible
        // for actually making the moves.
        final Game game = realGame.AICopy();

        final Battle battle = game.getBattle();
        final char terrain = battle.getTerrain();
        final Legion legion = battle.getActiveLegion();
        ArrayList critters = legion.getCritters();

Log.debug("There are " + critters.size() + " critters");

        // Sort critters in decreasing order of importance.  Keep
        // identical creatures together with a secondary sort by
        // creature name.
        Collections.sort(critters, new CritterComparator(terrain));

        // allCritterMoves is an ArrayList of moveLists.
        final ArrayList allCritterMoves = new ArrayList();
        HashSet hexesTaken = new HashSet();

        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            String currentHexLabel = critter.getCurrentHexLabel();

            // moves is a list of hex labels where one critter
            // can move.

            // Sometimes friendly critters need to get out of the way
            // to clear a path for a more important critter.  We
            // consider moves that the critter could make,
            // disregarding mobile allies.

            // TODO Make less important creatures get out of the way.

            Set moves = battle.showMoves(critter, true);

            // Not moving is also an option.
            moves.add(currentHexLabel);

Log.debug("Found " + moves.size() + " moves for " + critter.getDescription());

            // Move previously considered critters into their preferred
            // position so we can take them into account when evaluating
            // this critter's moves.
            Iterator it2 = allCritterMoves.iterator();
            while (it2.hasNext())
            {
                ArrayList moveList = (ArrayList)it2.next();
                CritterMove cm = (CritterMove)moveList.get(0);
                Critter critter2 = cm.getCritter();
                critter2.moveToHex(cm.getEndingHex(terrain));
            }

            // moveList is a list of CritterMoves for one critter.
            ArrayList moveList = new ArrayList();

            it2 = moves.iterator();
            while (it2.hasNext())
            {
                String hexLabel = (String)it2.next();

                // Don't bother evaluating this hex if a more important
                // critter already has dibs on it.
                if (hexesTaken.contains(hexLabel))
                {
                    continue;
                }

                CritterMove cm = new CritterMove(critter,
                   currentHexLabel, hexLabel);
                BattleHex hex = HexMap.getHexByLabel(terrain, hexLabel);

                // Need to move the critter to evaluate.
                critter.moveToHex(hex);

                // Compute and save the value for each CritterMove.
                cm.setValue(evaluateCritterMove(battle, critter));
                moveList.add(cm);
            }
            // Move the critter back where it started.
            critter.moveToHex(critter.getStartingHex());

            // Sort critter moves in descending order of score.
            Collections.sort(moveList, new Comparator()
            {
                public int compare(Object o1, Object o2)
                {
                    CritterMove cm1 = (CritterMove)o1;
                    CritterMove cm2 = (CritterMove)o2;
                    return cm2.getValue() - cm1.getValue();
                }
            });


            // Mark this critter's favorite move as taken, unless it's
            // offboard.
            CritterMove cm = (CritterMove)moveList.get(0);
            String hexLabel = cm.getEndingHexLabel();
            if (!hexLabel.startsWith("X"))
            {
                hexesTaken.add(hexLabel);
            }

            // Show the moves considered.
            StringBuffer buf = new StringBuffer("Considered " +
                moveList.size() + " moves for " + critter.getName() + " in " +
                critter.getStartingHexLabel() + ":");
            it2 = moveList.iterator();
            while (it2.hasNext())
            {
                cm = (CritterMove)it2.next();
                buf.append(" " + cm.getEndingHexLabel());
            }
            Log.debug(buf.toString());

            // Add this critter's moves to the list.
            allCritterMoves.add(moveList);

            // Put all critters back where they started.
            it2 = allCritterMoves.iterator();
            while (it2.hasNext())
            {
                moveList = (ArrayList)it2.next();
                cm = (CritterMove)moveList.get(0);
                Critter critter2 = cm.getCritter();
                critter2.setCurrentHexLabel(cm.getStartingHexLabel());
            }
        }


        // Remove critters that don't want to move from the list
        // before finding permutations of move orders.
        int perfectScore = 0;
        ArrayList trimmedCritterMoves = (ArrayList)allCritterMoves.clone();

        Collections.sort(trimmedCritterMoves, new MoveOrderComparator(battle));

        it = trimmedCritterMoves.iterator();
        while (it.hasNext())
        {
            ArrayList moveList = (ArrayList)it.next();
            CritterMove cm = (CritterMove)moveList.get(0);
            if (cm.getStartingHexLabel().equals(cm.getEndingHexLabel()))
            {
                it.remove();
            }
            else
            {
                perfectScore += cm.getCritter().getPointValue();
            }
        }
Log.debug("perfect score is : " + perfectScore);

        if (perfectScore == 0)
        {
            // No moves, so exit.
Log.debug("no moves");
            return null;
        }

        // Figure the order in which creatures should move to get in
        // each other's way as little as possible.
        // Iterate through all permutations of critter move orders,
        // tracking how many critters get their preferred hex with each
        // order, until we find an order that lets every creature reach
        // its preferred hex.  If none does, take the best we can find.

        // This is too slow.  TODO Optimize it.  Move it to a separate
        // worker thread if necessary, so that the GUI stays responsive.
        // Need a progress indicator.

        int turn = battle.getTurnNumber();
        int bestScore = 0;
        ArrayList bestOrder = null;
        ArrayList lastOrder = null;
        int count = 0;

        it = new Perms(trimmedCritterMoves).iterator();
        while (it.hasNext())
        {
            ArrayList order = (ArrayList)it.next();

            // On turn 1, all moving critters of the same creature type are
            // identical (since all come from the same entrance and none
            // are wounded), so we should be able to skip testing any
            // permutations that are effectively identical.

            // TODO Generate and save all permutations, then sort, so that
            // we can eliminate all duplicates, not just consecutive ones.


            if (turn == 1 && creatureNames(order).equals(creatureNames(
                lastOrder)))
            {
                continue;
            }

            count++;

            // TODO Change cursor to hourglass after N iterations.
            // Maybe pop up a progress bar or progress monitor.
if (count % 100 == 0)
{
Log.debug(count + " tries");
}

            int score = testMoveOrder(order, battle);
            if (score > bestScore)
            {
                bestOrder = (ArrayList)order.clone();
                bestScore = score;
                if (score >= perfectScore)
                {
Log.debug("got perfect score: " + score);
                    break;
                }
            }
            lastOrder = (ArrayList)order.clone();
        }
Log.debug("Got score " + bestScore + " in " + count + " permutations");
        return bestOrder;
    }

    // TODO Optimize this method.
    /** Try each of the moves in order.  Return the number that succeed,
     *  scaled by the importance of each critter. */
    private int testMoveOrder(ArrayList order, Battle battle)
    {
        int val = 0;
        Iterator it = order.iterator();
        while (it.hasNext())
        {
            ArrayList moveList = (ArrayList)it.next();
            CritterMove cm = (CritterMove)moveList.get(0);
            Critter critter = cm.getCritter();
            BattleHex hex = cm.getEndingHex(battle.getTerrain());
            if (battle.testMove(critter, hex))
            {
                val += critter.getPointValue();
            }
        }
        // Move them all back where they started.
        it = order.iterator();
        while (it.hasNext())
        {
            ArrayList moveList = (ArrayList)it.next();
            CritterMove cm = (CritterMove)moveList.get(0);
            Critter critter = cm.getCritter();
            String hexLabel = cm.getStartingHexLabel();
            critter.setCurrentHexLabel(hexLabel);
        }
        return val;
    }

    /** For an ArrayList of moveLists, which are ArrayLists of CritterMoves,
     *  concatenate all the creature names in order.  If the list is null
     *  or empty, return an empty String. */
    private String creatureNames(ArrayList list)
    {
        if (list == null)
        {
            return "";
        }
        StringBuffer buf = new StringBuffer("");
        Iterator it = list.iterator();
        while (it.hasNext())
        {
            ArrayList moveList = (ArrayList)it.next();
            CritterMove cm = (CritterMove)moveList.get(0);
            Critter critter = cm.getCritter();
            buf.append(critter.getName());
        }
        return buf.toString();
    }

    private static int evaluateCritterMove(Battle battle, Critter critter)
    {
        final char terrain = battle.getTerrain();
        final String masterHexLabel = battle.getMasterHexLabel();
        final Legion legion = critter.getLegion();
        final Legion enemy = battle.getInactiveLegion();
        final int skill = critter.getSkill();
        final BattleHex hex = critter.getCurrentHex();
        final int turn = battle.getTurnNumber();

        int value = 0;

        // Add for sitting in favorable terrain.
        // Subtract for sitting in unfavorable terrain.
        if (hex.isEntrance())
        {
            // Staying offboard to die is really bad.
            value -= 10 * getCombatValue(critter, terrain);
        }
        else if (MasterHex.isNativeCombatBonus(critter, terrain))
        {
            if (hex.isNativeBonusTerrain())
            {
                value += 10;
            }
            // Hack: We want marsh natives to slightly prefer
            // moving to bog hexes, even though there's no
            // real bonus there, to leave other hexes clear
            // for non-native allies.
            else if (hex.getTerrain() == 'o')
            {
                value += 9;
            }
        }
        else
        {
            if (hex.isNonNativePenaltyTerrain())
            {
                value -= 20;
            }
        }

        Set targetHexLabels = battle.findStrikes(critter, true);
        int numTargets = targetHexLabels.size();

        // TODO Reward ganging up on enemies.  Difficult because not
        // all allies have yet moved.

        if (numTargets >= 1)
        {
            if (!critter.isInContact(true))
            {
                // Rangestrikes.
                value += 30;

                // Having multiple targets is good, in case someone else
                // kills one.
                if (numTargets >= 2)
                {
                    value += 10;
                }

                // Non-warlock skill 4 rangestrikers should slightly prefer
                // range 3 to range 4.  Non-brush rangestrikers should
                // prefer strikes not through bramble.  Warlocks should
                // try to rangestrike titans.
                boolean bonus = false;
                Iterator it = targetHexLabels.iterator();
                while (it.hasNext())
                {
                    String hexLabel = (String)it.next();
                    Critter target = battle.getCritter(hexLabel);
                    if (target.isTitan())
                    {
                        value += 10;
                    }
                    int strikeNum = critter.getStrikeNumber(target);
                    if (strikeNum == 4 - skill + target.getSkill())
                    {
                        // No penality.
                        bonus = true;
                    }
                }
                if (bonus)
                {
                    value += 10;
                }
            }
            else
            {
                // Normal strikes.  If we can strike them, they can
                // strike us.

                // Reward being adjacent to an enemy if attacking.
                if (legion == battle.getAttacker())
                {
                    value += 40;
                }
                // Slightly penalize being adjacent to an enemy if defending.
                else
                {
                    value -= 2;
                }

                int killValue = 0;
                int numKillableTargets = 0;
                int hitsExpected = 0;

                Iterator it = targetHexLabels.iterator();
                while (it.hasNext())
                {
                    String hexLabel = (String)it.next();
                    Critter target = battle.getCritter(hexLabel);

                    // Reward being next to enemy titans.  (Banzai!)
                    if (target.isTitan())
                    {
                        value += 50;
                    }

                    // Reward being next to a rangestriker, so it can't hang
                    // back and plink us.
                    if (target.isRangestriker() && !critter.isRangestriker())
                    {
                        value += 50;
                    }

                    // Reward being next to an enemy that we can probably
                    // kill this turn.
                    int dice = critter.getDice(target);
                    int strikeNum = critter.getStrikeNumber(target);
                    double meanHits = Probs.meanHits(dice, strikeNum);
                    if (meanHits + target.getHits() >= target.getPower())
                    {
                        numKillableTargets++;
                        int targetValue = getKillValue(target, terrain);
                        killValue = Math.max(targetValue, killValue);
                    }

                    // Penalize damage that we can take this turn.
                    dice = target.getDice(critter);
                    strikeNum = target.getStrikeNumber(critter);
                    hitsExpected += Probs.meanHits(dice, strikeNum);
                }

                value += 8 * killValue + numKillableTargets;

                int power = critter.getPower();
                int hits = critter.getHits();
                if (hitsExpected + hits >= power)
                {
                    value -= 20 * getKillValue(critter, terrain);
                }
                else
                {
                    value -= 6 * getKillValue(critter, terrain) *
                        hitsExpected / power;
                }
            }
        }

        // Reward adjacent friendly creatures.
        int buddies = critter.numAdjacentAllies();
        value += 15 * buddies;

        BattleHex entrance = BattleMap.getEntrance(terrain, masterHexLabel,
            legion);

        // Reward titans sticking to the edges of the back row
        // surrounded by allies.  We need to relax this in the
        // last few turns of the battle, so that attacking titans
        // don't just sit back and wait for a time loss.
        if (critter.isTitan())
        {
            value += 40 * buddies;

            if (terrain == 'T')
            {
                // Stick to the center of the tower.
                value += 100 * hex.getElevation();
            }
            else
            {
                if (turn <= 4)
                {
                    value -= 30 * battle.getRange(hex, entrance, true);
                    for (int i = 0; i < 6; i++)
                    {
                        BattleHex neighbor = hex.getNeighbor(i);
                        if (neighbor == null || neighbor.getTerrain() == 't')
                        {
                            value += 40;
                        }
                    }
                }
            }
        }

        // Encourage defending critters to hang back, and
        // attacking critters to move forward.
        else if (legion == battle.getDefender())
        {
            if (terrain == 'T')
            {
                // Stick to the center of the tower.
                value += 8 * hex.getElevation();
            }
            else
            {
                int range = battle.getRange(hex, entrance, true);

                // To ensure that defending legions completely enter
                // the board, prefer the second row to the first.  The
                // exception is small legions early in the battle,
                // when trying to survive long enough to recruit.
                int preferredRange = 3;
                if (legion.getHeight() <= 3 && battle.getTurnNumber() < 4)
                {
                    preferredRange = 2;
                }
                if (range != preferredRange)
                {
                    value -= 6 * Math.min(range, 2);
                }
            }
        }
        else  // attacker
        {
            // In the last couple of turns, just head for enemy creatures,
            // not board edges.
            if (turn <= 5)
            {
                value += 3 * battle.getRange(hex, entrance, true);
            }
        }

        Log.debug("EVAL " + critter.getName() +
                (critter.hasMoved() ? " move to " : " stay in ") + hex +
                " = " + value);

        return value;
    }


    /** Fliers should move last, since they can fly over allies.
     *  Those moving far should move before those staying close to home.
     *  Non-natives should move before natives, since natives can often
     *     move through hexes that non-natives can't.
     *  More important creatures should move before less important
     *     creatures. */
    final class MoveOrderComparator implements Comparator
    {
        private Battle battle;

        public MoveOrderComparator(Battle battle)
        {
            this.battle = battle;
        }

        public int compare(Object o1, Object o2)
        {
            final char terrain = battle.getTerrain();
            final Legion legion = battle.getActiveLegion();

            ArrayList moveList1 = (ArrayList)o1;
            ArrayList moveList2 = (ArrayList)o2;
            CritterMove cm1 = (CritterMove)moveList1.get(0);
            CritterMove cm2 = (CritterMove)moveList2.get(0);
            Critter critter1 = cm1.getCritter();
            Critter critter2 = cm2.getCritter();
            BattleHex desiredHex1 = cm1.getEndingHex(terrain);
            BattleHex desiredHex2 = cm2.getEndingHex(terrain);

            if (critter1.isFlier() && !critter2.isFlier())
            {
                return 1;
            }
            else if (critter2.isFlier() && !critter1.isFlier())
            {
                return -1;
            }

            int range1 = battle.getRange(critter1.getStartingHex(),
                desiredHex1, true);
            int range2 = battle.getRange(critter2.getStartingHex(),
                desiredHex2, true);
            int diff = range2 - range1;
            if (diff != 0)
            {
                return diff;
            }

            if (MasterHex.isNativeCombatBonus(critter1, terrain) &&
                !MasterHex.isNativeCombatBonus(critter2, terrain))
            {
                return 1;
            }
            else if (MasterHex.isNativeCombatBonus(critter2, terrain) &&
                !MasterHex.isNativeCombatBonus(critter1, terrain))
            {
                return -1;
            }

            diff = getKillValue(critter2, terrain) -
                getKillValue(critter1, terrain);
            if (diff != 0)
            {
                return diff;
            }
            else
            {
                return critter1.getName().compareTo(critter2.getName());
            }
        }
    }

    /** Sort critters in decreasing order of importance.  Keep
     *  identical creatures together with a secondary sort by
     *  creature name. */
    final class CritterComparator implements Comparator
    {
        private char terrain;

        public CritterComparator(char terrain)
        {
            this.terrain = terrain;
        }

        public int compare(Object o1, Object o2)
        {
            Critter critter1 = (Critter)o1;
            Critter critter2 = (Critter)o2;
            int diff = getKillValue(critter2, terrain) -
                getKillValue(critter1, terrain);
            if (diff != 0)
            {
                return diff;
            }
            else
            {
                return critter1.getName().compareTo(critter2.getName());
            }
        }
    }


    class CritterMove implements Minimax.Move
    {
        private int value;
        private Critter critter;
        private String startingHexLabel;
        private String endingHexLabel;

        public CritterMove(Critter critter, String startingHexLabel,
            String endingHexLabel)
        {
            super();
            this.critter = critter;
            this.startingHexLabel = startingHexLabel;
            this.endingHexLabel = endingHexLabel;
        }

        public void setValue(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }

        public Critter getCritter()
        {
            return critter;
        }

        public String getStartingHexLabel()
        {
            return startingHexLabel;
        }

        public String getEndingHexLabel()
        {
            return endingHexLabel;
        }

        public BattleHex getStartingHex(char terrain)
        {
            return HexMap.getHexByLabel(terrain, startingHexLabel);
        }

        public BattleHex getEndingHex(char terrain)
        {
            return HexMap.getHexByLabel(terrain, endingHexLabel);
        }
    }
}
