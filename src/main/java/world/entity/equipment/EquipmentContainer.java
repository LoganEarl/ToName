package main.java.world.entity.equipment;

import main.java.world.entity.Attack;
import main.java.world.entity.Entity;
import main.java.world.item.DamageType;
import main.java.world.item.Item;
import main.java.world.item.ItemCollection;
import main.java.world.item.ItemType;
import main.java.world.item.armor.Armor;
import main.java.world.item.armor.ArmorSlot;
import main.java.world.item.container.Container;
import main.java.world.meta.World;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static main.java.world.entity.EntityTable.*;
import static main.java.world.item.armor.ArmorSlot.*;

public class EquipmentContainer implements Entity.SqlExtender, Attack.AttackModifier {
    public static final String SIGNIFIER = "items";

    private Map<ArmorSlot, Integer> slots = new HashMap<>();

    private Entity entity;
    private ItemCollection itemCollection;

    private static final List<String> HEADERS = Arrays.asList(SLOT_HEAD, SLOT_CHEST, SLOT_LEGS, SLOT_FEET, SLOT_HANDS, SLOT_HAND_LEFT, SLOT_SHEATH_LEFT, SLOT_HAND_RIGHT, SLOT_SHEATH_RIGHT, SLOT_BACK, SLOT_BELT_POUCH, SLOT_BELT_UTIL);

    private static final List<ArmorSlot> SLOTS = Arrays.asList(head, chest, legs, feet, hands, leftHand, leftSheath, rightHand, rightSheath, back, beltPouch, beltUtil);

    public static final int CODE_SUCCESS = 0;
    public static final int CODE_CONTAINER_FULL = -1;
    public static final int CODE_TOO_HEAVY = -2;
    public static final int CODE_NOT_NEAR = -3;
    public static final int CODE_NO_ITEM = -4;
    public static final int CODE_WRONG_TYPE = -5;
    public static final int CODE_ERROR = -100;

    public EquipmentContainer(ItemCollection items, Entity entity) {
        this.entity = entity;
        this.itemCollection = items;
    }

    public EquipmentContainer(ResultSet readFrom, ItemCollection items, Entity entity) throws SQLException {
        for (int i = 0; i < HEADERS.size(); i++) {
            slots.put(SLOTS.get(i), readFrom.getInt(HEADERS.get(i)));
        }
        this.itemCollection = items;
        this.entity = entity;
    }

    public int getEquipmentAC() {
        int total = 0;
        for (ArmorSlot slot : slots.keySet()) {
            Integer i = slots.get(slot);
            Item item;
            if (!empty(i) && (item = itemCollection.getItemByID(i, entity.getDatabaseName())) != null) {
                //kek, no holding a breastplate to get the bonuses from it
                ArmorSlot itemSlot;
                if (item.getItemType() == ItemType.armor &&
                        ((itemSlot = ((Armor) item).getSlot()) == slot ||
                                itemSlot == leftHand || itemSlot == rightHand)) {
                    total += ((Armor) item).getArmorClass();
                }
            }
        }
        return total;
    }

    public double getEquipmentDamageResistance(DamageType type){
        double total = 0;
        for (ArmorSlot slot : slots.keySet()) {
            Integer i = slots.get(slot);
            Item item;
            if (!empty(i) && (item = itemCollection.getItemByID(i, entity.getDatabaseName())) != null) {
                //kek, no holding a breastplate to get the bonuses from it
                ArmorSlot itemSlot;
                if (item.getItemType() == ItemType.armor &&
                        ((itemSlot = ((Armor) item).getSlot()) == slot ||
                                itemSlot == leftHand || itemSlot == rightHand)) {
                    total += ((Armor) item).getDefenceForDamageType(type);
                }
            }
        }
        return total;
    }

    private boolean empty(Integer i) {
        return i == null || i == 0;
    }

    public double getEquipmentWeight() {
        double total = 0;
        for (Integer i : slots.values()) {
            Item item;
            if (!empty(i) && (item = itemCollection.getItemByID(i, entity.getDatabaseName())) != null) {
                total += item.getWeight();
            }
        }
        return total;
    }

    public Item getEquippedItem(ArmorSlot selectedSlot) {
        Integer itemID = slots.get(selectedSlot);
        if (empty(itemID)) return null;
        return itemCollection.getItemByID(itemID, entity.getDatabaseName());
    }

