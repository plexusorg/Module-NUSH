package dev.plex.nush;

import javax.annotation.Nullable;

public enum NushAction
{
    MUTE("Mute Player", 0),
    CANCEL("Cancel", 1),
    SMITE("Smite", 2),
    BAN("Ban Player", 3),
    ACCEPT("Accept", 4);

    public final String humanReadable;
    public final int ordinal;

    NushAction(String humanReadable, int ordinal)
    {
        this.humanReadable = humanReadable;
        this.ordinal = ordinal;
    }

    @Nullable
    public static NushAction fromOrdinal(int ordinal)
    {
        for (NushAction value : NushAction.values())
        {
            if (value.ordinal == ordinal)
            {
                return value;
            }
        }

        return null;
    }
}
