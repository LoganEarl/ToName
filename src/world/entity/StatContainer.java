package world.entity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

import static world.entity.EntityTable.*;

public class StatContainer implements Entity.SqlExtender {
    private int strength;
    private int dexterity;
    private int intelligence;
    private int wisdom;

    private static final Random RND = new Random(System.currentTimeMillis());

    public static final String SIGNIFIER = "stats";
    private static final String[] HEADERS = new String[]{STR,DEX,INT,WIS};

    public StatContainer(int strength, int dexterity, int intelligence, int wisdom) {
        this.strength = strength;
        this.dexterity = dexterity;
        this.intelligence = intelligence;
        this.wisdom = wisdom;
    }

    public StatContainer(ResultSet readEntry) throws SQLException {
        strength = readEntry.getInt(STR);
        dexterity = readEntry.getInt(DEX);
        intelligence = readEntry.getInt(INT);
        wisdom = readEntry.getInt(WIS);
    }

    public int preformStatCheck(String stat, int difficultyModifier){
        int baseStat = getStat(stat);
        
        return baseStat - RND.nextInt(100)+difficultyModifier;
    }

    private int getStat(String columnName) {
        switch (columnName) {
            case STR:
                return strength;
            case DEX:
                return dexterity;
            case INT:
               return intelligence;
            case WIS:
                return wisdom;
            default:
                throw new IllegalArgumentException("Column name " + columnName + " is not an entity stat");
        }
    }

    @Override
    public String getSignifier() {
        return SIGNIFIER;
    }

    @Override
    public Object[] getInsertSqlValues() {
        return new Object[]{strength,dexterity,intelligence,wisdom};
    }

    @Override
    public String[] getSqlColumnHeaders() {
        return HEADERS;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public int getDexterity() {
        return dexterity;
    }

    public void setDexterity(int dexterity) {
        this.dexterity = dexterity;
    }

    public int getIntelligence() {
        return intelligence;
    }

    public void setIntelligence(int intelligence) {
        this.intelligence = intelligence;
    }

    public int getWisdom() {
        return wisdom;
    }

    public void setWisdom(int wisdom) {
        this.wisdom = wisdom;
    }
}
