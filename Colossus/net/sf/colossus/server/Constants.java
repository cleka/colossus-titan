package net.sf.colossus.server;


import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;


/**
 * Class Constants just holds constants.
 * @version $Id$
 * @author David Ripton
 */

public final class Constants
{
    // Constants for phases of a turn.
    public static final int SPLIT = 1;
    public static final int MOVE = 2;
    public static final int FIGHT = 3;
    public static final int MUSTER = 4;



    /** Base path for all external game data files. */
    public static final String gameDataPath = 
        System.getProperty("user.home") + "/Colossus/";

    // Constants related to the options config files
    public static final String optionsPath = gameDataPath;
    public static final String optionsBase = "Colossus-";
    public static final String optionsServerName = "server";
    public static final String optionsExtension = ".cfg";

    public static final String configVersion =
        "Colossus config file version 2";



    // Constants for savegames

    /** Must include trailing slash. */
    public static final String saveDirname = gameDataPath + "/saves/";
    public static final String saveExtension = ".sav";
    public static final String saveGameVersion =
        "Colossus savegame version 11";

    // Phases of a battle turn
    public static final int SUMMON = 0;
    public static final int RECRUIT = 1;
    //public static final int MOVE = 2;
    //public static final int FIGHT = 3;
    public static final int STRIKEBACK = 4;

    public static final int BIGNUM = 99;
    public static final int OUT_OF_RANGE = 5;

    /** Fake striker id for drift and other hex damage. */
    public static final int hexDamage = -1;

    // Angel-summoning states
    public static final int NO_KILLS = 0;
    public static final int FIRST_BLOOD = 1;
    public static final int TOO_LATE = 2;

    // Legion tags
    public static final int DEFENDER = 0;
    public static final int ATTACKER = 1;

    // Constants for hexside gates.
    public static final int NONE = 0;
    public static final int BLOCK = 1;
    public static final int ARCH = 2;
    public static final int ARROW = 3;
    public static final int ARROWS = 4;

    public static final int ARCHES_AND_ARROWS = -1;
    public static final int ARROWS_ONLY = -2;
    public static final int NOWHERE = -1;

    // MasterBoard size
    public static final int MIN_HORIZ_SIZE=15;
    public static final int MIN_VERT_SIZE=8;
    public static final int MAX_HORIZ_SIZE=60;
    public static final int MAX_VERT_SIZE=32;

    /* the three arrays below must match ; see also
       net.sf.colossus.server.Legion, it uses the
       shortened name directly */
    /* all should be MAX_MAX_PLAYERS long */
    public static final String [] colorNames =
        {"Black",  "Blue",   "Brown", "Gold", "Green", "Red",
         "Orange", "Purple", "Silver"};
    public static final String [] shortColorNames = 
        {"Bk",     "Bu",     "Br",    "Gd",   "Gr",    "Rd" ,
         "Or",     "Pu",     "Si"};
    public static final int [] colorMnemonics = {
        KeyEvent.VK_B, KeyEvent.VK_L, KeyEvent.VK_O,
        KeyEvent.VK_G, KeyEvent.VK_E, KeyEvent.VK_R,
        KeyEvent.VK_A, KeyEvent.VK_P, KeyEvent.VK_S};

    public static final String noShortName = "XX";
    private static final java.util.HashMap shortNamesMap = new HashMap();
    
    public static final int MIN_AI_DELAY = 0;      //in ms
    public static final int MAX_AI_DELAY = 3000;
    public static final int DEFAULT_AI_DELAY = 300;

    public static final int MIN_AI_TIME_LIMIT = 1;      //in s
    public static final int MAX_AI_TIME_LIMIT = 200;
    public static final int DEFAULT_AI_TIME_LIMIT = 30;

    // Entry sides
    public static final String bottom = "Bottom";
    public static final String right = "Right";
    public static final String left = "Left";

    /** Default directory for datafiles */
    public static final String defaultDirName = "Default";
    /** Images subdirectory name */
    public static final String imagesDirName = "images";
    /** Battlelands subdirectory name */
    public static final String battlelandsDirName = "Battlelands";
    /** Default CRE file */
    public static final String defaultCREFile = "Default.cre";
    /** Default MAP file */
    public static final String defaultMAPFile = "Default.map";
    /** Default TER file */
    public static final String defaultTERFile = "Default.ter";
    /** Default VAR file */
    public static final String defaultVARFile = "Default.var";

