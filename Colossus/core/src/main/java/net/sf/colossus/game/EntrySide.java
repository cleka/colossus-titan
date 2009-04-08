package net.sf.colossus.game;

/**
 * The entry side for a battle.
 */
public enum EntrySide // NO_UCD
{
    TOP_DEFENSE("Top defense"), RIGHT("Right"), RIGHT_DEFENSE(
        "Right defense"), BOTTOM("Bottom"), LEFT_DEFENSE("Left defense"), LEFT(
        "Left"), NOT_SET("Not set");

    private final String label;

    private EntrySide(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
    }

    public int getId()
    {
        // TODO: inline
        return ordinal();
    }

    public boolean isAttackerSide()
    {
        return ordinal() % 2 == 1;
    }

    public EntrySide getOpposingSide()
    {
        return values()[(ordinal() + 3) % 6];
    }

    public static EntrySide fromLabel(String label)
    {
        for (EntrySide entrySide : values())
        {
            // the old code relied on the side being one of the attacker sides,
            // so we keep the restriction
            if (entrySide.isAttackerSide()
                && entrySide.getLabel().equals(label))
            {
                return entrySide;
            }
        }
        throw new IllegalArgumentException(
            "No attacker entry side with label '" + label + "'");
    }

    public static EntrySide fromIntegerId(int id)
    {
        for (EntrySide entrySide : values())
        {
            // the old code relied on the side being one of the attacker sides,
            // so we keep the restriction
            // Clemens: I didn't find anything which restricts the entry sides
            // to be attackers only. Defender is set and stored also, and never
            // re-set/cleared, so there will be defender sides remaining.
            //  ==> letting everything trough as long as it is a valid value,
            // including NOT_SET.
            if (entrySide.getId() == id)
            {
                return entrySide;
                /*
                if (entrySide.isAttackerSide())
                {
                    return entrySide;
                }
                else if (entrySide == EntrySide.NOT_SET)
                {
                    return entrySide;
                }
                else
                {
                    return entrySide;
                }
                */
            }
        }
        throw new IllegalArgumentException("No entry side with id " + id);
    }
}