/**
 * Class Player holds the data for one player in a Titan game.
 * @version $Id$
 * author David Ripton
 */

class Player
{
    String name;
    String color;       // black, blue, brown, gold, green, red
    int startingTower;  // 1-6 
    int score;
    int titanPower;
    boolean canTitanTeleport;
    int angels;         // number of angels + archangels in legions
    boolean canSummonAngel; 


    Player(String inName, String inColor, int inStartingTower)
    {
        name = inName;
        color = inColor;
        startingTower = inStartingTower;
        score = 0;
        titanPower = 6;
        canTitanTeleport = false;
        angels = 1; 
        canSummonAngel = true;
    }

}
