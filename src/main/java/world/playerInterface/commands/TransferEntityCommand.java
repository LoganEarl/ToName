package main.java.world.playerInterface.commands;

import main.java.client.Account;
import main.java.client.Client;
import main.java.client.ClientRegistry;
import main.java.network.CommandExecutor;
import main.java.world.WorldModel;
import main.java.world.WorldUtils;
import main.java.world.entity.Entity;
import main.java.world.meta.World;
import main.java.world.notification.ConcreteNotification;
import main.java.world.notification.NotificationScope;
import main.java.world.room.RoomNotificationScope;

import static main.java.world.playerInterface.ColorTheme.*;

public class TransferEntityCommand extends EntityCommand {
    private String entityID;
    private String worldID;
    private boolean complete = false;
    private CommandExecutor executor;

    public TransferEntityCommand(String entityID, String worldID, Client sourceClient, WorldModel model) {
        super(sourceClient, model);

        this.entityID = entityID;
        this.worldID = worldID;
        this.executor = model.getExecutor();
    }

    @Override
    protected boolean entityCommandIsComplete() {
        return complete;
    }

    @Override
    protected void executeEntityCommand() {
        if (!WorldUtils.isAuthorized(getSourceClient(), Account.AccountType.ADMIN)) {
            getSourceClient().sendMessage(WorldUtils.getRefusedFlavorText());
        } else {
            Entity toTransfer = getWorldModel().getEntityCollection().getPlayableEntityByID(entityID);
            if (toTransfer == null)
                toTransfer = getWorldModel().getEntityCollection().getEntityByDisplayName(entityID, getSourceEntity().getRoomName(), getSourceEntity().getDisplayName());
            World transferTo;
            try {
                transferTo = World.getWorldByWorldID(Integer.parseInt(worldID));
            } catch (NumberFormatException ignored) {
                transferTo = null;
            }
            if (World.getLimboWorld().getName().toLowerCase().equals(worldID.toLowerCase()))
                transferTo = World.getLimboWorld();
            if (World.getHubWorld().getName().toLowerCase().equals(worldID.toLowerCase()))
                transferTo = World.getHubWorld();

            if (toTransfer != null) {
                if (transferTo != null) {
                    String sourceRoomName = toTransfer.getRoomName();
                    String sourceDatabaseName = toTransfer.getDatabaseName();

                    int result = toTransfer.transferToWorld(transferTo);
                    if (result == Entity.CODE_ALREADY_EXISTS_AT_DESTINATION)
                        getSourceClient().sendMessage("Unable to move you there. It looks like an entity with the same account as you already exists there");
                    else if (result != Entity.CODE_TRANSFER_COMPLETE)
                        getSourceClient().sendMessage("Unable to move that entity there. Code: " + result);
                    else {
                        NotificationScope lastScope = new RoomNotificationScope(sourceRoomName,sourceDatabaseName);
                        getWorldModel().getNotificationService().notify(
                                new TransferEntityNotification(
                                        getSourceEntity(),
                                        false,
                                        getWorldModel().getRegistry()),
                                lastScope);

                        notifyEntityRoom(new TransferEntityNotification(getSourceEntity(),true, getWorldModel().getRegistry()));
                        executor.scheduleCommand(new LookCommand("", false, getSourceClient(), getWorldModel()));
                    }
                } else {
                    getSourceClient().sendMessage("I don't see a main.java.world with ID: " + worldID);
                }
            } else
                getSourceClient().sendMessage("I don't see an player here with ID: " + entityID);
        }
        complete = true;
    }

    @Override
    protected boolean requiresBalance() {
        return true;
    }

    public class TransferEntityNotification extends ConcreteNotification {
        private Entity sourceEntity;
        private boolean didAppear;

        public TransferEntityNotification(Entity sourceEntity, boolean didAppear, ClientRegistry registry) {
            super(registry);

            this.sourceEntity = sourceEntity;
            this.didAppear = didAppear;
        }

        @Override
        public String getAsMessage(Entity viewer) {
            String message;
            if (viewer.equals(sourceEntity)) {
                if (didAppear)
                    message = getMessageInColor("You arrive at your destination", INFORMATIVE);
                else
                    message = "Harnessing eldritch powers you bore a hole through the fabric of reality. Concentrated energies give way and you feel yourself warped and pulled through the space between.";
            } else {
                if(didAppear)
                    message = getMessageInColor("Reality shudders. " + getEntityColored(sourceEntity,  viewer, getWorldModel()) + " emerges from a place between", INFORMATIVE);
                else
                    message = getMessageInColor("Reality shudders. " + getEntityColored(sourceEntity,  viewer, getWorldModel()) + " disappears into a place between", INFORMATIVE);
            }

            return message;
        }
    }
}
