import java.util.*;

/**
 *  Generic Minimax with alpha-beta pruning
 *  also supports averaging for games with randomness
 *
 *  @version $Id$
 *  @author Bruce Sherrod
 */
class Minimax
{ 
    private final static int INFINITY = Integer.MAX_VALUE;
    public static final int MAXIMIZE = 0;
    public static final int MINIMIZE = 1;
    public static final int AVERAGE  = 2;

    /**
     * A game position in the game to be searched.
     */
    public interface GamePosition
    {
	/** 
	 * determine how we should evaluate this position.
	 * In a typical 2-player game, this should alternate with 
	 * MAXIMIZE or MINIMIZE depending on who's turn it is.
	 * For games with a random element, AVERAGE can be used.
	 * For simple puzzle solving (e.g. 8-puzzle), return always MAXIMIZE.
	 * @return MAXIMIZE, MINIMIZE, or AVERAGE
	 */
	int maximize();

	/**
	 *  evaluation function 
	 * @return value of this GamePosition; where positive is better for
	 * the player who is doing the search()
	 */
	int evaluation();

	/** plausible move generator
	 * may be lazy; also, may implement forward pruning.
	 * @return an iterator which returns Move's, in the order
	 * that they should be evaluated by Minimax.  The moves should 
	 * extend Move, and be handled by applyMove().
	 */
	Iterator generateMoves();

	/** 
	 * advance game state ( this should adjust maximize() as necessary ) 
	 */
	GamePosition applyMove(Move move);
    }

    /**
     * a move in the game to be searched.  Implementors of this
     * interface should add whatever info they need in order to store
     * a move.
     */
    public interface Move
    {
	//
	// convenience methods so that Minimax can return a single
	// object that contains a move and a score for that move.
	//  
	void setValue(int value);
	int getValue(); 
    }

    private class FinalMove implements Move
    {
	private int value;
	public void setValue(int value) { this.value = value; }
	public int getValue() { return value; }
    }

    public Move search(GamePosition position, int maxDepth)
    {
	if (maxDepth == 0) 
	    throw new RuntimeException("depth must be > 0"); 
	return search(position, 0, maxDepth, INFINITY, -INFINITY);
    }
  
    /*package*/ Move search(GamePosition position,
			     int depth,  // current depth (in ply)
			     int maxDepth, // max depth of this search (in ply)
			     int alpha, // alpha
			     int beta) // beta
    {
	//System.out.println("search d " + depth 
			   //+ " / " + maxDepth
			   //+ " alpha " + alpha
			   //+ " beta " + beta );

	int best_score;
	Move best_move = null;
	int maximize = position.maximize();

	if (depth >= maxDepth) 
	{
	    best_score = position.evaluation();
	    best_move = new FinalMove();
	    //System.out.println(" returning score " + best_score);
	}
	else 
	{
	    Iterator moves_list = position.generateMoves();
	    switch (maximize)
	    {
		case MAXIMIZE: best_score = -INFINITY; break;
		case MINIMIZE: best_score = INFINITY; break;
		case AVERAGE: best_score = 0;
		default:
		    throw new RuntimeException("unknown value of maximize " + maximize);
	    }
	    int numMovesExamined  = 0;
	    move_loop: while (moves_list.hasNext())
	    {
		numMovesExamined++;

		Move current_move = (Move) moves_list.next();
		GamePosition newPosition = position.applyMove(current_move);
		Move next_move = search(newPosition, 
					depth+1, 
					maxDepth, 
					alpha, 
					beta);
		int current_score = next_move.getValue();
	
		switch (maximize)
		{
		    case MAXIMIZE:
			{
			    if (current_score > best_score) 
			    {
				best_move = current_move;
				best_score = current_score;
				if (best_score > beta) 
				{
				    if (best_score >= alpha) 
					break move_loop;  /*  alpha_beta cutoff  */
				    else
					beta = best_score;  //current_score
				}
			    }
			}
			break;
		    case MINIMIZE:
			{
			    if (current_score < best_score) 
			    {
				best_move = current_move;
				best_score = current_score;
				if (best_score < alpha) 
				{
				    if (best_score <= beta) 
					break move_loop;  /*  alpha_beta cutoff  */
				    else
					alpha = best_score;  //current_score
				}
			    }
			} 
			break;
		    case AVERAGE:
			{
			    best_score += current_score;
			}
			break;
		}
	    }
	    if (numMovesExamined  == 0)
	    {
		// no legal moves from here, so terminate
		best_score = position.evaluation();
		best_move = new FinalMove();
	    }
	    else if (maximize == AVERAGE)
	    {
		best_score /= numMovesExamined;
		best_move = new FinalMove();
	    }
	}

	best_move.setValue(best_score);
	return best_move;
    } 
  

    ////////////////////////////////////////////////////////////
    // test harness
    ////////////////////////////////////////////////////////////

    private class TestMove implements Move
    {
	private int value;
	private String move;
	public TestMove(String move)
	{
	    this.move = move;
	}
	public void setValue(int value) { this.value = value; }
	public int getValue() { return value; }
    }

    private class TestGame implements GamePosition
    {
	String position;
	int turn = 0;
	public int maximize() 
	{
	    if (turn % 2 == 0)
		return Minimax.MAXIMIZE;
	    else
		return Minimax.MINIMIZE;
	}
	public int evaluation()
	{
	    //System.out.println("called evaluation on " + position);
	    if ("11".equals(position)) return 8;
	    if ("12".equals(position)) return 5;
	    if ("21".equals(position)) return 3;
	    if ("22".equals(position)) return 9;
	    throw new RuntimeException("shouldn't get here if a-b pruning works");
	}
	public Iterator generateMoves()
	{
	    ArrayList list = new ArrayList();
	    list.add(new TestMove("1"));
	    list.add(new TestMove("2"));
	    return list.iterator();
	} 
	public GamePosition applyMove(Move move)
	{
	    TestGame newPosition = new TestGame();
	    newPosition.position = position + ((TestMove)move).move;
	    newPosition.turn = turn ++;
	    return newPosition;
	}
    }

    public void test()
    {
	TestGame game = new TestGame();
	game.position ="";
	TestMove move = (TestMove) search(game,2);
	System.out.println("Final result is to make move " + move.move 
			   + " with value " + move.getValue());
    }

    public static void main(String[] args)
    { 
	Minimax minimax = new Minimax();
	minimax.test();
    }

}
