
package net.sf.colossus.battle;

import java.io.*;

/**
 * Stores everything necessary to recreate a battle
 */
public class BattleMemo implements Serializable
{
    LegionMemo m_oAttacker;
    LegionMemo m_oDefender;
    boolean m_bAngelAvailable;
    boolean m_bArchangelAvailable;
    char m_chTerrain;
    String  m_strMasterHex;
    int m_nEntrySide;

    public LegionMemo getAttacker() { return m_oAttacker; }
    public LegionMemo getDefender() { return m_oDefender; }
    public String getMasterHex() { return m_strMasterHex; }
    public int getEntrySide() { return m_nEntrySide; }

    public BattleMemo(
        LegionMemo oAttacker,
        LegionMemo oDefender,
        boolean bAngelAvailable,
        boolean bArchangelAvailable,
        char chTerrain,
        String strMasterHex,
        int nEntrySide)
        {
            m_oAttacker = oAttacker;
            m_oDefender = oDefender;

            m_bAngelAvailable = bAngelAvailable;
            m_bArchangelAvailable = bArchangelAvailable;
            m_chTerrain = chTerrain;
            m_strMasterHex = strMasterHex;
            m_nEntrySide = nEntrySide;
        }

    public static void writeToFile(BattleMemo oMemo, String strFileName)
        {
            try
            {
                OutputStream oFileStream = new FileOutputStream(new File(strFileName));
                ObjectOutputStream oObjectStream = new ObjectOutputStream(oFileStream);
                oObjectStream.writeObject(oMemo);
            } 
            catch(Throwable t)
            {
                t.printStackTrace();
            }
        }


    public static BattleMemo readFromFile(String strFileName)
        {
            try
            {
                InputStream oFileStream = new FileInputStream(new File(strFileName));
                ObjectInputStream oObjectStream = new ObjectInputStream(oFileStream);
                return (BattleMemo) oObjectStream.readObject();
            } 
            catch(Throwable t)
            {
                t.printStackTrace();
            }
            return null;
        }

}

