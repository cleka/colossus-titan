
package net.sf.colossus;

import javax.swing.ImageIcon;

/** Interface for stuff that CreatureCollectionView needs from the
 * main colossus for images.  We are trying to isolate this from the
 * main code so no references can be made back there. Plus the main 
 * code is in the default package.
 *
 * <P><B>TODO:</B>remove overlap with existing colossus
 *
 *  @version $Id$
 *  @author Tom Fruchterman */
public interface IImageUtility
{
    public ImageIcon getImageIcon(String strPath);

    public String getImagePath(String strName);
}
