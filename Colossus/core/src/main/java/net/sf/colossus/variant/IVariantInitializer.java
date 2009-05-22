package net.sf.colossus.variant;


import java.util.Collection;
import java.util.List;

import net.sf.colossus.variant.Variant.AcquirableData;


/**
 *  Access methods how information that is loaded e.g. by TerrainRecruitLoader
 *  finds it's way into the Variant object.
 *  Might be handy also for UnitTest setup.
 */
public interface IVariantInitializer
{

    public List<AcquirableData> getAcquirablesList();

    public int getTitanImprovementValue();

    public int getTitanTeleportValue();

    public Collection<MasterBoardTerrain> getTerrains();

}
