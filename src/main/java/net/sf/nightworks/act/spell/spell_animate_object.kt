package net.sf.nightworks.act.spell

import net.sf.nightworks.AFF_CHARM
import net.sf.nightworks.DICE_BONUS
import net.sf.nightworks.DICE_NUMBER
import net.sf.nightworks.DICE_TYPE
import net.sf.nightworks.ITEM_WEAR_BODY
import net.sf.nightworks.ITEM_WEAR_HANDS
import net.sf.nightworks.ITEM_WEAR_SHIELD
import net.sf.nightworks.MOB_VNUM_ARMOR
import net.sf.nightworks.MOB_VNUM_WEAPON
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_AFFECTS
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.affect_to_char
import net.sf.nightworks.char_to_room
import net.sf.nightworks.count_charmed
import net.sf.nightworks.create_mobile
import net.sf.nightworks.extract_obj
import net.sf.nightworks.get_mob_index_nn
import net.sf.nightworks.get_skill
import net.sf.nightworks.util.interpolate
import net.sf.nightworks.is_affected
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.Obj
import net.sf.nightworks.util.randomPercent
import net.sf.nightworks.util.number_range
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.CAN_WEAR
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.capitalize

fun spell_animate_object(level: Int, ch: CHAR_DATA, obj: Obj) {
    if (obj.item_type != ItemType.Weapon && obj.item_type != ItemType.Armor) {
        send_to_char("You can animate only armors and weapons.\n", ch)
        return
    }

    if (is_affected(ch, Skill.animate_object)) {
        send_to_char("You cannot summon the strength to handle more undead bodies.\n", ch)
        return
    }

    if (obj.level > level) {
        act("\$p is too powerful for you to animate it.", ch, obj, null, TO_CHAR)
        return
    }

    if (count_charmed(ch) != 0) {
        return
    }

    if (obj.item_type == ItemType.Armor && !(CAN_WEAR(obj, ITEM_WEAR_BODY)
            || CAN_WEAR(obj, ITEM_WEAR_HANDS)
            || CAN_WEAR(obj, ITEM_WEAR_SHIELD))) {
        send_to_char("You can only animate that type of armor.\n", ch)
        return
    }

    if (randomPercent() > get_skill(ch, Skill.animate_object)) {
        act("\$p violately explodes!", ch, obj, null, TO_CHAR)
        act("\$p violately explodes!", ch, obj, null, TO_ROOM)
        extract_obj(obj)
        return
    }

    val mob = if (obj.item_type == ItemType.Weapon) {
        create_mobile(get_mob_index_nn(MOB_VNUM_WEAPON))
    } else {
        create_mobile(get_mob_index_nn(MOB_VNUM_ARMOR))
    }

    val buf = TextBuffer()
    buf.sprintf("animate %s", obj.name)
    mob.name = buf.toString()

    buf.sprintf(mob.short_desc, obj.short_desc)
    mob.short_desc = buf.toString()

    buf.sprintf("%s is here, staring at you!.\n", capitalize(obj.short_desc))
    mob.long_desc = buf.toString()

    char_to_room(mob, ch.room)
    mob.level = obj.level

    mob.perm_stat.fill { Math.min(25, ch.perm_stat[it]) }

    mob.armor.fill(interpolate(mob.level, 100, -100), 0, 3)
    mob.armor[3] = interpolate(mob.level, 100, 0)

    if (obj.item_type == ItemType.Weapon) {
        mob.hit = if (ch.pcdata == null)
            100
        else
            Math.min(25 * mob.level + 1000, 30000)
        mob.max_hit = mob.hit
        mob.mana = ch.level * 40
        mob.max_mana = mob.mana
        mob.move = ch.level * 40
        mob.max_move = mob.move
        mob.timer = 0
        mob.damage[DICE_NUMBER] = obj.value[1].toInt()
        mob.damage[DICE_TYPE] = obj.value[2].toInt()
        mob.damage[DICE_BONUS] = number_range(level / 10, level / 8)
    }

    if (obj.item_type == ItemType.Armor) {
        mob.hit = if (ch.pcdata == null)
            100
        else
            Math.min(100 * mob.level + 2000, 30000)
        mob.max_hit = mob.hit
        mob.mana = ch.level * 40
        mob.max_mana = mob.mana
        mob.move = ch.level * 40
        mob.max_move = mob.move
        mob.timer = 0
        mob.damage[DICE_NUMBER] = number_range(level / 15, level / 12)
        mob.damage[DICE_TYPE] = number_range(level / 3, level / 2)
        mob.damage[DICE_BONUS] = number_range(level / 10, level / 8)
    }
    mob.sex = ch.sex
    mob.gold = 0
    mob.leader = ch
    mob.master = mob.leader
    mob.affected_by = SET_BIT(mob.affected_by, AFF_CHARM)

    val af = Affect()
    af.type = Skill.animate_object
    af.level = ch.level
    af.duration = 1 + obj.level / 30
    affect_to_char(ch, af)

    act("You give life to \$p with your power!\n", ch, obj, null, TO_CHAR)
    act("\$n gives life to \$p with \$s power!\n", ch, obj, null, TO_ROOM)

    extract_obj(obj)
}