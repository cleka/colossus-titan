package net.sf.colossus;

import java.util.Hashtable;

/** Base class for Graveyard and CreatureKeeper
 * <P><B>TODO:</B>remove overlap with existing colossus
 *  @version $Id$
 *  @author Tom Fruchterman
 */
class CreatureCollection implements ICreatureCollection
{
    private Hashtable m_oCharacterCounts = new Hashtable();
    private String m_strName;

    CreatureCollection(String strName)
	{
	    m_strName = strName;
	}

    public String getName() { return m_strName; }

    public void setCount(String strCharacterName, int nCount)
	{
	    m_oCharacterCounts.put(strCharacterName, new Integer(nCount));
	}
    public int getCount(String strCharacterName)
	{
	    int nCount = 0;
	    if(m_oCharacterCounts.containsKey(strCharacterName))
	    {
		nCount = ((Integer)m_oCharacterCounts.get(strCharacterName)).intValue();
	    }
	    return nCount;
	}
	
}
