import java.io.*;
import java.util.*;

/**
 * MiniMax implementation of a Titan AI
 * @version $Id$ 
 * @author Bruce Sherrod
 * @author David Ripton
 */

// XXX This class is currently somewhat broken:  
// Only one legion moves per player.
// MovementDie and the title bar do not update correctly.

// XXX Need to extract constants. 

class MinimaxAI extends SimpleAI implements AI
{
    private Minimax minimax = new Minimax();


    public void masterMove(Game game)
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
}
