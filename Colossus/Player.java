/**
 * Class Player holds the data for one player in a Titan game.
 * @version $Id$
 * author David Ripton
 */

class Player
{
    String name;
    String color;       // Bk, Bl, Br, Gd, Gr, Rd 
    int startingTower;  // 1-6
    int score;
    int angels;         // number of angels + archangels in legions
    boolean canSummonAngel; 
    String playersEliminated;    // BkBlRdGd
    String [] markersAvailable = new String[12];
    Legion [] legions;

    Player(String name, String color, int startingTower)
    {
        this.name = name;
        this.color = color;
        this.startingTower = startingTower;
        score = 0;
        angels = 1; 
        canSummonAngel = true;
        for (int i = 1; i <= 9; i++)
        {
            markersAvailable[i] = color + '0' + Integer.toString(i);
        }
        for (int i = 10; i <= 12; i++)
        {
            markersAvailable[i] = color + Integer.toString(i);
        }
    }

    boolean canTitanTeleport()
    {
        return (score >= 400);
    }


    int titanPower()
    {
        return 6 + (score / 100);
    }
}
