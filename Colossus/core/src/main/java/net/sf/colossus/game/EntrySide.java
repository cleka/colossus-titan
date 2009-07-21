package net.sf.colossus.game;


/**
 * The entry side for a battle.
 */
public enum EntrySide // NO_UCD
{
    TOP_DEFENSE("Top defense"), RIGHT("Right"), RIGHT_DEFENSE("Right defense"), BOTTOM(
        "Bottom"), LEFT_DEFENSE("Left defense"), LEFT("Left"), NOT_SET(
        "Not set");

    private final String label;

    private EntrySide(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
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
            if (entrySide.getLabel().equals(label))
            {
                return entrySide;
            }
        }
        throw new IllegalArgumentException(
            "No attacker entry side with label '" + label + "'");
    }
}