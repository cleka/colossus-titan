
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
                m_oVector.remove(strEmpty);
                m_oVector.add(o);
                fireContentsChanged(this, 0, getSize());
            }
        public void remove(Object o)
            {
                if(m_oVector.contains(o))
                {
                    m_oVector.remove(o);
                    m_oVector.add("");
                    fireContentsChanged(this, 0, getSize());
                }
            }

    }

    final static String strEmpty = "";

    LegionEditor(String strCaption)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder(strCaption));
        JComboBox oColorChoice = new JComboBox(strColorAbbrevs);
        JComboBox oLegionChoice = new JComboBox(strLegionAbbrevs);


        final VectorListModel oModel = new VectorListModel();
        final JList oList = new JList(oModel);
        oList.setVisibleRowCount(7);
        for(int i = 0; i < 7; i++)
            oModel.add(strEmpty);
        final JComboBox oCharacterChoice = new JComboBox(CharacterArchetype.getCharactersArray());
        JButton addButton = new JButton("+");
        JButton removeButton = new JButton("-");
        addButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent evt)
                    {
                        if(oModel.getSize() <= 7)
                        {
                            oModel.add(oCharacterChoice.getSelectedItem());
                        }
                    }
            });
        removeButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent evt)
                    {
                        oModel.remove(oCharacterChoice.getSelectedItem());
                    }
            });
        
        add(oColorChoice);
        add(oLegionChoice);
        add(oList);
        add(oCharacterChoice);
        add(addButton);
        add(removeButton);
    }
}

public class BattleEditor extends JFrame
{
    private JPanel createControlPanel()
    {
        JPanel oControlPanel = new JPanel();
        oControlPanel.setLayout(new BoxLayout(oControlPanel, 
                                              BoxLayout.X_AXIS));
        oControlPanel.add(new LegionEditor("Defender"));
        oControlPanel.add(new LegionEditor("Attacker"));
        return oControlPanel;
    }
    private JPanel createButtonPanel()
    {
        JPanel oButtonPanel = new JPanel();
        oButtonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        oButtonPanel.add(new JButton("Save"));
        return oButtonPanel;
    }
    public BattleEditor()
    {
        super("Battle Editor");

        Container oContentPane = getContentPane();

        JPanel oButtonPanel = createButtonPanel();
        JPanel oControlPanel = createControlPanel();

        oContentPane.add(oControlPanel, BorderLayout.CENTER);
        oContentPane.add(oButtonPanel, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter()
            {
                public void windowClosing(WindowEvent evt)
                    {
                        System.exit(0);
                    }
            });
    }

    public static void main(String[] strArgsArray)
    {
        JFrame oFrame = new BattleEditor();
        oFrame.pack();
        oFrame.show();
        System.out.println("Edit Battles!");
    }
}
