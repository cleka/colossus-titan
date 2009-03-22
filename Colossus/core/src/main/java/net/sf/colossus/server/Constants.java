package net.sf.colossus.server;


import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Class Constants just holds constants.
 * @version $Id$
 * @author David Ripton
 *
 * TODO some of this would probably have a decent place somewhere else, e.g.
 * the Phase and BattlePhase enums could be in the game package.
 */

public final class Constants
{
    // TODO this should probably start numbering with zero as does the BattlePhase and
    // as does any other Java enum -- currently SPLIT is serialized as "1"
    public static enum Phase
    {
        SPLIT("Split"), MOVE("Move"), FIGHT("Fight"), MUSTER("Muster");

        /**
         * Deserialize enum from integer value.
         *
         * @param i The number for the phase.
         * @return The matching Phase instance.
         *
         * @throws ArrayOutOfBoundsException iff the number is not valid.
         */
        public static Phase fromInt(int i)
        {
            return values()[i - 1];
        }

        /**
         * Serialize the object to an integer code.
         *
         * Used for savegames.
         *
         * @return An integer code representing the phase.
         */
        public int toInt()
        {
            return ordinal() + 1;
        }

        /**
         * Returns a non-localized UI string for the phase.
         */
        @Override
        public String toString()
        {
            return name;
        }

        private final String name;

        private Phase(String name)
        {
            this.name = name;
        }
    }

    public static enum BattlePhase
    {
        SUMMON("Summon", true, false), RECRUIT("Recruit", true, false), MOVE(
            "Move", true, false), FIGHT("Fight", false, true), STRIKEBACK(
            "Strikeback", false, true);

        /**
         * Determine if the phase is part of the fighting.
         *
         * @return true iff the phase is either FIGHT or STRIKEBACK.
         */
        public boolean isFightPhase()
        {
            return isFightPhase;
        }

        /**
         * Determine if the phase is part of the fighting.
         * Right now we consider also SUMMON or RECRUT as move phase
         * (currently they are own phases; I would like those actions
         *  to happen as part of the move phase instead, then this here
         *  can be changed).
         *
         * @return true iff the phase is a move phase;
         *
         */
        public boolean isMovePhase()
        {
            return isMovePhase;
        }

        /**
         * Returns a non-localized UI string for the phase.
         */
        @Override
        public String toString()
        {
            return name;
        }

        private final String name;
        private final boolean isMovePhase;
        private final boolean isFightPhase;

        private BattlePhase(String name, boolean isMovePhase,
            boolean isFightPhase)
        {
            this.name = name;
            this.isMovePhase = isMovePhase;
            this.isFightPhase = isFightPhase;
        }
    }

    // Special feature to end the game after one battle is completed,
    // for tuning the AI
    private static String END_AFTER_FIRST_BATTLE_PROPERTY = "net.sf.colossus.endAfterFirstBattle";
    public static final boolean END_AFTER_FIRST_BATTLE = Boolean.valueOf(
        System.getProperty(END_AFTER_FIRST_BATTLE_PROPERTY, "false")
            .toString()).booleanValue();

    // Special feature for stresstest: force at least one board
    private static String FORCE_VIEW_BOARD_PROPNAME = "net.sf.colossus.forceViewBoard";
    public static final boolean FORCE_VIEW_BOARD = Boolean.valueOf(
        System.getProperty(FORCE_VIEW_BOARD_PROPNAME, "false").toString())
        .booleanValue();

    /** Base path for all external game data files. */
    public static final String GAME_DATA_PATH = System
        .getProperty("user.home")
        + File.separator + ".colossus" + File.separator;

    // Constants related to the options config files
    public static final String OPTIONS_PATH = GAME_DATA_PATH;
    public static final String OPTIONS_BASE = "Colossus-";
    public static final String OPTIONS_SERVER_NAME = "server";
    public static final String OPTIONS_NET_CLIENT_NAME = "netclient";
    public static final String OPTIONS_WEB_CLIENT_NAME = "webclient";
    // virtual "name" for Options instance in GetPlayers (never saved)
    public static final String OPTIONS_START = "start";
    public static final String OPTIONS_EXTENSION = ".cfg";

    public static final String CONFIG_VERSION = "Colossus config file version 2";

    // Constants for savegames

    /** Must include trailing slash. */
    public static final String SAVE_DIR_NAME = GAME_DATA_PATH + File.separator
        + "saves" + File.separator;
    public static final String XML_EXTENSION = ".xml";
    public static final String XML_SNAPSHOT_START = "snap";
    public static final String XML_SNAPSHOT_VERSION = "12";

