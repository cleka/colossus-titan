
/**
 * interface to allow for multiple AI implementations
 * 
 * @version $Id$
 */
public interface AI
{
    /** make (masterboard) moves for current player in the Game */
    public void move(Game game);

    /** make splits for current player */
    public void split(Game game);

    /** make recruits for current player */
    public void muster(Game game);

    /** pick one reinforcement */
    public Creature reinforce(Legion legion, Game game);
    
    /** choose whether legion should flee from enemy */
    public boolean flee(Legion legion, Legion enemy, Game game);
    
    /** choose whether legion should concede to enemy */
    public boolean concede(Legion legion, Legion enemy, Game game);

    /** make strikes battle for legion */
    public void strike(Legion legion, Battle battle, Game game);
    
    /** make strikes battle for legion */
    public boolean chooseStrikePenalty(Critter critter, Critter target,
        Critter carryTarget, Battle battle, Game game);
}
