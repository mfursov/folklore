package net.sf.nightworks.act.spell

import net.sf.nightworks.ITEM_GLOW
import net.sf.nightworks.ITEM_HUM
import net.sf.nightworks.ITEM_INVIS
import net.sf.nightworks.ITEM_MAGIC
import net.sf.nightworks.ITEM_NODROP
import net.sf.nightworks.ITEM_NOREMOVE
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.extract_obj
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.Wear
import net.sf.nightworks.util.randomPercent
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.URANGE

fun spell_disenchant_weapon(level: Int, ch: CHAR_DATA, obj: Obj) {
    if (obj.item_type != ItemType.Weapon) {
        send_to_char("That isn't a weapon.\n", ch)
        return
    }

    if (obj.wear_loc.isOn()) {
        send_to_char("The item must be carried to be enchanted.\n", ch)
        return
    }

    /* find the bonuses */
    var fail = 75
    fail -= (level - obj.level) * 5
    if (IS_SET(obj.extra_flags, ITEM_MAGIC)) {
        fail += 25
    }

    fail = URANGE(5, fail, 95)

    val result = randomPercent()

    /* the moment of truth */
    if (result < fail / 5)
    /* item destroyed */ {
        act("\$p shivers violently and explodes!", ch, obj, null, TO_CHAR)
        act("\$p shivers violently and explodeds!", ch, obj, null, TO_ROOM)
        extract_obj(obj)
        return
    }

    if (result <= fail / 2) {
        send_to_char("Nothing seemed to happen.\n", ch)
        return
    }

    /* item disenchanted */
    act("\$p glows brightly, then fades.", ch, obj, null, TO_CHAR)
    act("\$p glows brightly, then fades.", ch, obj, null, TO_ROOM)
    obj.enchanted = true

    /* remove all affects */
    var paf = obj.affected
    while (paf != null) {
        //todo:??
        paf = paf.next
    }
    obj.affected = null
    obj.enchanted = false

    /* clear some flags */
    obj.extra_flags = REMOVE_BIT(obj.extra_flags, ITEM_GLOW)
    obj.extra_flags = REMOVE_BIT(obj.extra_flags, ITEM_HUM)
    obj.extra_flags = REMOVE_BIT(obj.extra_flags, ITEM_MAGIC)
    obj.extra_flags = REMOVE_BIT(obj.extra_flags, ITEM_INVIS)
    obj.extra_flags = REMOVE_BIT(obj.extra_flags, ITEM_NODROP)
    obj.extra_flags = REMOVE_BIT(obj.extra_flags, ITEM_NOREMOVE)
}