
package net.sf.colossus.battle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import net.sf.colossus.*;
import java.util.*;


class LegionEditor extends JPanel
{
    static String[] strColorAbbrevs = 
    {
        "Bk",
        "Br",
        "Bu",
        "Gd",
        "Gr",
        "Rd",
    };

    static String[] strLegionAbbrevs = 
    {
        "01",  
        "02",  
        "03",  
        "04",  
        "05",  
        "06",  
        "07",  
        "08",  
        "09",  
        "10",  
        "11",  
        "12",  
    };

    class VectorListModel extends AbstractListModel
    {
        private Vector m_oVector = new Vector();
        public int getSize() { return m_oVector.size(); }
        public Object getElementAt(int n) 
            { 
                return m_oVector.elementAt(n);
            }

        public void add(Object o)
            {
                //m_oVector.remove(strEmpty);
                m_oVector.add(o);
                fireContentsChanged(this, 0, getSize());
            }
        public void remove(Object o)
            {
                System.out.println("VectorListModel.remove");
                if(m_oVector.contains(o))
                {
                    m_oVector.remove(o);
                    //m_oVector.add(strEmpty);
                    fireContentsChanged(this, 0, getSize());
                }
            }

    }

    public void restoreLegion(LegionMemo oMemo)
        {
            if(oMemo == null)
            {
                System.out.println("cant restore legion ");
                return;
            }
            String strMarkerId = oMemo.getMarkerId();
            String strColorChoice = strMarkerId.substring(0, 2);
            String strLegionChoice = strMarkerId.substring(2, 4);
            System.out.println("color " + strColorChoice);
            System.out.println("legion " + strLegionChoice);

            for(int i = 0; i < strColorAbbrevs.length; i++)
            {
                if(strColorAbbrevs[i].equals(strColorChoice))
                {
                    m_oColorChoice.setSelectedIndex(i);
                    break;
                }
            }

            for(int i = 0; i < strLegionAbbrevs.length; i++)
            {
                if(strLegionAbbrevs[i].equals(strLegionChoice))
                {
                    m_oLegionChoice.setSelectedIndex(i);
                    break;
                }
            }

            String[] strCrittersArray = oMemo.getCritters();
            for(int i = 0; i < strCrittersArray.length; i++)
            {
                m_oCritterModel.add(strCrittersArray[i]);
            }
        }

    private String[] getCrittersArray()
    {
        int nSize = m_oCritterModel.getSize();
        int nCount = 0;
        for(int i = 0; i < nSize; i++)
        {
            
            if(!m_oCritterModel.getElementAt(i).toString().equals(""))
            {
                nCount++;
            }
        }
        String[] strCrittersArray = new String[nCount];
        for(int i = 0; i < nCount; i++)
        {
            strCrittersArray[i] = m_oCritterModel.getElementAt(i).toString();
        }
        return strCrittersArray;
    }

    public LegionMemo saveLegion(String strHexLabel)
    {
        String[] strCrittersArray = getCrittersArray();
        String strMarkerId = m_oColorChoice.getSelectedItem().toString() + m_oLegionChoice.getSelectedItem().toString();
        
        LegionMemo oMemo = 
            new LegionMemo(strMarkerId,
                           "Bk01",
                           strHexLabel,
                           "130",
                           strCrittersArray,
                           "Tom");
        return oMemo;
    }

    final static String strEmpty = "";

    private JComboBox m_oColorChoice;
    private JComboBox m_oLegionChoice;
    private VectorListModel m_oCritterModel;

    LegionEditor(String strCaption)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder(strCaption));
        m_oColorChoice = new JComboBox(strColorAbbrevs);
        m_oLegionChoice = new JComboBox(strLegionAbbrevs);

        m_oCritterModel = new VectorListModel();
        final JList oList = new JList(m_oCritterModel);
        oList.setVisibleRowCount(7);
/*
        for(int i = 0; i < 7; i++)
            m_oCritterModel.add(strEmpty);
*/
        final JComboBox oCharacterChoice = new JComboBox(CharacterArchetype.getCharactersArray());
        JButton addButton = new JButton("+");
        JButton removeButton = new JButton("-");
        addButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent evt)
                    {
                        if(m_oCritterModel.getSize() < 7)
                        {
                            m_oCritterModel.add(oCharacterChoice.getSelectedItem());
                        }
                    }
            });
        removeButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent evt)
                    {
                        m_oCritterModel.remove(oCharacterChoice.getSelectedItem());
                    }
            });
        
        add(m_oColorChoice);
        add(m_oLegionChoice);
        add(oList);
        add(oCharacterChoice);
        add(addButton);
        add(removeButton);
    }
}
