
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
    
    /** choose whether legion should flee from enemy */
    public boolean flee(Legion legion, Legion enemy);
}