    public Item getEquippedItem(int itemID) {
        for (Integer slotItemID : slots.values()) {
            if (!empty(slotItemID) && slotItemID.equals(itemID)) {
                Item equipped = itemCollection.getItemByID(slotItemID, entity.getDatabaseName());
                if (equipped != null)
                    return equipped;
            }
        }
        return null;
    }

    public Item getEquippedItem(String itemName) {
        itemName = itemName.toLowerCase();
        for (Integer itemID : slots.values()) {
            if (!empty(itemID)) {
                Item equipped = itemCollection.getItemByID(itemID, entity.getDatabaseName());
                if (equipped != null && (equipped.getItemName().toLowerCase().equals(itemName) ||
                        equipped.getDisplayableName().toLowerCase().equals(itemName)))
                    return equipped;
            }
        }
        return null;
    }

    public boolean hasItemEquipped(Item equipped) {
        for (Integer itemID : slots.values())
            if (!empty(itemID) &&
                    itemID.equals(equipped.getItemID()) &&
                    entity.getDatabaseName().equals(equipped.getDatabaseName()))
                return true;
        return false;
    }

    /**
     * equips the given piece of armor from either the left or right hand into the fitting slot. If there was already an item in that slot, it will be placed in the hand that equipped it
     *
     * @param armorPiece the armor piece in either the left or right hand
     * @return true if item was equipped
     */
    public boolean equipArmor(Armor armorPiece) {
        ArmorSlot sourceSlot = getSlotOfItem(armorPiece);
        ArmorSlot slotType = armorPiece.getSlot();
        Integer curEquipID = slots.get(slotType);

        if (sourceSlot == leftHand || sourceSlot == rightHand) {
            if (!empty(curEquipID)) {
                Item curEquip = itemCollection.getItemByID(curEquipID, entity.getDatabaseName());
                if (curEquip != null)
                    slots.put(sourceSlot, curEquipID);
            } else
                slots.remove(sourceSlot);
            slots.put(slotType, armorPiece.getItemID());
            return true;
        }
        return false;
    }

    public int holdItem(Item toHold, boolean overrideNearness) {
        int holdCode;
        if ((holdCode = canHoldItem(toHold, overrideNearness)) != CODE_SUCCESS)
            return holdCode;

        ArmorSlot freeHand = getFreeHand();
        slots.put(freeHand, toHold.getItemID());
        toHold.setRoomName("");
        return CODE_SUCCESS;
    }

    public int dropItem(Item toDrop) {
        if (!hasItemEquipped(toDrop))
            return CODE_NO_ITEM;
        ArmorSlot holdingSlot = getSlotOfItem(toDrop);
        if (holdingSlot == rightHand || holdingSlot == leftHand) {
            slots.remove(holdingSlot);
            toDrop.setRoomName(entity.getRoomName());
            return CODE_SUCCESS;
        }
        return CODE_ERROR;
    }

    public boolean isHoldingItem(Item item) {
        ArmorSlot holdingSlot = getSlotOfItem(item);
        return (holdingSlot == rightHand || holdingSlot == leftHand);
    }

    public boolean hasFreeHand() {
        return getFreeHand() != null;
    }

    public boolean isEncumbered() {
        return entity.getStats().getWeightSoftLimit() < getEquipmentWeight();
    }

    /**
     * will stow the weapon in the given hand slot into its sheath/holster slot
     *
     * @param handSlot wither ArmorSlot.rightHand or ArmorSlot.leftHand
     * @return one of the CODE_* constants. {@link #CODE_SUCCESS} if successful
     */
    public int stowWeapon(ArmorSlot handSlot) {
        if (handSlot != leftHand && handSlot != rightHand)
            return CODE_ERROR;

        Integer handID = slots.get(leftHand);
        if (empty(handID))
            return CODE_NO_ITEM;

        if (empty(slots.get(leftSheath)))
            return CODE_CONTAINER_FULL;

        Item leftWeapon = itemCollection.getItemByID(handID, entity.getDatabaseName());
        if (leftWeapon == null)
            return CODE_NO_ITEM;
        if (leftWeapon.getItemType() != ItemType.weapon)
            return CODE_WRONG_TYPE;

        slots.put(rightSheath, leftWeapon.getItemID());
        slots.remove(rightHand);
        return CODE_SUCCESS;
    }

