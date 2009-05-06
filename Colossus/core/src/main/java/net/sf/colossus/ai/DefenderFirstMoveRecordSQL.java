package net.sf.colossus.ai;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import net.sf.colossus.client.CritterMove;
import net.sf.colossus.client.HexMap;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.variant.AllCreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;

/**
 *
 * @author Romain Dolbeau
 */
class DefenderFirstMoveRecordSQL
{

    final static private Logger LOGGER = Logger.getLogger(
            DefenderFirstMoveRecordSQL.class.getName());
    private Connection connection;
    private final AllCreatureType creatures;
    private final String variantName;

    /** Creates a new instance of BattleRecordSQL */
    DefenderFirstMoveRecordSQL(String serverName, String username,
            String password, AllCreatureType creatures, String variantName)
    {
        this.creatures = creatures;
        this.variantName = variantName;
        /* connect */
        try
        {
            // Load the JDBC driver
            String driverName = "com.mysql.jdbc.Driver";
            Class.forName(driverName);

            // Create a connection to the database
            String mydatabase = getDatabaseName();
            String url = "jdbc:mysql://" + serverName + "/" + mydatabase;
            connection = DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException e)
        {
            // Could not find the database driver
            connection = null;
            LOGGER.warning("SQL: " + e.toString());
        } catch (SQLException e)
        {
            // Could not connect to the database
            connection = null;
            LOGGER.warning("SQL: " + e.toString());
        }
        if (connection != null)
        {
            boolean found = false;
            try
            {
                // Gets the database metadata
                DatabaseMetaData dbmd = connection.getMetaData();

                // Specify the type of object; in this case we want tables
                String[] types =
                {
                    "TABLE"
                };
                ResultSet resultSet = dbmd.getTables(null, null, "%", types);

                String myTable = getTableName();
                // Get the table names
                while (!found && resultSet.next())
                {
                    // Get the table name
                    String tableName = resultSet.getString(3);
                    if (tableName.equals(myTable))
                    {
                        found = true;
                    }
                }
            } catch (SQLException e)
            {
                LOGGER.warning("SQL: " + e.toString());
            }
            if (!found)
            {
                try
                {
                    Statement stmt = connection.createStatement();

                    // Create table called my_table
                    String sql = getCreateTableStatement();

                    stmt.executeUpdate(sql);
                } catch (SQLException e)
                {
                    LOGGER.warning("SQL CREATE: " + e.toString());
                }
            }
        }
    }

    private String critterMoveListToNameString(List<CritterMove> moves)
    {
        StringBuffer buf = new StringBuffer();
        for (CritterMove cm : moves)
        {
            buf.append(cm.getCritter().getCreatureType().getName());
            buf.append(",");
        }
        return buf.toString();
    }
    private String critterMoveListToHexString(List<CritterMove> moves)
    {
        StringBuffer buf = new StringBuffer();
        for (CritterMove cm : moves)
        {
            buf.append(cm.getEndingHex().getLabel());
            buf.append(",");
        }
        return buf.toString();
    }
    private String battleCritterListToString(List<BattleCritter> attackers)
    {
        StringBuffer buf = new StringBuffer();
        for (BattleCritter bc : attackers)
        {
            buf.append(bc.getCreatureType().getName());
            buf.append(",");
        }
        return buf.toString();
    }

    void insertMove(List<CritterMove> moves, List<BattleCritter> attackers, MasterBoardTerrain mbt, EntrySide es)
    {
        Collections.sort(moves, new Comparator<CritterMove>()
        {
            public int compare(CritterMove cm1, CritterMove cm2)
            {
                return cm1.getCritter().getCreatureType().getName().compareTo(cm2.getCritter().
                        getCreatureType().getName());
            }
        });
        Collections.sort(attackers, new Comparator<BattleCritter>()
        {

            public int compare(BattleCritter cm1, BattleCritter cm2)
            {
                return cm1.getCreatureType().getName().compareTo(cm2.
                        getCreatureType().
                        getName());
            }
        });

        StringBuffer sql = new StringBuffer();

        sql.append("INSERT INTO ");
        sql.append(getTableName());
        sql.append(" (attackers,defenders,battleland,entryside,moves) VALUES ('");
        sql.append(battleCritterListToString(attackers));
        sql.append("','");
        sql.append(critterMoveListToNameString(moves));
        sql.append("','");
        sql.append(mbt.getDisplayName());
        sql.append("','");
        sql.append(es.getLabel());
        sql.append("','");
        sql.append(critterMoveListToHexString(moves));
        sql.append("');");

        try
        {
            Statement stmt = connection.createStatement();

            // Execute the insert statement
            stmt.executeUpdate(sql.toString());

            LOGGER.warning("SQL INSERT: [" + sql.toString() +
                    "] SUCCESSFUL");
        } catch (SQLException e)
        {
            LOGGER.warning("SQL INSERT: " + e.toString() + "[" + sql.toString() +
                    "] FAILED");
        }
    }

