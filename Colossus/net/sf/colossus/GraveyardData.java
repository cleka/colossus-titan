package net.sf.colossus;


/** Graveyard stacks
 * <P><B>TODO:</B>remove overlap with existing colossus
 *  @version $Id$
 *  @author Tom Fruchterman
 */
public class GraveyardData
    extends CreatureCollection
    implements java.io.Serializable, IGraveyardData
{
    public GraveyardData()
	{
	    super("Graveyard");
	}


    public void retireCharacter(String strCharacterName)
	{
	    int nCount = getCount(strCharacterName);
	    nCount++;
	    setCount(strCharacterName, nCount);
	}
}
