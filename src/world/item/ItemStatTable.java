package world.item;

import database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Contains the definition for a SQL table that holds the stats for different types of items. Does not hold the
 * specific instances of each item, only the stats.
 * @author Logan Earl
 */
public class ItemStatTable implements DatabaseManager.DatabaseTable {
    /**The name of the table used to store item stats*/
    public static final String TABLE_NAME = "itemStats";

    /**The name of the item. Used as a unique identifier*/
    public static final String ITEM_NAME = "itemName";
    /**The description of the item*/
    public static final String ITEM_DESCRIPTION = "itemDescription";
    /**The weight of the item in KGs*/
    public static final String WEIGHT = "weightKgs";
    /**The size of the item in Liters*/
    public static final String VOLUME = "volume";

    public static final String ARMOR_STATS = "armorStats";

    public static final String CONSUMABLE_STATS = "consumableStats";

    public static final String CONTAINER_STATS = "containerStats";

    public static final String WEAPON_STATS = "weaponStats";

    public static final String MISC_STATS = "miscStats";

    /**The type of the item. Must be one of the TYPE_* constants defined in this class*/
    public static final String ITEM_TYPE = "itemType";

    public enum WeaponType{
        head, chest, legs, feet, hands
    }

    /**A Map, containing the column names as keys and the associated data-type of the column as values*/
    public final Map<String, String> TABLE_DEFINITION = new LinkedHashMap<>();

    public ItemStatTable(){
        TABLE_DEFINITION.put(ITEM_NAME, "VARCHAR(32) PRIMARY KEY NOT NULL");
        TABLE_DEFINITION.put(ITEM_DESCRIPTION, "TEXT");
        TABLE_DEFINITION.put(WEIGHT,"DECIMAL");
        TABLE_DEFINITION.put(VOLUME,"DECIMAL");
        TABLE_DEFINITION.put(ITEM_TYPE,"VARCHAR(16)");
    }

    private static final String GET_SQL = String.format(Locale.US, "SELECT * FROM %s WHERE %s=?", TABLE_NAME, ITEM_NAME);

    public static Map<String,String> getStatsForItem(String itemName, String databaseName){
        Connection c = DatabaseManager.getDatabaseConnection(databaseName);
        PreparedStatement getSQL;
        Map<String, String> itemStats = new HashMap<>();
        if(c == null)
            return null;
        else{
            try {
                getSQL = c.prepareStatement(GET_SQL);
                getSQL.setString(1,itemName);
                ResultSet accountSet = getSQL.executeQuery();
                if(accountSet.next()) {
                    itemStats.put(ITEM_NAME, accountSet.getString(ITEM_NAME));
                    itemStats.put(ITEM_DESCRIPTION, accountSet.getString(ITEM_DESCRIPTION));
                    itemStats.put(WEIGHT, accountSet.getString(WEIGHT));
                    itemStats.put(VOLUME, accountSet.getString(VOLUME));
                    itemStats.put(ITEM_TYPE, accountSet.getString(ITEM_TYPE));
                }else
                    itemStats = null;
                getSQL.close();
                //c.close();
            }catch (SQLException e){
                itemStats = null;
            }
        }
        return itemStats;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public Map<String, String> getColumnDefinitions() {
        return TABLE_DEFINITION;
    }

    @Override
    public List<String> getConstraints() {
        return null;
    }
}
