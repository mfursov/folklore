package net.sf.nightworks.act.spell


import net.sf.nightworks.AFF_POISON
import net.sf.nightworks.DT
import net.sf.nightworks.ITEM_BLESS
import net.sf.nightworks.ITEM_BURN_PROOF
import net.sf.nightworks.Skill
import net.sf.nightworks.TARGET_OBJ
import net.sf.nightworks.TO_AFFECTS
import net.sf.nightworks.TO_ALL
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.TO_WEAPON
import net.sf.nightworks.WEAPON_FLAMING
import net.sf.nightworks.WEAPON_FROST
import net.sf.nightworks.WEAPON_HOLY
import net.sf.nightworks.WEAPON_POISON
import net.sf.nightworks.WEAPON_SHARP
import net.sf.nightworks.WEAPON_SHOCKING
import net.sf.nightworks.WEAPON_VAMPIRIC
import net.sf.nightworks.WEAPON_VORPAL
import net.sf.nightworks.act
import net.sf.nightworks.affect_join
import net.sf.nightworks.affect_to_obj
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.Apply
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.Obj
import net.sf.nightworks.saves_spell
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.IS_OBJ_STAT
import net.sf.nightworks.util.IS_WEAPON_STAT

fun spell_poison(level: Int, ch: CHAR_DATA, vo: Any, target: Int) {
    if (target == TARGET_OBJ) {
        val obj = vo as Obj

        if (obj.item_type == ItemType.Food || obj.item_type == ItemType.DrinkContainer) {
            if (IS_OBJ_STAT(obj, ITEM_BLESS) || IS_OBJ_STAT(obj, ITEM_BURN_PROOF)) {
                act("Your spell fails to corrupt \$p.", ch, obj, null, TO_CHAR)
                return
            }
            obj.value[3] = 1
            act("\$p is infused with poisonous vapors.", ch, obj, null, TO_ALL)
            return
        }

        if (obj.item_type == ItemType.Weapon) {
            if (IS_WEAPON_STAT(obj, WEAPON_FLAMING)
                    || IS_WEAPON_STAT(obj, WEAPON_FROST)
                    || IS_WEAPON_STAT(obj, WEAPON_VAMPIRIC)
                    || IS_WEAPON_STAT(obj, WEAPON_SHARP)
                    || IS_WEAPON_STAT(obj, WEAPON_VORPAL)
                    || IS_WEAPON_STAT(obj, WEAPON_SHOCKING)
                    || IS_WEAPON_STAT(obj, WEAPON_HOLY)
                    || IS_OBJ_STAT(obj, ITEM_BLESS) || IS_OBJ_STAT(obj, ITEM_BURN_PROOF)) {
                act("You can't seem to envenom \$p.", ch, obj, null, TO_CHAR)
                return
            }

            if (IS_WEAPON_STAT(obj, WEAPON_POISON)) {
                act("\$p is already envenomed.", ch, obj, null, TO_CHAR)
                return
            }
            val af = Affect()
            af.where = TO_WEAPON
            af.type = Skill.poison
            af.level = level / 2
            af.duration = level / 8
            af.bitvector = WEAPON_POISON
            affect_to_obj(obj, af)

            act("\$p is coated with deadly venom.", ch, obj, null, TO_ALL)
            return
        }

        act("You can't poison \$p.", ch, obj, null, TO_CHAR)
        return
    }

    val victim = vo as CHAR_DATA

    if (saves_spell(level, victim, DT.Poison)) {
        act("\$n turns slightly green, but it passes.", victim, null, null, TO_ROOM)
        send_to_char("You feel momentarily ill, but it passes.\n", victim)
        return
    }
    val af = Affect()
    af.type = Skill.poison
    af.level = level
    af.duration = 10 + level / 10
    af.location = Apply.Str
    af.modifier = -2
    af.bitvector = AFF_POISON
    affect_join(victim, af)
    send_to_char("You feel very sick.\n", victim)
    act("\$n looks very ill.", victim, null, null, TO_ROOM)
}