package net.sf.colossus;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;


/** Viewer for a collection, say the graveyard or the creature keeper
 * <P><B>TODO:</B>remove overlap with existing colossus
 *  @version $Id$
 *  @author Tom Fruchterman
 */
public class CreatureCollectionView extends JDialog
{
	private ICreatureCollection m_oCollection;

	protected IImageUtility m_oImageUtility;

	public CreatureCollectionView(JFrame oParentFrame, 
								  ICreatureCollection oCollection,
								  IImageUtility oImageUtility)
		{
			super(oParentFrame, oCollection.getName(), true);

			m_oCollection = oCollection;
			m_oImageUtility = oImageUtility;

			JPanel oButtonPanel = new JPanel(new FlowLayout());
			getContentPane().add(makeCreaturePanel(), 
								 BorderLayout.CENTER);
			getContentPane().add(oButtonPanel, BorderLayout.SOUTH);

			JButton oCloseButton = new JButton("Close");
			oButtonPanel.add(oCloseButton);
			oCloseButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent evtAction)
						{ 
							hide();
						}
				});
		}
	
	class CreatureCount extends JPanel
	{
		String m_strName;

		CreatureCount(String strName)
			{
				super(new BorderLayout());
				
				setBorder(BorderFactory.createLineBorder(Color.black));

				m_strName = strName;
				JLabel lblCreatureName = new JLabel(strName, SwingConstants.CENTER);
				String strPath = m_oImageUtility.getImagePath(strName);
				ImageIcon oIcon = m_oImageUtility.getImageIcon(strPath);
				JLabel lblCreatureIcon = new JLabel(oIcon);
				JLabel lblCreatureCount = new JLabel(Integer.toString(m_oCollection.getCount(strName)), SwingConstants.CENTER);
				add(lblCreatureName, BorderLayout.NORTH);
				add(lblCreatureIcon, BorderLayout.CENTER);
				add(lblCreatureCount, BorderLayout.SOUTH);
			}
	}

	private JPanel makeCreaturePanel()
		{
			java.util.List oCharacterList = CharacterArchetype.getCharacters();
			int nNumCharacters = oCharacterList.size();
			JPanel oCreaturePanel = new JPanel(new GridLayout(5, nNumCharacters / 5));
			Iterator i = oCharacterList.iterator();
			for(; i.hasNext();)
			{
				String strName = (String) i.next();
				oCreaturePanel.add(new CreatureCount(strName));
			}
			
			return oCreaturePanel;
		}
}