    public static final int BIGNUM = 99;
    public static final int OUT_OF_RANGE = 5;

    /** Fake striker id for drift and other hex damage. */
    public static final int HEX_DAMAGE = -1;

    public static enum HexsideGates
    {
        NONE, BLOCK, ARCH, ARROW, ARROWS
    }

    public static final int ARCHES_AND_ARROWS = -1;
    public static final int ARROWS_ONLY = -2;
    public static final int NOWHERE = -1;

    // MasterBoard size
    public static final int MIN_HORIZ_SIZE = 15;
    public static final int MIN_VERT_SIZE = 8;
    public static final int MAX_HORIZ_SIZE = 60;
    public static final int MAX_VERT_SIZE = 32;

    /* the three arrays below must match ; see also
     net.sf.colossus.server.Legion, it uses the
     shortened name directly */

    // TODO the colors themselves should be defined in here
    public static enum PlayerColor
    {
        BLACK("Black", "Bk", KeyEvent.VK_B), BLUE("Blue", "Bu", KeyEvent.VK_L), BROWN(
            "Brown", "Br", KeyEvent.VK_O), GOLD("Gold", "Gd", KeyEvent.VK_G), GREEN(
            "Green", "Gr", KeyEvent.VK_E), RED("Red", "Rd", KeyEvent.VK_R), ORANGE(
            "Orange", "Or", KeyEvent.VK_A), PURPLE("Purple", "Pu",
            KeyEvent.VK_P), SILVER("Silver", "Si", KeyEvent.VK_S), SKY("Sky",
            "Sk", KeyEvent.VK_K), PINE("Pine", "Pi", KeyEvent.VK_N), INDIGO(
            "Indigo", "In", KeyEvent.VK_I);

        private final String name;
        private final String shortName;
        private final int mnemonic;

        private PlayerColor(String name, String shortName, int mnemonic)
        {
            this.name = name;
            this.shortName = shortName;
            this.mnemonic = mnemonic;
        }

        public int getMnemonic()
        {
            return mnemonic;
        }

        public String getName()
        {
            return name;
        }

        public String getShortName()
        {
            return shortName;
        }

        public static PlayerColor getByName(String name)
        {
            for (PlayerColor color : values())
            {
                if (color.getName().equals(name))
                {
                    return color;
                }
            }
            return null; // seems to happen when game starts
        }

        public static List<PlayerColor> getByName(List<String> names)
        {
            List<PlayerColor> retVal = new ArrayList<PlayerColor>();
            for (String name : names)
            {
                retVal.add(getByName(name));
            }
            return retVal;
        }

        @Override
        public String toString()
        {
            return getName(); // important as long as some code might still expect the old strings
        }
    }

    public static final int MIN_AI_DELAY = 0; //in ms
    public static final int MAX_AI_DELAY = 3000;
    public static final int DEFAULT_AI_DELAY = 300;

    public static final int MIN_AI_TIME_LIMIT = 1; //in s
    public static final int MAX_AI_TIME_LIMIT = 200;
    public static final int DEFAULT_AI_TIME_LIMIT = 30;

    /**
     * The entry side for a battle.
     */
    public static enum EntrySide
    {
        TOP_DEFENSE("Top defense"), RIGHT("Right"), RIGHT_DEFENSE(
            "Right defense"), BOTTOM("Bottom"), LEFT_DEFENSE("Left defense"), LEFT(
            "Left"), NOT_SET("Not set");

        private final String label;

        private EntrySide(String label)
        {
            this.label = label;
        }

        public String getLabel()
        {
            return label;
        }

        public int getId()
        {
            // TODO: inline
            return ordinal();
        }

        public boolean isAttackerSide()
        {
            return ordinal() % 2 == 1;
        }

        public EntrySide getOpposingSide()
        {
            return values()[(ordinal() + 3) % 6];
        }

        public static EntrySide fromLabel(String label)
        {
            for (EntrySide entrySide : values())
            {
                // the old code relied on the side being one of the attacker sides,
                // so we keep the restriction
                if (entrySide.isAttackerSide()
                    && entrySide.getLabel().equals(label))
                {
                    return entrySide;
                }
            }
            throw new IllegalArgumentException(
                "No attacker entry side with label '" + label + "'");
        }

