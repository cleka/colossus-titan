/**
 * TMJF -- test creature displays
 * 
 * <P><B>TODO:</B>remove overlap with existing colossus
 *
 *  @version $Id$
 *  @author Tom Fruchterman
 */

import net.sf.colossus.*;
import javax.swing.*;

public class CreatureDisplay
{
	
	public static void main(String[] strArgsArray)
		{
			IImageUtility oImageUtility = new ChitImageUtility();
			System.out.println("Test!");
			CreatureKeeperData oKeeperData = new CreatureKeeperData();
			oKeeperData.resetAllCounts();
			JFrame oFrame = null;
			JDialog oDialog = 
				new CreatureCollectionView(oFrame, 
										   oKeeperData,
										   oImageUtility);
			oDialog.pack();
			oDialog.show();

			oDialog = new CreatureCollectionView(oFrame,
												 new GraveyardData(),
												 oImageUtility);
			oDialog.pack();
			oDialog.show();

			System.exit(0);
		}
}
