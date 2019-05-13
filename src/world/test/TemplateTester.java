package world.test;

import client.AccountTable;
import database.DatabaseManager;
import world.entity.Entity;
import world.entity.EntityTable;
import world.item.ContainerInstanceTable;
import world.item.ContainerStatTable;
import world.item.ItemInstanceTable;
import world.item.ItemStatTable;
import world.meta.World;
import world.meta.WorldMetaTable;
import world.meta.WorldTable;
import world.room.RoomTable;
import world.story.StoryArcTable;

import java.util.LinkedList;
import java.util.List;

public class TemplateTester {
    private static final String TEMPLATE_NAME = "testTemplate";

    public static void main(String[] args){
        List<DatabaseManager.DatabaseTable> tables = new LinkedList<>();
        tables.add(new AccountTable());
        tables.add(new ItemStatTable());
        tables.add(new ContainerStatTable());
        tables.add(new RoomTable());
        tables.add(new EntityTable());
        tables.add(new StoryArcTable());

        tables.add(new ItemInstanceTable());
        tables.add(new ContainerInstanceTable());
        tables.add(new WorldMetaTable());

        DatabaseManager.createDirectories();
        DatabaseManager.createNewTemplate(TEMPLATE_NAME + ".db");
        DatabaseManager.createTemplateTables(TEMPLATE_NAME + ".db", tables);

        tables.clear();
        tables.add(new WorldTable());

        DatabaseManager.createNewWorldDatabase(World.META_DATABASE_NAME);
        DatabaseManager.createWorldTables(World.META_DATABASE_NAME,tables);

        World test1 = World.createWorldFromTemplate(TEMPLATE_NAME);
        World test2 = World.createWorldFromTemplate(TEMPLATE_NAME);

        if(test1 == null || test2 == null){
            System.out.println("Unable to create new world sims");
            return;
        }

        System.out.println("Attempting to save entity to database");
        Entity newEntity = new Entity.EntityBuilder()
                .setID("TestEnt")
                .setControllerType(EntityTable.CONTROLLER_TYPE_PLAYER)
                .setDisplayName("CartOfSwine")
                .setRoomName("The Origin")
                .setDatabaseName(test1.getDatabaseName())
                .build();
        newEntity.saveToDatabase(newEntity.getDatabaseName());
        World.setWorldOfEntity(newEntity,test1);

        Entity testEntityRetrieval = Entity.getEntityByEntityID(newEntity.getID(),newEntity.getDatabaseName());
        if(testEntityRetrieval == null){
            System.out.println("Unable to get new entity from test 1");
        }

    }
}