        public static EntrySide fromIntegerId(int id)
        {
            for (EntrySide entrySide : values())
            {
                // the old code relied on the side being one of the attacker sides, 
                // so we keep the restriction
                // Clemens: I didn't find anything which restricts the entry sides
                // to be attackers only. Defender is set and stored also, and never
                // re-set/cleared, so there will be defender sides remaining.
                //  ==> letting everything trough as long as it is a valid value,
                // including NOT_SET.
                if (entrySide.getId() == id)
                {
                    return entrySide;
                    /*                    
                    if (entrySide.isAttackerSide())
                    {
                        return entrySide;
                    }
                    else if (entrySide == EntrySide.NOT_SET)
                    {
                        return entrySide;
                    }
                    else
                    {
                        return entrySide;
                    }
                    */
                }
            }
            throw new IllegalArgumentException("No entry side with id " + id);
        }
    }

    /** all variants are subdirectories of this dir.
     /*  the default dir name can is not prepended by this. */
    public static final String varPath = "";

    /** Default directory for datafiles, can be outside variants,
     * but should be there.
     */
    //public static final String defaultDirName = "variants/Default";
    public static final String defaultDirName = "Default";
    public static final String defaultVarName = "Default";

    /** Images subdirectory name */
    public static final String imagesDirName = "images";

    /** Battlelands subdirectory name */
    public static final String battlelandsDirName = "Battlelands";

    /** Default CRE file */
    public static final String defaultCREFile = "DefaultCre.xml";

    /** Default MAP file */
    public static final String defaultMAPFile = "DefaultMap.xml";

    /** Default TER file */
    public static final String defaultTERFile = "DefaultTer.xml";

    /** Default HINT file */
    public static final String defaultHINTFile = "DefaultHint";

    // AI types for hints
    public static final String sectionAllAI = "AllAI:";
    public static final String sectionOffensiveAI = "OffensiveAI:";
    public static final String sectionDefensiveAI = "DefensiveAI:";

    /** Default VAR file */
    public static final String defaultVARFile = "DefaultVar.xml";
    public static final String varEnd = "Var.xml";

    /** markers name are mapped in this one */
    public static final String markersNameFile = "MarkersName";

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

    /* number of available colors/markers */
    public static final int MAX_MAX_PLAYERS = 12;

    // Player types
    public static final String human = "Human";
    public static final String network = "Network";
    public static final String none = "None";
    public static final String observer = "Observer";
    public static final String ai = "AI";
    public static final String anyAI = "A Random AI";
    public static final String defaultAI = anyAI;
    public static final String[] aiArray = { "SimpleAI", "CowardSimpleAI",
        "RationalAI", "HumanHaterRationalAI", "MilvangAI", "ExperimentalAI" };

    public static final int numAITypes = aiArray.length;
    public static final String all = "All";
    public static final String aiPackage = "net.sf.colossus.ai.";
    public static final String oldAiPackage = "net.sf.colossus.client.";

    // Player names
    public static final String byColor = "<By color>";
    public static final String byType = "<By type>";
    public static final String byClient = "<By client>";
    public static final String username = System.getProperty("user.name",
        byColor);

    public static final String titan = "Titan";
    public static final String angel = "Angel";

    // Network stuff
    public static final int defaultPort = 26567;

    // Web clients:
    public static final String defaultWebServer = "localhost";
    public static final int defaultWebPort = 26766;
    public static final String defaultWebLogin = System.getProperty(
        "user.name", "webuser");
    public static final String defaultWebPassword = "webpassword";

    public static final int numSavedServerNames = 10;

    // Game actions used in several places.
    public static final String newGame = "New game";
    public static final String loadGame = "Load game";
    public static final String saveGame = "Save game";
    public static final String quitGame = "Quit Game";
    public static final String closeBoard = "Close MasterBoard";
    public static final String checkConnection = "Check connection";

    public static final String runClient = "Run network client";
    public static final String runWebClient = "Run web client";

    // Used as prompt, and as the string for "strike penalty" send to server
    // for cancelling the strike.
    public static final String cancelStrike = "Cancel";

    /** Available internal variants  Try to keep this list mostly
     *  alphabetized for easier searching, with Default at the top. */
    public static final String[] variantArray = { "Default", "Abyssal3",
        "Abyssal6", "Abyssal9", "Badlands", "Badlands-JDG", "Balrog",
        "Beelzebub", "Beelzebub12", "BeelzeGods12", "ExtTitan", "Infinite",
        "Outlands", "Pantheon", "SmallTitan", "TG-ConceptI", "TG-ConceptII",
        "TG-ConceptIII", "TG-SetII", "TG-SetIII", "TG-Wild", "TitanPlus",
        "Undead", "Unified" };

