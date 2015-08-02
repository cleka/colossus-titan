package net.sf.colossus.webclient;


import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import net.sf.colossus.webcommon.GameInfo;
import net.sf.colossus.webcommon.GameInfo.GameState;
import net.sf.colossus.webcommon.User;


public class GameTableModel extends AbstractTableModel
{
    private final String[] columnNames = { "#", "state", "by", "when",
        "duration", "info", "Variant", "Viewmode", "Expire", "Options",
        "Teleport", "min", "target", "max", "actual", "players", "online" };

    private final Vector<GameInfo> data = new Vector<GameInfo>(17, 1);
    private final HashMap<String, Integer> rowIndex = new HashMap<String, Integer>();
    private final Locale myLocale;

    public GameTableModel(Locale myLocale)
    {
        super();
        this.myLocale = myLocale;
    }

    public int getColumnCount()
    {
        return columnNames.length;
    }

    public int getRowCount()
    {
        return data.size();
    }

    @Override
    public String getColumnName(int col)
    {
        return columnNames[col];
    }

    public Object getValueAt(int row, int col)
    {
        int rows = data.size();

        if (row >= rows)
        {
            return "-";
        }

        GameInfo gi = data.get(row);
        if (gi == null)
        {
            return "-";
        }
        Object o = null;
        switch (col)
        {
            case 0:
                o = gi.getGameId();
                break;

            case 1:
                o = gi.getStateString();
                break;

            case 2:
                o = gi.getInitiator();
                break;

            case 3:
                if (gi.isScheduledGame())
                {
                    o = humanReadableTime(gi.getStartTime());
                }
                else
                {
                    o = "-instantly-";
                }
                break;

            case 4:
                o = gi.getDuration().toString() + " min.";
                break;

            case 5:
                o = gi.getSummary();
                break;

            case 6:
                o = gi.getVariant();
                break;

            case 7:
                o = gi.getViewmode();
                break;

            case 8:
                o = gi.getEventExpiring();
                break;

            case 9:
                o = gi.getGameOptionsFlagsString();
                break;

            case 10:
                o = gi.getTeleportOptionsFlagsString();
                break;

            case 11:
                o = gi.getMin();
                break;

            case 12:
                o = gi.getTargetInteger();
                break;

            case 13:
                o = gi.getMax();
                break;

            case 14:
                o = gi.getEnrolledCount();
                break;

            case 15:
                o = gi.getPlayerListAsString();
                break;

            case 16:
                o = Integer.valueOf(gi.getOnlineCount());
                break;
        }
        return o;
    }

    @Override
    public Class<?> getColumnClass(int col)
    {
        Class<?> c = String.class;

        switch (col)
        {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                c = String.class;
                break;

            case 9:
            case 10:
                // c = Boolean.class;
                c = String.class;
                break;

            case 11:
            case 12:
            case 13:
            case 14:
            case 16:
                c = Integer.class;
                break;

            case 15:
                c = String.class;
                break;
        }

        return c;
    }

    // TableModel forces us into casting
    @SuppressWarnings("unchecked")
    @Override
    public void setValueAt(Object value, int row, int col)
    {
        if (col == -1)
        {
            setRowAt(value, row);
            return;
        }
        GameInfo gi = data.get(row);
        if (gi == null)
        {
            gi = new GameInfo("", false);

        }
        switch (col)
        {
            case 0:
                gi.setGameId((String)value);
                break;

            /*            case 0: String gameId = (String) value;
             gi.setGameId(gameId);
             rowIndex.put(gameId, new Integer(row));
             break;
             */
            case 1:
                GameState gameState = GameState.valueOf((String)value);
                gi.setState(gameState);
                break;

            case 2:
                gi.setInitiator((String)value);
                break;

            case 3:
                gi.setStartTime((String)value);
                break;

            case 4:
                gi.setDuration((String)value);
                break;

            case 5:
                gi.setSummary((String)value);
                break;

            case 6:
                gi.setVariant((String)value);
                break;

            case 7:
                gi.setViewmode((String)value);
                break;

            case 8:
                gi.setEventExpiring((String)value);
                break;

            case 9:
                gi.setUnlimitedMulligans(((Boolean)value).booleanValue());
                break;

            case 10:
                gi.setUnlimitedMulligans(((Boolean)value).booleanValue());
                break;

            case 11:
                gi.setMin((Integer)value);
                break;

            case 12:
                gi.setTarget((Integer)value);
                break;

            case 13:
                gi.setMax((Integer)value);
                break;

            case 14:
                gi.setEnrolledCount((Integer)value);
                break;

            case 15:
                gi.setPlayerList((ArrayList<User>)value);
                break;

            case 16:
                gi.setOnlineCount(((Integer)value).intValue());
                break;
        }
        fireTableCellUpdated(row, col);
    }

    public String getOptionsTooltipText(int row)
    {
        return data.get(row).GetOptionsTooltipText();
    }

    public String getTeleportOptionsTooltipText(int row)
    {
        return data.get(row).GetTeleportOptionsTooltipText();
    }

    public int addGame(GameInfo gi)
    {
        int nextIndex = data.size();
        data.add(gi);
        String gameId = gi.getGameId();
        rowIndex.put(gameId, Integer.valueOf(nextIndex));
        fireTableRowsUpdated(nextIndex, nextIndex);
        return nextIndex;
    }

    // Note that webclient state change code relies on the fact that calling
    // this for a game that is not in the table does not harm nor complain!
    public void removeGame(String gameId)
    {
        int index = this.findRowIndex(gameId);
        if (index != -1)
        {
            data.remove(index);
            rowIndex.remove(gameId);
            redoRowIndices();
            fireTableRowsDeleted(index, index);
        }
        else
        {
            // no problem. For example on login client gets told all the
            // running games and client tries to remove them from pot table
            // but they are not there...
        }
    }

    public void resetTable()
    {
        int size = data.size();
        if (size > 0)
        {
            data.clear();
            rowIndex.clear();
            fireTableRowsDeleted(0, size - 1);
        }
    }

    public void redoRowIndices()
    {
        rowIndex.clear();
        int size = data.size();
        int i = 0;
        while (i < size)
        {
            GameInfo gi = data.get(i);
            String gameId = gi.getGameId();
            rowIndex.put(gameId, Integer.valueOf(i));
            i++;
        }
    }

    public void setRowAt(Object value, int row)
    {
        GameInfo gi = (GameInfo)value;
        String gameId = gi.getGameId();
        rowIndex.put(gameId, Integer.valueOf(row));

        data.set(row, gi);

        fireTableRowsUpdated(row, row);
    }

    public int findRowIndex(String gameId)
    {
        Integer iI = rowIndex.get(gameId);
        if (iI == null)
        {
            return -1;
        }
        else
        {
            return iI.intValue();
        }
    }

    public Integer getRowIndex(GameInfo gi)
    {
        Integer index = rowIndex.get(gi.getGameId());
        if (index == null)
        {
            index = Integer.valueOf(data.size());
            int row = index.intValue();
            data.add(gi);
            rowIndex.put(gi.getGameId(), Integer.valueOf(row));
            fireTableRowsInserted(row, row);
        }
        return index;
    }

    private String humanReadableTime(Long startTime)
    {
        String timeString = "";

        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT,
            DateFormat.SHORT, myLocale);
        df.setTimeZone(TimeZone.getDefault());
        df.setLenient(false);

        timeString = df.format(startTime);

        return timeString;
    }

}
