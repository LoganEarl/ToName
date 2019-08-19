package world.playerInterface;

import world.WorldModel;
import world.diplomacy.DiplomacyManager;
import world.diplomacy.DiplomaticRelation;
import world.diplomacy.Faction;
import world.entity.Entity;
import world.item.Item;

import java.awt.*;
import java.util.Locale;

public class ColorTheme {
    public static final Color OUTGOING_DAMAGE = new Color(16,119,192);
    public static final Color INCOMING_DAMAGE = new Color(255,11,5);

    public static final Color ITEM = new Color(91,52,0);

    public static final Color SUCCESS = new Color(0,208,14);
    public static final Color WARNING = new Color(255,144,0);
    public static final Color FAILURE = new Color(255,7,0);
    public static final Color INFORMATIVE = new Color(1,103,176);

    public static final Color ALLY = new Color(0,72,5);
    public static final Color FRIENDLY = new Color(0,34,59);
    public static final Color NEUTRAL = new Color(69,69,69);
    public static final Color UNFRIENDLY = new Color(91,52,0);
    public static final Color ENEMY = new Color(91,2,0);

    public static final Color HP_COLOR = new Color(210,5,0);
    public static final Color MP_COLOR = new Color(0,79,135);
    public static final Color STAMINA_COLOR = new Color(0,165,11);
    public static final Color BURNOUT_COLOR = new Color(210,118,0);

    public static String getMessageInColor(String message, DiplomaticRelation relation){
        return getMessageInColor(message, getColorOfRelation(relation));
    }

    public static Color getColorOfRelation(DiplomaticRelation relation){
        switch (relation){
            case allied:
                return ALLY;
            case friendly:
                return FRIENDLY;
            case neutral:
                return NEUTRAL;
            case unfriendly:
                return UNFRIENDLY;
            case enemies:
                return ENEMY;
        }
        return NEUTRAL;
    }

    public static String getItemColored(Item item){
        return getMessageInColor(item.getDisplayableName(),ITEM);
    }

    public static String getEntityColored(Entity targetEntity, Entity sourceEntity, WorldModel worldModel){
        return getEntityColored(targetEntity, sourceEntity,worldModel.getDiplomacyManager());
    }

    public static String getEntityColored(Entity viewedEntity, Entity observerEntity, DiplomacyManager diplomacyManager){
        Faction viewedFaction = viewedEntity.getDiplomacy().getFaction();
        DiplomaticRelation relation = diplomacyManager.getRelation(viewedFaction,observerEntity.getDiplomacy().getFaction());
        return getMessageInColor(viewedEntity.getDisplayName() + " the " + viewedEntity.getRace().getDisplayName(),relation);
    }

    public static String getMessageInColor(String message, Color selectColor){
        return String.format(Locale.US, "<font color=\"%s\">%s</font>", getHexValue(selectColor), message);
    }

    public static String getHexValue(Color color){
        return String.format("#%02x%02x%02x",color.getRed(),color.getGreen(), color.getBlue());
    }
}
