package net.sf.colossus.util;


public class CompareDoubles
{

    /**
     * Returns true if the two given doubles are basically equal (comparing
     * difference to very small value to get rid of rounding issues)
     * @param d1 First double value
     * @param d2 Second double value
     * @return boolean whether they are basically equal value or not
     */
    public static boolean almostEqual(double d1, double d2)
    {
        return (Math.abs(d1 - d2) < 0.0000001);
    }
}
