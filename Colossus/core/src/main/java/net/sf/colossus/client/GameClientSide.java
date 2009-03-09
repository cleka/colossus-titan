package net.sf.colossus.client;


import net.sf.colossus.game.Game;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.Variant;


public class GameClientSide extends Game
{
    //    private static final Logger LOGGER = Logger.getLogger(GameClientSide.class
    //        .getName());

    private Client client;

    public GameClientSide(Variant variant, String[] playerNames)
    {
        super(variant, playerNames);
    }

    public void setClient(Client client)
    {
        this.client = client;
    }

    // TODO: move method from Client to here, or even to game.Game?
    @Override
    public Legion getLegionByMarkerId(String markerId)
    {
        return client.getLegion(markerId);
    }

}