    private List<CritterMove> makeCritterMoveList(MasterBoardTerrain mbt, List<CritterMove> moves, String fmoves)
    {
        List<CritterMove> result = new ArrayList<CritterMove>();

        String[] smoves = fmoves.split(",");

        for (int i = 0 ; i < moves.size() ; i++)
        {
            result.add(new CritterMove(moves.get(i).getCritter(),
                                       moves.get(i).getStartingHex(),
                                       HexMap.getHexByLabel(mbt, smoves[i])));
        }

        return result;
    }

    Set<List<CritterMove>> getPerfectMatches(List<CritterMove> moves, List<BattleCritter> attackers, MasterBoardTerrain mbt, EntrySide es)
    {
        Set<List<CritterMove>> results = new HashSet<List<CritterMove>>();

        StringBuffer sql = new StringBuffer();
        sql.append("SELECT moves FROM ");
        sql.append(getTableName());
        sql.append(" WHERE attackers='");
        sql.append(battleCritterListToString(attackers));
        sql.append("' AND defenders='");
        sql.append(critterMoveListToNameString(moves));
        sql.append("' AND battleland='");
        sql.append(mbt.getDisplayName());
        sql.append("' AND entryside='");
        sql.append(es.getLabel());
        sql.append("';");

        ResultSet rs = null;
        try {
            // Create a result set containing all data from my_table
            Statement stmt = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery(sql.toString());
        } catch (SQLException e) {
            LOGGER.warning("SQL SELECT: " + e.toString());
        }

        if (rs != null) {
            try {
                while (rs.next()) {
                    String fmoves = rs.getString("moves");
                    LOGGER.warning("Found Movement '" + fmoves + '"');

                    List<CritterMove> lcm = makeCritterMoveList(mbt, moves, fmoves);

                    results.add(lcm);
                }
            } catch (SQLException e) {
                LOGGER.warning("SQL RS: " + e.toString());
            }
        }

        return results;
    }

    Set<List<CritterMove>> getImperfectMatches(List<CritterMove> moves, List<BattleCritter> attackers, MasterBoardTerrain mbt, EntrySide es)
    {
        Set<List<CritterMove>> results = new HashSet<List<CritterMove>>();

        StringBuffer sql = new StringBuffer();
        sql.append("SELECT moves,attackers FROM ");
        sql.append(getTableName());
        sql.append(" WHERE defenders='");
        sql.append(critterMoveListToNameString(moves));
        sql.append("' AND battleland='");
        sql.append(mbt.getDisplayName());
        sql.append("' AND entryside='");
        sql.append(es.getLabel());
        sql.append("';");

        ResultSet rs = null;
        try {
            // Create a result set containing all data from my_table
            Statement stmt = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery(sql.toString());
        } catch (SQLException e) {
            LOGGER.warning("SQL SELECT: " + e.toString());
        }

        if (rs != null) {
            try {
                while (rs.next()) {
                    String fmoves = rs.getString("moves");
                    String fattackers = rs.getString("attackers");
                    LOGGER.warning("Found Movement '" + fmoves + "' with attackers '" + fattackers + "'");

                    List<CritterMove> lcm = makeCritterMoveList(mbt, moves, fmoves);

                    results.add(lcm);
                }
            } catch (SQLException e) {
                LOGGER.warning("SQL RS: " + e.toString());
            }
        }

        return results;
    }

    boolean isUsable() {
        return connection != null;
    }

    private String getCreateTableStatement()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("CREATE TABLE " + getTableName() +
                " (SID integer AUTO_INCREMENT,");
        buf.append("attackers varchar(512),");
        buf.append("defenders varchar(512),");
        buf.append("battleland varchar(32),");
        buf.append("entryside varchar(32),");
        buf.append("moves varchar(128),");
        buf.append("PRIMARY KEY (SID));");
        LOGGER.finest("SQL Create Table Statement is \"" + buf.toString() + "\"");
        return buf.toString();
    }

    private String getDatabaseName()
    {
        return "ColossusDefenderFirstMoveRecord";
    }

    private String getTableName()
    {
        return "TableFor" + variantName.replaceAll(".xml", "");
    }
}
