package net.sf.colossus;


/** Interface for collections
 * <P><B>TODO:</B>remove overlap with existing colossus
 *  @version $Id$
 *  @author Tom Fruchterman
 */
public interface ICreatureCollection
{
    public String getName();
    public void setCount(String strCharacterName, int nCount);
    public int getCount(String strCharacterName);
}

