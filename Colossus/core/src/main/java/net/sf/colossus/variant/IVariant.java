package net.sf.colossus.variant;


import java.util.List;


/**
 *  Defined for which methods we currently abuse the Client to forward the
 *  question about variant specific information to static methods in
 *  TerrainRecruitLoader.
 */
public interface IVariant
{

    /** TODO get from Variant instead of static TerrainRecruitLoader access
     *  Just forwarding the query, to get at least the GUI classes get rid of
     *  dependency to static TerrainRecruitLoader access.
     *
     */
    public List<CreatureType> getPossibleRecruits(MasterBoardTerrain terrain,
        MasterHex hex);

    /** TODO get from Variant instead of static TerrainRecruitLoader access
     *  Just forwarding the query, to get at least the GUI classes get rid of
     *  dependency to static TerrainRecruitLoader access.
     */
    public int numberOfRecruiterNeeded(CreatureType recruiter,
        CreatureType recruit, MasterBoardTerrain terrain, MasterHex hex);

}
