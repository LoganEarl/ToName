package world.entity;

import database.DatabaseManager;
import world.meta.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import static world.entity.EntityTable.*;

public class Entity implements DatabaseManager.DatabaseEntry {

    private String entityID;
    private String displayName;
    private int hp;
    private int maxHP;
    private int mp;
    private int maxMP;
    private int stamina;
    private int maxStamina;

    private int strength;
    private int dexterity;
    private int intelligence;
    private int wisdom;

    private String controllerType;
    private String roomName;

    private String databaseName;

    private static final String GET_SQL = String.format(Locale.US,"SELECT * FROM %s WHERE %s=?",TABLE_NAME,ENTITY_ID);
    private static final String CREATE_SQL = String.format(Locale.US,"INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            TABLE_NAME,ENTITY_ID,DISPLAY_NAME, HP,MAX_HP,MP,MAX_MP,STAMINA,MAX_STAMINA,STR,DEX,INT,WIS,CONTROLLER_TYPE,ROOM_NAME);
    private static final String DELETE_SQL = String.format(Locale.US,"DELETE FROM %s WHERE %s=?", TABLE_NAME,ENTITY_ID);
    private static final String UPDATE_SQL = String.format(Locale.US,"UPDATE %s SET %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=? WHERE %s=?",
            TABLE_NAME, DISPLAY_NAME, HP,MAX_HP,MP,MAX_MP,STAMINA,MAX_STAMINA,STR,DEX,INT,WIS,CONTROLLER_TYPE,ROOM_NAME,ENTITY_ID);

    /**Code returned when an entity has been successfully transferred to a new world by the transferToWorld() method*/
    public static final int CODE_TRANSFER_COMPLETE = 0;
    /**Code returned when an entity could not be transferred to the new world because there already exists an entity by that id in the target world*/
    public static final int CODE_ALREADY_EXISTS_AT_DESTINATION = -1;
    /**Code returned when an entity could not be transferred to the new world because of an unspecified database/file io error*/
    public static final int CODE_TRANSFER_FAILED = -2;

    private Entity() {
        //for use by the builder
    }

    private Entity(ResultSet readEntry, String databaseName) throws SQLException {
        entityID = readEntry.getString(ENTITY_ID);
        displayName = readEntry.getString(DISPLAY_NAME);

        hp = readEntry.getInt(HP);
        maxHP = readEntry.getInt(MAX_HP);
        mp = readEntry.getInt(MP);
        maxMP = readEntry.getInt(MAX_MP);
        stamina = readEntry.getInt(STAMINA);
        maxStamina = readEntry.getInt(MAX_STAMINA);
        strength = readEntry.getInt(STR);
        dexterity = readEntry.getInt(DEX);
        intelligence = readEntry.getInt(INT);
        wisdom = readEntry.getInt(WIS);

        controllerType = readEntry.getString(CONTROLLER_TYPE);
        roomName = readEntry.getString(ROOM_NAME);

        this.databaseName = databaseName;
    }



    /**
     * will transfer this entity to the given world, updating the meta file and everything.
     * If it fails, it will return the appropriate code and keep the entity in it's current world.
     * There cannot be more than one entity with the same Entity_ID in one world, attempting to move
     * an entity whos ID is also in the new world will fail the attempt
     * @param newWorld the new world this entity will exist in
     * @return ont of the CODE_* constants defined above.
     */
    public int transferToWorld(World newWorld){
        if(newWorld == null)
            throw new IllegalArgumentException("cannot transfer to a null world");

        if(existsInDatabase(databaseName) && !removeFromDatabase(databaseName)) {
            updateInDatabase(databaseName);
            return CODE_TRANSFER_FAILED;
        }

        if(existsInDatabase(newWorld.getDatabaseName()))
            return CODE_ALREADY_EXISTS_AT_DESTINATION;
        if(!saveToDatabase(newWorld.getDatabaseName()))
            return CODE_TRANSFER_FAILED;

        if(!World.setWorldOfEntity(this,newWorld))
            return CODE_TRANSFER_FAILED;
        this.databaseName = newWorld.getDatabaseName();
        this.roomName = newWorld.getEntryRoomName();
        updateInDatabase(databaseName);
        return CODE_TRANSFER_COMPLETE;
    }

    public static Entity getEntityByEntityID(String entityID, String databaseName){
        Connection c = DatabaseManager.getDatabaseConnection(databaseName);
        PreparedStatement getSQL = null;
        Entity toReturn;
        if(c == null)
            return null;
        else{
            try {
                getSQL = c.prepareStatement(GET_SQL);
                getSQL.setString(1,entityID);
                ResultSet accountSet = getSQL.executeQuery();
                if(accountSet.next())
                    toReturn = new Entity(accountSet,databaseName);
                else
                    toReturn = null;
                getSQL.close();
                c.close();
            }catch (SQLException e){
                toReturn = null;
            }
        }
        return toReturn;
    }

    @Override
    public boolean saveToDatabase(String databaseName) {
        Entity entity = getEntityByEntityID(entityID,databaseName);
        if(entity == null){
            return DatabaseManager.executeStatement(CREATE_SQL,databaseName,
                    entityID,displayName, hp,maxHP,mp,maxMP,stamina,maxStamina,strength,dexterity, intelligence,wisdom,controllerType,roomName) > 0;
        }else{
            return updateInDatabase(databaseName);
        }
    }

    @Override
    public boolean removeFromDatabase(String databaseName) {
        return DatabaseManager.executeStatement(DELETE_SQL,databaseName, entityID) > 0;
    }

    @Override
    public boolean updateInDatabase(String databaseName) {
        return DatabaseManager.executeStatement(UPDATE_SQL,databaseName,
                displayName, hp,maxHP,mp,maxMP,stamina,maxStamina,strength,dexterity, intelligence,wisdom,controllerType,roomName) > 0;
    }

    @Override
    public boolean existsInDatabase(String databaseName) {
        return getEntityByEntityID(entityID,databaseName) != null;
    }

    public String getID(){
        return entityID;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getControllerType() {
        return controllerType;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public static class EntityBuilder{
        private String entityID = "";
        private String displayName = "";
        private int hp = 10;
        private int maxHP = 10;
        private int mp = 10;
        private int maxMP = 10;
        private int stamina = 10;
        private int maxStamina = 10;

        private int strength = 10;
        private int dexterity = 10;
        private int intelligence = 10;
        private int wisdom = 10;

        private String controllerType = CONTROLLER_TYPE_STATIC;
        private String roomName = "";

        private String databaseName = "";

        public Entity build(){
            Entity e = new Entity();
            e.hp = hp;
            e.maxHP = maxHP;
            e.mp = mp;
            e.maxMP = maxMP;
            e.stamina = stamina;
            e.maxStamina = maxStamina;
            e.entityID = entityID;
            e.displayName = displayName;
            e.strength = strength;
            e.dexterity = dexterity;
            e.intelligence = intelligence;
            e.wisdom = wisdom;
            e.controllerType = controllerType;
            e.roomName = roomName;
            e.databaseName = databaseName;
            return e;
        }

        public EntityBuilder setHPVals(int hp, int maxHP){
            this.hp = hp;
            this.maxHP = maxHP;
            return this;
        }

        public EntityBuilder setMPVals(int mp, int maxMP){
            this.mp = mp;
            this.maxMP = maxMP;
            return this;
        }

        public EntityBuilder setStaminaVals(int stamina, int maxStamina){
            this.stamina = stamina;
            this.maxStamina = maxStamina;
            return this;
        }

        public EntityBuilder setStrength(int strength){
            this.strength = strength;
            return this;
        }

        public EntityBuilder setDexterity(int dexterity){
            this.dexterity = dexterity;
            return this;
        }

        public EntityBuilder setIntelligence(int intelligence){
            this.intelligence = intelligence;
            return this;
        }

        public EntityBuilder setWisdom(int wisdom){
            this.wisdom = wisdom;
            return this;
        }

        public EntityBuilder setID(String id){
            this.entityID = id;
            return this;
        }

        public EntityBuilder setDisplayName(String displayName){
            this.displayName = displayName;
            return this;
        }

        public EntityBuilder setDatabaseName(String databaseName){
            this.databaseName = databaseName;
            return this;
        }

        public EntityBuilder setRoomName(String roomName){
            this.roomName = roomName;
            return this;
        }

        /**
         * sets the controller
         * @param controllerType one of the EntityTable.CONTROLLER_TYPE_* constants
         * @throws IllegalArgumentException if passed in controller type is not one of the EntityTable.CONTROLLER_TYPE_* constants
         * @return this builder
         */
        public EntityBuilder setControllerType(String controllerType){
            if(controllerType != null && controllerType.equals(CONTROLLER_TYPE_PLAYER) || controllerType.equals(CONTROLLER_TYPE_STATIC))
                this.controllerType = controllerType;
            else
                throw new IllegalArgumentException("Cannot assign a controller type that is not one of the EntityTable.CONTROLLER_TYPE_* constants");
            return this;
        }

    }
}