    /* icon setup */
    public static final String masterboardIconImage = "Colossus";
    public static final String masterboardIconText = "Colossus";
    public static final String masterboardIconTextColor = "black";
    public static final String masterboardIconSubscript = "Main";
    public static final String battlemapIconImage = "Colossus";
    public static final String battlemapIconText = "Colossus";
    public static final String battlemapIconTextColor = "black";
    public static final String battlemapIconSubscript = "Battle";

    public static final int DEFAULT_MAX_PLAYERS = 6;
    /* number of available colours/markers */
    public static final int MAX_MAX_PLAYERS = 9;

    // Player types
    public static final String human = "Human";
    public static final String network = "Network";
    public static final String none = "None";
    public static final String observer = "Observer";
    public static final String ai = "AI";
    public static final String anyAI = "A Random AI";
    public static final String defaultAI = anyAI;
    public static final String[] aiArray = { "SimpleAI", "CowardSimpleAI" };
    public static final int numAITypes = aiArray.length;
    public static final String all = "All";
    public static final String aiPackage = "net.sf.colossus.client.";

    // Player names
    public static final String byColor = "<By color>";
    public static final String byClient = "<By client>";
    public static final String username = System.getProperty("user.name",
        byColor);

    public static final String titan = "Titan";
    public static final String angel = "Angel";

    // Network stuff
    public static final int defaultPort = 1969;
    public static final String localhost = "localhost";

    // Game actions used in several places.
    public static final String newGame = "New game";
    public static final String loadGame = "Load game";
    public static final String saveGame = "Save game";
    public static final String quit = "Quit";
    public static final String runClient = "Run network client";


    /** Available internal variants  Try to keep this list mostly
     *  alphabetized for easier searching, with Default at the top. */
    public static final String [] variantArray =
    {
        "Default",
        "Badlands",
        "Badlands-JDG",
        "ExtTitan", 
        "Outlands",
        "SmallTitan",
        "TG-ConceptI",
        "TG-ConceptII",
        "TG-ConceptIII",
        "TG-SetII",
        "TG-SetIII",
        "TG-Wild",
        "TitanPlus",
        "Undead"
    };

    public static final int numVariants = variantArray.length;

    private static final java.util.List variantList = new ArrayList();

    // static initializer
    {
        for (int i = 0; i < variantArray.length; i++)
        {
            variantList.add(variantArray[i]);
        }
    }

    public static java.util.List getVariantList()
    {
        return Collections.unmodifiableList(variantList);
    }


    // Protocol packet type constants
    /** XXX If any of the args in the protocol contain this string, then
     *  the protocol will break.  Escape it for chat. */
    public static final String protocolTermSeparator=" ~ ";

    // From client to server
    public static final String signOn = "signOn";
    public static final String fixName = "fixName";

    public static final String leaveCarryMode = "leaveCarryMode";
    public static final String doneWithBattleMoves = "doneWithBattleMoves";
    public static final String doneWithStrikes = "doneWithStrikes";
    public static final String acquireAngel = "acquireAngel";
    public static final String doSummon = "doSummon";
    public static final String doRecruit = "doRecruit";
    public static final String engage = "engage";
    public static final String concede = "concede";
    public static final String doNotConcede = "doNotConcede";
    public static final String flee = "flee";
    public static final String doNotFlee = "doNotFlee";
    public static final String makeProposal = "makeProposal";
    public static final String fight = "fight";
    public static final String doBattleMove = "doBattleMove";
    public static final String strike = "strike";
    public static final String applyCarries = "applyCarries";
    public static final String undoBattleMove = "undoBattleMove";
    public static final String assignStrikePenalty = "assignStrikePenalty";
    public static final String mulligan = "mulligan";
    public static final String undoSplit = "undoSplit";
    public static final String undoMove = "undoMove";
    public static final String undoRecruit = "undoRecruit";
    public static final String doneWithSplits = "doneWithSplits";
    public static final String doneWithMoves = "doneWithMoves";
    public static final String doneWithEngagements = "doneWithEngagements";
    public static final String doneWithRecruits = "doneWithRecruits";
    public static final String withdrawFromGame = "withdrawFromGame";
    public static final String setDonor = "setDonor";
    public static final String doSplit = "doSplit";
    public static final String doMove = "doMove";
    public static final String assignColor = "assignColor";
    public static final String relayChatMessage = "relayChatMessage";
    //public static final String newGame = "newGame";
    //public static final String loadGame = "loadGame";
    //public static final String saveGame = "saveGame";

