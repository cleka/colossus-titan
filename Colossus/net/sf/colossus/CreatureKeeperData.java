package net.sf.colossus;


/** Creature Keeper stacks
 * <P><B>TODO:</B>remove overlap with existing colossus
 *  @version $Id$
 *  @author Tom Fruchterman
 */
public class CreatureKeeperData 
	extends CreatureCollection 
	implements java.io.Serializable, ICreatureKeeperData
{
	public CreatureKeeperData()
		{
			super("Keeper's Stacks");
		}
    public void resetAllCounts()
    {
        setCount(CharacterArchetype.strANGEL, 18);
        setCount(CharacterArchetype.strARCH_ANGEL, 6);
        setCount(CharacterArchetype.strBEHEMOTH, 18);
        setCount(CharacterArchetype.strCENTAUR, 25);
        setCount(CharacterArchetype.strCOLUSSUS, 10);
        setCount(CharacterArchetype.strCYCLOPS, 28);
        setCount(CharacterArchetype.strDRAGON, 18);
        setCount(CharacterArchetype.strGARGOYLE, 21);
        setCount(CharacterArchetype.strGIANT, 18);
        setCount(CharacterArchetype.strGORGON, 25);
        setCount(CharacterArchetype.strGRIFFON, 18);
        setCount(CharacterArchetype.strGUARDIAN, 6);
        setCount(CharacterArchetype.strHYDRA, 10);
        setCount(CharacterArchetype.strLION, 28);
        setCount(CharacterArchetype.strMINOTAUR, 21);
        setCount(CharacterArchetype.strOGRE, 25);
        setCount(CharacterArchetype.strRANGER, 28);
        setCount(CharacterArchetype.strSERPENT, 10);
        setCount(CharacterArchetype.strTITAN, 6);
        setCount(CharacterArchetype.strTROLL, 28);
        setCount(CharacterArchetype.strUNICORN, 12);
        setCount(CharacterArchetype.strWARBEAR, 21);
        setCount(CharacterArchetype.strWARLOCK, 6);
        setCount(CharacterArchetype.strWYVERN, 18);
    }

/*
	public Character getCharacter(IPlayer oPlayer, String strCharacterName)
		{
			int nCount = getCount(strCharacterName);
			if(nCount > 0)
			{
				nCount--;
				setCount(strCharacterName, nCount);
				return (Character) CharacterArchetype.getCharacterFromName(strCharacterName).clone();
			}
			return null;
		}
*/

	public void returnCharacter(String strCharacterName)
		{
			int nCount = getCount(strCharacterName);
			nCount++;
			setCount(strCharacterName, nCount);
		}

}
