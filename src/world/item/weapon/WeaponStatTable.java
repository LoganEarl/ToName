package world.item.weapon;

import database.DatabaseManager;

import java.util.List;
import java.util.Map;

public class WeaponStatTable implements  DatabaseManager.DatabaseTable {
    @Override
    public String getTableName() {
        return null;
    }

    @Override
    public Map<String, String> getColumnDefinitions() {
        return null;
    }

    @Override
    public List<String> getConstraints() {
        return null;
    }
}
