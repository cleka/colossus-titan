package net.sf.colossus.gui;

import junit.framework.TestCase;
import net.sf.colossus.common.Options;

public class ColumnWidthPersistingJTableTest extends TestCase
{
    public void TestParseWidthProperty()
    {
        ColumnWidthPersistingJTable.ColumnWidthPersistingTableColumnModel model = new ColumnWidthPersistingJTable.ColumnWidthPersistingTableColumnModel(
            "irrelevent", null);

        Options testOptions = new Options("irrelevant");

        testOptions.setOption("invalid",
            "this is not a valid column width string");
        assertEquals(-1, model.getPreferredWidth(testOptions, "invalid", 0));

        testOptions.setOption("no data", "0::0:,0,,::,:");
        assertEquals(-1, model.getPreferredWidth(testOptions, "no data", 0));

        testOptions.setOption("valid", "3:5,19:193,0:11,2:99,");
        assertEquals(11, model.getPreferredWidth(testOptions, "valid", 0));
        assertEquals(-1, model.getPreferredWidth(testOptions, "valid", 1));
        assertEquals(99, model.getPreferredWidth(testOptions, "valid", 2));
        assertEquals(5, model.getPreferredWidth(testOptions, "valid", 3));
        assertEquals(193, model.getPreferredWidth(testOptions, "valid", 19));

        testOptions.setOption("extraspace", "3 : 5, 19:193 , 0 : 11 ,2:99,");
        assertEquals(11, model.getPreferredWidth(testOptions, "extraspace", 0));
        assertEquals(-1, model.getPreferredWidth(testOptions, "extraspace", 1));
        assertEquals(99, model.getPreferredWidth(testOptions, "extraspace", 2));
        assertEquals(5, model.getPreferredWidth(testOptions, "extraspace", 3));
        assertEquals(193, model.getPreferredWidth(testOptions, "extraspace",
            19));

        assertEquals(-1, model.getPreferredWidth(testOptions, "non-existent",
            0));
    }
}
