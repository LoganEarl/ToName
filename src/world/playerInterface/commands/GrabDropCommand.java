package world.playerInterface.commands;

import client.Client;
import world.entity.EquipmentContainer;
import world.item.Item;
import world.item.ItemType;
import world.item.container.Container;

import static world.entity.EquipmentContainer.*;

public class GrabDropCommand extends EntityCommand {
    private String itemIdentifier;
    private String containerIdentifier;
    private boolean pickUp;
    private boolean complete = false;

    public GrabDropCommand(String itemIdentifier, String containerIdentifier, boolean pickUp, Client sourceClient) {
        super(sourceClient);

        this.itemIdentifier = itemIdentifier;
        this.containerIdentifier = containerIdentifier;
        this.pickUp = pickUp;
    }

    @Override
    protected boolean requiresBalance() {
        return true;
    }

    @Override
    protected boolean entityCommandIsComplete() {
        return complete;
    }

    @Override
    protected void executeEntityCommand() {
        if(pickUp)
            pickUp();
        else
            putIn();

        complete = true;
    }

    private void putIn(){
        Item toPutIn = Item.getFromEntityContext(itemIdentifier,getSourceEntity());

        if(toPutIn == null || !getSourceEntity().getEquipment().isHoldingItem(toPutIn))
            getSourceClient().sendMessage("You are not holding a " + itemIdentifier);
        else if(containerIdentifier.isEmpty()){
            //drop it to the floor
            int result = getSourceEntity().getEquipment().dropItem(toPutIn);
            if(result == CODE_SUCCESS)
                getSourceClient().sendMessage("You drop the " + toPutIn.getDisplayableName());
            else if(result == CODE_NO_ITEM)
                getSourceClient().sendMessage("You are not holding a " + itemIdentifier);
            else
                getSourceClient().sendMessage("An error has occurred. You are unable to drop a " + itemIdentifier + " (" + result + ")");
        }else{
            //try store in container
            Item toStoreIn = Item.getFromEntityContext(containerIdentifier, getSourceEntity());
            if(toStoreIn == null)
                getSourceClient().sendMessage("There is not a " + containerIdentifier + " nearby");
            else if(toStoreIn.getItemType() != ItemType.container)
                getSourceClient().sendMessage("The " + toStoreIn.getDisplayableName() + " is not a container. You cannot store items in it");
            else if(toStoreIn.getItemID() == toPutIn.getItemID())
                getSourceClient().sendMessage("You attempt to " + toPutIn.getDisplayableName() + " inside of itself. You fail. Honestly what did you expect?");
            else{
                Container container = (Container) toStoreIn;
                if(!container.canHoldItem(toPutIn))
                    getSourceClient().sendMessage("The " + container.getDisplayableName() + " cannot hold that item");
                else if(container.tryStoreItem(toPutIn)){
                    getSourceClient().sendMessage("You put the " +
                            toPutIn.getDisplayableName() + " in the " +
                            container.getDisplayableName());
                }else
                    getSourceClient().sendMessage("You are unable to put the " +
                            toPutIn.getDisplayableName() + " in the " +
                            container.getDisplayableName());
            }
        }
    }

    private void pickUp(){
        Item toPickUp = Item.getFromEntityContext(itemIdentifier,getSourceEntity());
        int holdCode;

        if(toPickUp == null)
            getSourceClient().sendMessage("There is no " + itemIdentifier + " nearby");
        else if((holdCode = getSourceEntity().getEquipment().canHoldItem(toPickUp)) != EquipmentContainer.CODE_SUCCESS){
            if(holdCode == CODE_CONTAINER_FULL)
                getSourceClient().sendMessage("Your hands are full. You cannot pick up the " + toPickUp.getDisplayableName());
            else if(holdCode == CODE_TOO_HEAVY)
                getSourceClient().sendMessage("The " + toPickUp.getDisplayableName() + " is heavier than you can carry given your strength and equipment");
            else if(holdCode == CODE_NOT_NEAR)
                getSourceClient().sendMessage("There is no " + itemIdentifier + " nearby");
            else
                getSourceClient().sendMessage("An error has occurred. You are unable to pick up that item");
        }else{
            boolean proceed = false;
            if(!containerIdentifier.isEmpty()){
                Item pickupFrom = Item.getFromEntityContext(containerIdentifier, getSourceEntity());
                if(pickupFrom == null)
                    getSourceClient().sendMessage("There is no " + containerIdentifier + " nearby");
                else if(pickupFrom.getItemType() != ItemType.container)
                    getSourceClient().sendMessage("The " + pickupFrom.getDisplayableName() + " is not a container. It cannot hold items in it");
                else if(!((Container)pickupFrom).containsItem(toPickUp))
                    getSourceClient().sendMessage("The " + pickupFrom.getDisplayableName() + " does not contain a " + toPickUp.getDisplayableName());
                else {
                    proceed = true;
                }
            }else{
                proceed = true;
            }

            if(proceed) {
                toPickUp.setContainerID(0);
                toPickUp.setRoomName("");
                getSourceEntity().getEquipment().holdItem(toPickUp);
                getSourceClient().sendMessage("You pick up the " + toPickUp.getDisplayableName());
            }
        }
    }
}