    public static final int numVariants = variantArray.length;

    private static final List<String> variantList = Arrays
        .asList(variantArray);

    public static List<String> getVariantList()
    {
        return Collections.unmodifiableList(variantList);
    }

    // Protocol packet type constants
    /** XXX If any of the args in the protocol contain this string, then
     *  the protocol will break. */
    public static final String protocolTermSeparator = " ~ ";
    public static final String fileServerIgnoreFailSignal = "~/~Ignore-Fail~/~";

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
    public static final String disconnect = "disconnect";
    public static final String stopGame = "stopGame";
    public static final String doSplit = "doSplit";
    public static final String doMove = "doMove";
    public static final String assignColor = "assignColor";
    public static final String assignFirstMarker = "assignFirstMarker";
    public static final String askPickFirstMarker = "askPickFirstMarker";
    //public static final String newGame = "newGame";
    //public static final String loadGame = "loadGame";
    //public static final String saveGame = "saveGame";
    public static final String processedCtr = "processedCtr";
    public static final String catchupConfirmation = "catchupConfirmation";
    public static final String serverConnectionOK = "serverConnectionOK";

    public static final String reasonSplit = "Split";
    public static final String reasonSummon = "Summon";
    public static final String reasonUndoSummon = "UndoSummon";
    public static final String reasonAcquire = "Acquire";
    public static final String reasonRecruiter = "Recruiter";
    public static final String reasonRecruited = "Recruited";
    public static final String reasonReinforced = "Reinforced";
    public static final String reasonUndidRecruit = "UndidRecruit";
    public static final String reasonUndidReinforce = "UndidReinforce";
    public static final String reasonNegotiated = "Negotiated";
    public static final String reasonTeleport = "Teleport";
    public static final String reasonInitial = "Initial";
    public static final String reasonGameOver = "GameOver";
    public static final String reasonWinner = "Winner";
    public static final String reasonBattleStarts = "BattleStarts";
    public static final String reasonEngaged = "Engaged";
    public static final String reasonConcession = "Concession";
    public static final String reasonFled = "Fled";

    // From server to client
    public static final String tellEngagement = "tellEngagement";
    public static final String tellEngagementResults = "tellEngagementResults";
    public static final String tellMovementRoll = "tellMovementRoll";
    public static final String tellWhatsHappening = "tellWhatsHappening";
    public static final String setOption = "setOption";
    public static final String updatePlayerInfo = "updatePlayerInfo";
    public static final String setColor = "setColor";
    public static final String updateCreatureCount = "updateCreatureCount";
    public static final String dispose = "dispose";
    public static final String removeLegion = "removeLegion";
    public static final String setLegionStatus = "setLegionStatus";
    public static final String addCreature = "addCreature";
    public static final String removeCreature = "removeCreature";
    public static final String revealCreatures = "revealCreatures";
    public static final String revealEngagedCreatures = "revealEngagedCreatures";
    public static final String removeDeadBattleChits = "removeDeadBattleChits";
    public static final String placeNewChit = "placeNewChit";
    public static final String replayOngoing = "replayOngoing";
    public static final String initBoard = "initBoard";
    public static final String setPlayerName = "setPlayerName";
    public static final String createSummonAngel = "createSummonAngel";
    public static final String askAcquireAngel = "askAcquireAngel";
    public static final String askChooseStrikePenalty = "askChooseStrikePenalty";
    public static final String tellGameOver = "tellGameOver";
    public static final String tellPlayerElim = "tellPlayerElim";
    public static final String askConcede = "askConcede";
    public static final String askFlee = "askFlee";
    public static final String askNegotiate = "askNegotiate";
    public static final String tellProposal = "tellProposal";
    public static final String tellStrikeResults = "tellStrikeResults";
    public static final String initBattle = "initBattle";
    public static final String cleanupBattle = "cleanupBattle";
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
    public static final String didSummon = "didSummon";
    public static final String undidMove = "undidMove";
    public static final String undidSplit = "undidSplit";
    public static final String didSplit = "didSplit";
    public static final String askPickColor = "askPickColor";
    public static final String log = "log";
    public static final String nak = "nak";
    public static final String boardActive = "boardActive";
    public static final String askConfirmCatchUp = "askConfirmCatchUp";

    // engagement resolved, methods:
    public static final String erMethodFlee = "flee";
    public static final String erMethodConcede = "concede";
    public static final String erMethodFight = "fight";
    public static final String erMethodTimeLoss = "timeloss";
    public static final String erMethodNegotiate = "negotiate";

}
