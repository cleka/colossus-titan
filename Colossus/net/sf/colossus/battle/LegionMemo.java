
package net.sf.colossus.battle;

import java.io.*;

/**
 * Stores everything necessary to recreate a legion
 */
public class LegionMemo implements Serializable
{
    private String m_strMarkerId;
    private String m_strParentMarkerId;
    private String m_strCurrentHexLabel;
    private String m_strStartingHexLabel;
    private String[] m_strCritters;
    private String m_strPlayerName;

    public LegionMemo(
        String strMarkerId,
        String strParentMarkerId,
        String strCurrentHexLabel,
        String strStartingHexLabel,
        String[] strCritters,
        String strPlayerName)
    {
        m_strMarkerId = strMarkerId;
        m_strParentMarkerId = strParentMarkerId;
        m_strCurrentHexLabel = strCurrentHexLabel;
        m_strStartingHexLabel = strStartingHexLabel;
        m_strCritters = strCritters;
        m_strPlayerName = strPlayerName;
    }

/**
 * Get the value of m_strMarkerId.
 * @return value of m_strMarkerId.
 */
    public String getMarkerId() 
    {
        return m_strMarkerId;
    }

    public String getColor()
    {
        return m_strMarkerId.substring(0, 2);
    }
    
/**
 * Get the value of m_strParentMarkerId.
 * @return value of m_strParentMarkerId.
 */
    public String getParentMarkerId() 
    {
        return m_strParentMarkerId;
    }
    
/**
 * Get the value of m_strCurrentHexLabel.
 * @return value of m_strCurrentHexLabel.
 */
    public String getCurrentHexLabel() 
    {
        return m_strCurrentHexLabel;
    }

    
/**
 * Get the value of m_strStartingHexLabel.
 * @return value of m_strStartingHexLabel.
 */
    public String getStartingHexLabel() 
    {
        return m_strStartingHexLabel;
    }

/**
 * Get the nth (0-indexed) critter name or null if we don't have that many
 * @return nth critter name or null
 */
    public String getCritter(int n)
    {
        return (m_strCritters.length >= (n + 1)) ? m_strCritters[n] : null;
    }


/**
 * Get the nth (0-indexed) critter name or null if we don't have that many
 * @return nth critter name or null
 */
    public String[] getCritters()
    {
        return m_strCritters;
    }



/**
 */
    public int getCritterCount()
    {
        return m_strCritters.length;
    }

    
/**
 * Get the value of m_strPlayerName.
 * @return value of m_strPlayerName.
 */
    public String getPlayerName() 
   {
        return m_strPlayerName;
    }


}

