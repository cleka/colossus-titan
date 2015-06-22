package net.sf.colossus.gui;


import java.util.Enumeration;

import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import net.sf.colossus.common.Options;


/**
 * This is a JTable that remembers the size of its columns between runs of the
 * program. The column sizes are stored in the provided Options object, under
 * option names that begin with the provided prefix.
 *
 * We override the table's TableColumnModel with one that gets the desired width
 * of the column from the Options when that column is being added to the table,
 * and writes the new width back to the Options when columns are resided.
 *
 * This class doesn't actually persist the options. The caller is responsible
 * for persisting the options before shutdown and reloading them before calling
 * the constructor for this class.
 *
 * Note that the value we store not only includes the desired widths, but also
 * the column order, but we aren't using the order information when creating the
 * table so the columns always appear in their original order when the program
 * is restarted. This could be enhanced in the future to restore column
 * ordering.
 *
 * @author Scott Greenman
 */
public class ColumnWidthPersistingJTable extends JTable
{
    public ColumnWidthPersistingJTable(String optionName, Options options,
        TableModel tableModel)
    {
        super(tableModel, new ColumnWidthPersistingTableColumnModel(
            optionName, options));
        // When you provide the JTable with a TableColumnModel, it doesn't by
        // default automatically create the columns. If we don't tell it to
        // do that, we end up with an empty table.
        setAutoCreateColumnsFromModel(true);
    }

    // Exposed to package for unit testing
    static class ColumnWidthPersistingTableColumnModel extends
        DefaultTableColumnModel
    {
        private static String COLUMN_SEPARATOR = ",";
        private static String FIELD_SEPARATOR = ":";

        private final String optionName;
        private final Options options;

        public ColumnWidthPersistingTableColumnModel(String optionName,
            Options options)
        {
            super();
            this.optionName = optionName;
            this.options = options;

            addColumnModelListener(new TableColumnModelListener()
            {
                public void columnAdded(TableColumnModelEvent e)
                {
                    // just needed to satisfy the interface
                }

                public void columnRemoved(TableColumnModelEvent e)
                {
                    // just needed to satisfy the interface
                }

                public void columnMoved(TableColumnModelEvent e)
                {
                    // just needed to satisfy the interface
                }

                public void columnSelectionChanged(ListSelectionEvent e)
                {
                    // just needed to satisfy the interface
                }

                /**
                 * This gets called for each column when the table is being
                 * created, and again if a column is resized. Update the option
                 * with the current field sizes.
                 */
                public void columnMarginChanged(ChangeEvent e)
                {
                    StringBuilder optionValueBuilder = new StringBuilder(50);
                    Enumeration<TableColumn> columns = getColumns();
                    while (columns.hasMoreElements())
                    {
                        TableColumn column = columns.nextElement();
                        optionValueBuilder.append(column.getModelIndex())
                            .append(FIELD_SEPARATOR).append(column.getWidth())
                            .append(COLUMN_SEPARATOR);
                    }
                    getOptions().setOption(getOptionName(),
                        optionValueBuilder.toString());
                }
            });
        }

        /**
         * We override addColumn() to set the preferred width
         */
        @Override
        public void addColumn(TableColumn aColumn)
        {
            int preferredWidth = getPreferredWidth(options, optionName,
                aColumn.getModelIndex());
            if (preferredWidth != -1)
            {
                aColumn.setPreferredWidth(preferredWidth);
            }
            super.addColumn(aColumn);
        }

        public String getOptionName()
        {
            return optionName;
        }

        public Options getOptions()
        {
            return options;
        }

        public int getPreferredWidth(Options options, String optionName,
            int dataModelColumnNumber)
        {
            int preferredWidth = -1;
            String preferredWidths = options.getStringOption(optionName);
            if (preferredWidths == null)
            {
                return preferredWidth;
            }
            String colNumAsString = Integer.toString(dataModelColumnNumber);
            String[] cols = preferredWidths.split(COLUMN_SEPARATOR);
            for (String col : cols)
            {
                String[] fields = col.split(FIELD_SEPARATOR);
                if (fields.length == 2)
                {
                    String column = fields[0].trim();
                    String value = fields[1].trim();
                    if (column.length() > 0 && value.length() > 0
                        && colNumAsString.equals(column))
                    {
                        preferredWidth = Integer.parseInt(value);
                        break;
                    }
                }
            }
            return preferredWidth;
        }
    }

}