    // From server to client
    public static final String tellMovementRoll = "tellMovementRoll";
    public static final String setOption = "setOption";
    public static final String updatePlayerInfo = "updatePlayerInfo";
    public static final String setColor = "setColor";
    public static final String updateCreatureCount = "updateCreatureCount";
    public static final String dispose = "dispose";
    public static final String removeLegion = "removeLegion";
    public static final String setLegionHeight = "setLegionHeight";
    public static final String setLegionContents = "setLegionContents";
    public static final String addCreature = "addCreature";
    public static final String removeCreature = "removeCreature";
    public static final String revealCreatures = "revealCreatures";
    public static final String removeDeadBattleChits = "removeDeadBattleChits";
    public static final String placeNewChit = "placeNewChit";
    public static final String initBoard = "initBoard";
    public static final String setPlayerName = "setPlayerName";
    public static final String createSummonAngel = "createSummonAngel";
    public static final String askAcquireAngel = "askAcquireAngel";
    public static final String askChooseStrikePenalty = 
        "askChooseStrikePenalty";
    public static final String showMessageDialog = "showMessageDialog";
    public static final String tellGameOver = "tellGameOver";
    public static final String askConcede = "askConcede";
    public static final String askFlee = "askFlee";
    public static final String askNegotiate = "askNegotiate";
    public static final String tellProposal = "tellProposal";
    public static final String tellStrikeResults = "tellStrikeResults";
    public static final String initBattle = "initBattle";
    public static final String cleanupBattle = "cleanupBattle";
    public static final String highlightEngagements = "highlightEngagements";
    public static final String nextEngagement = "nextEngagement";
    public static final String doReinforce = "doReinforce";
    public static final String didRecruit = "didRecruit";
    public static final String undidRecruit = "undidRecruit";
    public static final String setupTurnState = "setupTurnState";
    public static final String setupSplit = "setupSplit";
    public static final String setupMove = "setupMove";
    public static final String setupFight = "setupFight";
    public static final String setupMuster = "setupMuster";
    public static final String setupBattleSummon = "setupBattleSummon";
    public static final String setupBattleRecruit = "setupBattleRecruit";
    public static final String setupBattleMove = "setupBattleMove";
    public static final String setupBattleFight = "setupBattleFight";
    public static final String tellLegionLocation = "tellLegionLocation";
    public static final String tellBattleMove = "tellBattleMove";
    public static final String didMove = "didMove";
    public static final String undidMove = "undidMove";
    public static final String undidSplit = "undidSplit";
    public static final String didSplit = "didSplit";
    public static final String askPickColor = "askPickColor";
    public static final String log = "log";
    public static final String showChatMessage = "showChatMessage";

    static
    {
        if (colorNames.length != shortColorNames.length)
        {
            System.err.println("ERROR: List of constants is wrong !");
            System.exit(1);
        }
        for (int i = 0; i < colorNames.length; i++)
        {
            shortNamesMap.put(colorNames[i], shortColorNames[i]);
        }
    }

    public static final String getPhaseName(int phase)
    {
        switch (phase)
        {
            case SPLIT:
                return "Split";
            case MOVE:
                return "Move";
            case FIGHT:
                return "Fight";
            case MUSTER:
                return "Muster";
            default:
                return "";
        }
    }

    public static String getBattlePhaseName(int phase)
    {
        switch (phase)
        {
            case Constants.SUMMON:
                return "Summon";
            case Constants.RECRUIT:
                return "Recruit";
            case Constants.MOVE:
                return "Move";
            case Constants.FIGHT:
                return "Fight";
            case Constants.STRIKEBACK:
                return "Strikeback";
            default:
                return "";
        }
    }

    public static String getShortColorName(String c)
    {
        String temp = (String)shortNamesMap.get(c);
        if (temp != null)
            return temp;
        else
            return noShortName;
    }
}
