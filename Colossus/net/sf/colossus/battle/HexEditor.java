
package net.sf.colossus.battle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import net.sf.colossus.*;
import java.util.*;

import MasterBoard;
import MasterHex;


class HexEditor extends JPanel
{
    static Hashtable s_oTerrainHash = new Hashtable();
    static String[] s_strTerrainArray = 
    {
        "Brush",
        "Desert",
        "Hills",
        "Mountains",
        "Marsh",
        "Plains",
        "Swamp",
        "Tower",
        "Tundra",
        "Woods",
    };


    static
    {
        s_oTerrainHash.put("Brush", "B");
        s_oTerrainHash.put("Desert", "D");
        s_oTerrainHash.put("Hills", "H");
        s_oTerrainHash.put("Mountains", "m");
        s_oTerrainHash.put("Marsh", "M");
        s_oTerrainHash.put("Plains", "P");
        s_oTerrainHash.put("Swamp", "S");
        s_oTerrainHash.put("Tower", "T");
        s_oTerrainHash.put("Tundra", "t");
        s_oTerrainHash.put("Woods", "W");
    }

    JComboBox m_oTerrainChoice;
    JLabel m_oHexLabel;

    public char getTerrain()
    {
        String strTerrain = (String)
            s_oTerrainHash.get(m_oTerrainChoice.getSelectedItem());
        return strTerrain.charAt(0);
    }

    public String getLabel()
    {
        return m_oHexLabel.getText();
    }

    private void setLabel(String strLabel)
    {
        m_oHexLabel.setText(strLabel);
    }

    private void fixHexId()
    {
        String strTerrain = m_oTerrainChoice.getSelectedItem().toString();
        char chTerrain = s_oTerrainHash.get(strTerrain).toString().charAt(0);
        MasterHex oHex = MasterBoard.getAnyHexWithTerrain(chTerrain);
        String strHexLabel = oHex.getLabel();
        setLabel(strHexLabel);
    }
    
    HexEditor()
    {
        m_oTerrainChoice = new JComboBox(s_strTerrainArray);
        add(m_oTerrainChoice);
        m_oHexLabel = new JLabel("");
        add(m_oHexLabel);

        fixHexId();

        m_oTerrainChoice.addItemListener(new ItemListener()
            {
                public void itemStateChanged(ItemEvent evt)
                {
                    fixHexId();
                }
            }
            );
    }
}
