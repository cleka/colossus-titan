package net.sf.colossus;

import java.util.List;
import java.util.Arrays;
import java.util.Iterator;

/** Constants associated with character types
 *
 *
 * <P><B>TODO:</B>remove overlap with existing colossus
 * <P><B>TODO:</B>rename to CreatureConstants, which would be a better
 * name
 *
 *  @version $Id$
 *  @author Tom Fruchterman */
public class CharacterArchetype
{
    public static final String strANGEL = "Angel";
    public static final String strARCH_ANGEL = "Archangel";
    public static final String strBEHEMOTH = "Behemoth";
    public static final String strCENTAUR = "Centaur";
    public static final String strCOLUSSUS = "Colossus";
    public static final String strCYCLOPS = "Cyclops";
    public static final String strDRAGON = "Dragon";
    public static final String strGARGOYLE = "Gargoyle";
    public static final String strGIANT = "Giant";
    public static final String strGORGON = "Gorgon";
    public static final String strGRIFFON = "Griffon";
    public static final String strGUARDIAN = "Guardian";
    public static final String strHYDRA = "Hydra";
    public static final String strLION = "Lion";
    public static final String strMINOTAUR = "Minotaur";
    public static final String strOGRE = "Ogre";
    public static final String strRANGER = "Ranger";
    public static final String strSERPENT = "Serpent";
    public static final String strTITAN = "Titan";
    public static final String strTROLL = "Troll";
    public static final String strUNICORN = "Unicorn";
    public static final String strWARBEAR = "Warbear";
    public static final String strWARLOCK = "Warlock";
    public static final String strWYVERN = "Wyvern";
    public static final String strUNKNOWN = "Unknown";

    private static final String [] strCharactersArray = {
	strANGEL,
	strARCH_ANGEL,
	strBEHEMOTH,
	strCENTAUR,
	strCOLUSSUS,
	strCYCLOPS,
	strDRAGON,
	strGARGOYLE,
	strGIANT,
	strGORGON,
	strGRIFFON,
	strGUARDIAN,
	strHYDRA,
	strLION,
	strMINOTAUR,
	strOGRE,
	strRANGER,
	strSERPENT,
	strTITAN,
	strTROLL,
	strUNICORN,
	strWARBEAR,
	strWARLOCK,
	strWYVERN,
    };

    private static final List characters = Arrays.asList(strCharactersArray);

    /**
     * Used by CreatureCollectionView
     */
    public static List getCharacters()
	{
	    return characters;
	}

    public static String[] getCharactersArray()
	{
	    return strCharactersArray;
	}


    /** We are borrowing this new object to help re-implemnent the old
     * Caretaker object in the main code. So this isn't the real
     * caretakers stack, we are just borrowing it because it has the
     * maximum character counts */
    private static CreatureKeeperData s_oCreatureKeeperData = 
    new CreatureKeeperData();
    static
	{
	    // Initialize the counts. Since we never change them they
	    // will always be the maximum
	    s_oCreatureKeeperData.resetAllCounts();
	}

    /** For the Caretaker */
    public static int getMaxCount(String strCreatureName)
	{
	    return s_oCreatureKeeperData.getCount(strCreatureName);
	}
}
