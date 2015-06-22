package net.sf.colossus.game.actions;


import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.MasterHex;


public class RelocateLegion extends LegionAction
{
    private final MasterHex hex;

    public RelocateLegion(Legion legion, MasterHex hex)
    {
        super(legion);
        this.hex = hex;
    }

    public MasterHex getDestination()
    {
        return hex;
    }

    @Override
    public String toString()
    {
        return String.format("Relocation of legion %s to new hex %s",
            getLegion().getMarkerId(), getDestination().getLabel());
    }
}
