
package net.sf.colossus.battle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import net.sf.colossus.*;
import java.util.*;

public class BattleEditor extends JFrame
{
    private String m_strFileName = "c:/Default.battle";
    LegionEditor m_oFirstLegionEditor;
    LegionEditor m_oSecondLegionEditor;
    HexEditor m_oHexEditor;
    
    private JPanel createControlPanel()
    {
        JPanel oControlPanel = new JPanel();
        oControlPanel.setLayout(new BoxLayout(oControlPanel, 
                                              BoxLayout.X_AXIS));
        m_oFirstLegionEditor = new LegionEditor("Defender");
        oControlPanel.add(m_oFirstLegionEditor);
        m_oSecondLegionEditor = new LegionEditor("Attacker");
        oControlPanel.add(m_oSecondLegionEditor);
        
        m_oHexEditor = new HexEditor();
        oControlPanel.add(m_oHexEditor);
        
        return oControlPanel;
    }

    private void restoreBattle()
        {
            BattleMemo oMemo = BattleMemo.readFromFile(m_strFileName);
            if(oMemo != null)
            {
                LegionMemo oAttacker = oMemo.getAttacker();
                LegionMemo oDefender = oMemo.getDefender();
                m_oFirstLegionEditor.restoreLegion(oAttacker);
                m_oSecondLegionEditor.restoreLegion(oDefender);
                System.out.println("Restore battle " + m_strFileName);
            }
        }

    private void saveBattle()
        {
            BattleMemo oMemo;
            LegionMemo oFirstLegionMemo = m_oFirstLegionEditor.saveLegion(m_oHexEditor.getLabel());
            LegionMemo oSecondLegionMemo = m_oSecondLegionEditor.saveLegion(m_oHexEditor.getLabel());
            oMemo = new BattleMemo(oFirstLegionMemo,
                                   oSecondLegionMemo,
                                   false,
                                   false,
                                   m_oHexEditor.getTerrain(),
                                   m_oHexEditor.getLabel(),
                                   1);
            BattleMemo.writeToFile(oMemo, m_strFileName);
            System.out.println("Save battle " + m_strFileName);
        }

    private JPanel createButtonPanel()
    {
        JPanel oButtonPanel = new JPanel();
        JButton oSaveButton = new JButton("Save");
        oButtonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        oButtonPanel.add(oSaveButton);
        oSaveButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent evt)
                    {
                        saveBattle();
                    }
            });
        return oButtonPanel;
    }

    public BattleEditor(String strFileName)
    {
        super("Battle Editor");
        m_strFileName = strFileName;

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
        String strFileName;
        if(strArgsArray.length == 1)
            strFileName = strArgsArray[0];
        else
            strFileName = "c:/Default.battle";

        BattleEditor oFrame = new BattleEditor(strFileName);
        oFrame.restoreBattle();
        oFrame.pack();
        oFrame.show();
        System.out.println("Edit Battles!");
    }
}