    /**
     * determines if it is possible to hold the given item in a free hand
     *
     * @param toHold the item to hold
     * @return one of teh CODE_* constants. CODE_SUCCESS if able to hold
     */
    public int canHoldItem(Item toHold, boolean overrideNearness) {
        if (!hasFreeHand())
            return CODE_CONTAINER_FULL;
        if (toHold.getWeight() + getEquipmentWeight() > entity.getStats().getWeightHardLimit())
            return CODE_TOO_HEAVY;
        if (!toHold.getRoomName().equals(entity.getRoomName()) && !overrideNearness) {
            return CODE_NOT_NEAR;
        }
        return CODE_SUCCESS;
    }

    /**
     * unequipps the given piece of armor if equipped, and places it in a free hand
     *
     * @param armorPiece the piece of armor to remove
     * @return the previously free hand if successful. null if not successful
     */
    public ArmorSlot unequipArmor(Armor armorPiece) {
        ArmorSlot freeHand = getFreeHand();
        ArmorSlot sourceSlot = getSlotOfItem(armorPiece);

        if (freeHand != null && sourceSlot != null) {
            slots.put(freeHand, armorPiece.getItemID());
            slots.remove(sourceSlot);
            return freeHand;
        }
        return null;
    }

    /**
     * This will put the given item in the given slot. Ignores traditional constraints. If the slot is occupied, will attempt to put the
     * item in a free hand. Failing that, it will be dropped to the floor
     *
     * @param toPut the item to put in the slot
     * @param where the slot to put the item in
     */
    public void forcePutItemInSlot(Item toPut, ArmorSlot where) {
        Integer existingItem = slots.get(where);
        if (existingItem != null && existingItem != 0) {
            ArmorSlot freeHand = getFreeHand();
            if (freeHand == null) {
                Item heldItem = itemCollection.getItemByID(existingItem, entity.getDatabaseName());
                if (heldItem != null) {
                    heldItem.setRoomName(entity.getRoomName());
                    heldItem.saveToDatabase(heldItem.getDatabaseName());
                }
            } else {
                slots.put(freeHand, existingItem);
            }
        }
        slots.put(where, toPut.getItemID());
    }

    @Override
    public Attack modifyAttack(Attack in) {
        DamageType type = in.getDamageType();
        double resistance = getEquipmentDamageResistance(type);
        if(resistance > 1) resistance = 1;
        int reduction = (int)(in.getDamageDealt() * resistance);
        in.setDamageDealt(in.getDamageDealt() - reduction);
        return in;
    }

    public ArmorSlot getSlotOfItem(Item item) {
        for (ArmorSlot slot : slots.keySet()) {
            Integer equipped = slots.get(slot);
            if (!empty(equipped) && equipped.equals(item.getItemID())) {
                return slot;
            }
        }
        return null;
    }

    public ArmorSlot getFreeHand() {
        if (empty(slots.get(rightHand)))
            return rightHand;
        if (empty(slots.get(leftHand)))
            return leftHand;
        return null;
    }

    public void transferEquipmentToWorld(World newWorld) {
        if (newWorld == null)
            return;

        for (Integer itemID : slots.values()) {
            if (!empty(itemID)) {
                Item toTransfer = itemCollection.getItemByID(itemID, entity.getDatabaseName());
                if (toTransfer != null)
                    transferItem(toTransfer, newWorld);
            }
        }
    }

    private void transferItem(Item item, World newWorld) {
        if (item.getItemType() == ItemType.container)
            for (Item i : ((Container) item).getStoredItems())
                transferItem(i, newWorld);
        item.transferToWorld(newWorld);
    }

    @Override
    public String getSignifier() {
        return SIGNIFIER;
    }

    @Override
    public Object[] getInsertSqlValues() {
        List<Object> values = new ArrayList<>(10);
        for (ArmorSlot slot : SLOTS)
            values.add(slots.get(slot));
        return values.toArray(new Object[0]);
    }

    @Override
    public String[] getSqlColumnHeaders() {
        return HEADERS.toArray(new String[0]);
    }
}
