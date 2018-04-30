package net.sf.nightworks.act.spell

import net.sf.nightworks.ITEM_MAGIC
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.util.dice
import net.sf.nightworks.extract_obj
import net.sf.nightworks.get_skill
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.Obj
import net.sf.nightworks.util.randomPercent
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.IS_SET

fun spell_drain(ch: CHAR_DATA, obj: Obj) {
    if (!IS_SET(obj.extra_flags, ITEM_MAGIC)) {
        send_to_char("That item is not magical.\n", ch)
        return
    }

    var drain = when (obj.item_type) {
        ItemType.Armor -> obj.value[3].toInt()
        ItemType.Treasure -> 4
        ItemType.Potion -> 8
        ItemType.Scroll, ItemType.Staff, ItemType.Wand -> 12
        ItemType.Weapon -> (obj.value[1] + obj.value[2] / 2).toInt()
        ItemType.Light -> if (obj.value[2] == -1L) 10 else 4
        else -> 1
    }

    var paf = obj.affected
    while (paf != null) {
        drain += 5
        paf = paf.next
    }

    drain *= dice(2, 5)
    drain += obj.level / 2

    if (randomPercent() > get_skill(ch, Skill.drain)) {
        act("\$p evaporates!", ch, obj, null, TO_ROOM)
        act("\$p evaporates, but you fail to channel the energy.", ch, obj, null, TO_CHAR)
    } else {
        act("\$p evaporates as \$n drains its energy!", ch, obj, null, TO_ROOM)
        act("\$p evaporates as you drain its energy!", ch, obj, null, TO_CHAR)
        ch.mana = Math.min(ch.mana + drain, ch.max_mana)
    }
    extract_obj(obj)
}