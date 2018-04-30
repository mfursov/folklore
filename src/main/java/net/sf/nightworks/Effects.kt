package net.sf.nightworks

import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.Apply
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.ROOM_INDEX_DATA
import net.sf.nightworks.util.DAZE_STATE
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_OBJ_STAT
import net.sf.nightworks.util.URANGE
import net.sf.nightworks.util.randomPercent
import net.sf.nightworks.util.number_range

fun acid_effect(vo: Any, level: Int, dam: Int, target: Int) {
    when (target) {
    /* nail objects on the floor */
        TARGET_ROOM -> (vo as ROOM_INDEX_DATA).objects.forEach { obj -> acid_effect(obj, level, dam, TARGET_OBJ) }
    /* do the effect on a victim */
        TARGET_CHAR -> (vo as CHAR_DATA).carrying.forEach { obj -> acid_effect(obj, level, dam, TARGET_OBJ) }
    /* toast an object */
        TARGET_OBJ -> {
            val obj = vo as Obj
            if (IS_OBJ_STAT(obj, ITEM_BURN_PROOF) || IS_OBJ_STAT(obj, ITEM_NOPURGE)
                    || obj.pIndexData.limit != -1 || number_range(0, 4) == 0) return

            var chance = level / 4 + dam / 10

            if (chance > 25) {
                chance = (chance - 25) / 2 + 25
            }
            if (chance > 50) {
                chance = (chance - 50) / 2 + 50
            }

            if (IS_OBJ_STAT(obj, ITEM_BLESS)) {
                chance -= 5
            }

            chance -= obj.level * 2

            val msg: String
            when (obj.item_type) {
                ItemType.Container, ItemType.CorpsePc, ItemType.CorpseNpc -> msg = "\$p fumes and dissolves."
                ItemType.Armor -> msg = "\$p is pitted and etched."
                ItemType.Clothing -> msg = "\$p is corroded into scrap."
                ItemType.Staff, ItemType.Wand -> {
                    chance -= 10
                    msg = "\$p corrodes and breaks."
                }
                ItemType.Scroll -> {
                    chance += 10
                    msg = "\$p is burned into waste."
                }
                else -> return
            }

            chance = URANGE(5, chance, 95)

            if (randomPercent() > chance) {
                return
            }

            val carried_by = obj.carried_by
            val in_room = obj.in_room
            if (carried_by != null) {
                act(msg, carried_by, obj, null, TO_ALL)
            } else if (in_room != null) {
                act(msg, in_room, obj, null, TO_ALL)
            }

            if (obj.item_type == ItemType.Armor) {

                affect_enchant(obj)

                var af_found = false
                var paf = obj.affected
                while (paf != null) {
                    if (paf.location === Apply.Ac) {
                        af_found = true
                        paf.type = Skill.general_purpose
                        paf.modifier += 1
                        paf.level = Math.max(paf.level, level)
                        break
                    }
                    paf = paf.next
                }

                if (!af_found) { /* needs a new affect */
                    paf = Affect()

                    paf.type = Skill.general_purpose
                    paf.level = level
                    paf.duration = -1
                    paf.location = Apply.Ac
                    paf.modifier = 1
                    paf.next = obj.affected
                    obj.affected = paf
                }

                if (carried_by != null && obj.wear_loc.isOn()) {
                    for (i in 0..3) carried_by.armor[i] += 1
                }
                return
            }

            /* get rid of the object */
            loop@ for (t_obj in obj.contains) {
                obj_from_obj(t_obj)
                when {
                    in_room != null -> obj_to_room(t_obj, in_room)
                    carried_by != null -> obj_to_room(t_obj, carried_by.room)
                    else -> {
                        extract_obj(t_obj)
                        continue@loop
                    }
                }
                acid_effect(t_obj, level / 2, dam / 2, TARGET_OBJ)
            }
            extract_obj(obj)
        }
    }
}


fun cold_effect(vo: Any, level: Int, dam: Int, target: Int) {
    when (target) {
    /* nail objects on the floor */
        TARGET_ROOM -> (vo as ROOM_INDEX_DATA).objects.forEach { obj -> cold_effect(obj, level, dam, TARGET_OBJ) }
        TARGET_CHAR -> { /* whack a character */
            val victim = vo as CHAR_DATA

            /* chill touch effect */
            if (!saves_spell(level / 4 + dam / 20, victim, DT.Cold)) {
                act("\$n turns blue and shivers.", victim, null, null, TO_ROOM)
                act("A chill sinks deep into your bones.", victim, null, null, TO_CHAR)
                val af = Affect()
                af.type = Skill.chill_touch
                af.level = level
                af.duration = 6
                af.location = Apply.Str
                af.modifier = -1
                affect_join(victim, af)
            }

            /* hunger! (warmth sucked out */
            if (victim.pcdata != null) {
                gain_condition(victim, COND_HUNGER, dam / 20)
            }

            /* let's toast some gear */
            victim.carrying.forEach { obj -> cold_effect(obj, level, dam, TARGET_OBJ) }
        }
        TARGET_OBJ -> /* toast an object */ {
            val obj = vo as Obj
            if (IS_OBJ_STAT(obj, ITEM_BURN_PROOF)
                    || IS_OBJ_STAT(obj, ITEM_NOPURGE)
                    || obj.pIndexData.limit != -1
                    || number_range(0, 4) == 0) {
                return
            }

            var chance = level / 4 + dam / 10

            if (chance > 25) {
                chance = (chance - 25) / 2 + 25
            }
            if (chance > 50) {
                chance = (chance - 50) / 2 + 50
            }

            if (IS_OBJ_STAT(obj, ITEM_BLESS)) {
                chance -= 5
            }

            chance -= obj.level * 2

            val msg: String
            when (obj.item_type) {
                ItemType.Potion -> {
                    msg = "\$p freezes and shatters!"
                    chance += 25
                }
                ItemType.DrinkContainer -> {
                    msg = "\$p freezes and shatters!"
                    chance += 5
                }
                else -> return
            }

            chance = URANGE(5, chance, 95)

            if (randomPercent() > chance) {
                return
            }
            val carried_by = obj.carried_by
            if (carried_by != null) {
                act(msg, carried_by, obj, null, TO_ALL)
            } else {
                val in_room = obj.in_room
                if (in_room != null) {
                    act(msg, in_room, obj, null, TO_ALL)
                }
            }
            extract_obj(obj)
        }
    }
}


fun fire_effect(vo: Any, level: Int, dam: Int, target: Int) {
    when (target) {
    /* nail objects on the floor */
        TARGET_ROOM -> (vo as ROOM_INDEX_DATA).objects.forEach { obj -> fire_effect(obj, level, dam, TARGET_OBJ) }
        TARGET_CHAR -> { /* do the effect on a victim */
            val victim = vo as CHAR_DATA

            /* chance of blindness */
            if (!IS_AFFECTED(victim, AFF_BLIND) && !saves_spell(level / 4 + dam / 20, victim, DT.Fire)) {
                act("\$n is blinded by smoke!", victim, null, null, TO_ROOM)
                act("Your eyes tear up from smoke...you can't see a thing!", victim, null, null, TO_CHAR)

                val af = Affect()
                af.type = Skill.fire_breath
                af.level = level
                af.duration = number_range(0, level / 10)
                af.location = Apply.Hitroll
                af.modifier = -4
                af.bitvector = AFF_BLIND

                affect_to_char(victim, af)
            }

            /* getting thirsty */
            if (victim.pcdata != null) {
                gain_condition(victim, COND_THIRST, dam / 20)
            }

            /* let's toast some gear! */
            victim.carrying.forEach { obj -> fire_effect(obj, level, dam, TARGET_OBJ) }
        }
    /* toast an object */
        TARGET_OBJ -> {
            val obj = vo as Obj
            if (IS_OBJ_STAT(obj, ITEM_BURN_PROOF)
                    || IS_OBJ_STAT(obj, ITEM_NOPURGE)
                    || obj.pIndexData.limit != -1
                    || number_range(0, 4) == 0) {
                return
            }

            var chance = level / 4 + dam / 10

            if (chance > 25) {
                chance = (chance - 25) / 2 + 25
            }
            if (chance > 50) {
                chance = (chance - 50) / 2 + 50
            }

            if (IS_OBJ_STAT(obj, ITEM_BLESS)) {
                chance -= 5
            }
            chance -= obj.level * 2

            val msg: String
            if (check_material(obj, "ice")) {
                chance += 30
                msg = "\$p melts and evaporates!"
            } else {
                when (obj.item_type) {
                    ItemType.Container -> msg = "\$p ignites and burns!"
                    ItemType.Potion -> {
                        chance += 25
                        msg = "\$p bubbles and boils!"
                    }
                    ItemType.Scroll -> {
                        chance += 50
                        msg = "\$p crackles and burns!"
                    }
                    ItemType.Staff -> {
                        chance += 10
                        msg = "\$p smokes and chars!"
                    }
                    ItemType.Wand -> msg = "\$p sparks and sputters!"
                    ItemType.Food -> msg = "\$p blackens and crisps!"
                    ItemType.Pill -> msg = "\$p melts and drips!"
                    else -> return
                }
            }

            chance = URANGE(5, chance, 95)

            if (randomPercent() > chance) {
                return
            }

            val carried_by = obj.carried_by
            val in_room = obj.in_room
            if (carried_by != null) {
                act(msg, carried_by, obj, null, TO_ALL)
            } else {
                if (in_room != null) {
                    act(msg, in_room, obj, null, TO_ALL)
                }
            }

            loop@ for (t_obj in obj.contains) {
                obj_from_obj(t_obj)
                when {
                    in_room != null -> obj_to_room(t_obj, in_room)
                    carried_by != null -> obj_to_room(t_obj, carried_by.room)
                    else -> {
                        extract_obj(t_obj)
                        continue@loop
                    }
                }
                fire_effect(t_obj, level / 2, dam / 2, TARGET_OBJ)
            }

            extract_obj(obj)
        }
    }

}

fun poison_effect(vo: Any, level: Int, dam: Int, target: Int) {
    when (target) {
    /* nail objects on the floor */
        TARGET_ROOM -> (vo as ROOM_INDEX_DATA).objects.forEach { obj -> poison_effect(obj, level, dam, TARGET_OBJ) }
    /* do the effect on a victim */
        TARGET_CHAR -> {
            val victim = vo as CHAR_DATA
            /* chance of poisoning */
            if (!saves_spell(level / 4 + dam / 20, victim, DT.Poison)) {
                send_to_char("You feel poison coursing through your veins.\n", victim)
                act("\$n looks very ill.", victim, null, null, TO_ROOM)

                val af = Affect()
                af.type = Skill.poison
                af.level = level
                af.duration = level / 2
                af.location = Apply.Str
                af.modifier = -1
                af.bitvector = AFF_POISON
                affect_join(victim, af)
            }

            /* equipment */
            victim.carrying.forEach { obj -> poison_effect(obj, level, dam, TARGET_OBJ) }
        }
    /* do some poisoning */
        TARGET_OBJ -> {
            val obj = vo as Obj

            if (IS_OBJ_STAT(obj, ITEM_BURN_PROOF)
                    || IS_OBJ_STAT(obj, ITEM_BLESS)
                    || obj.pIndexData.limit != -1
                    || number_range(0, 4) == 0) {
                return
            }

            var chance = level / 4 + dam / 10
            if (chance > 25) {
                chance = (chance - 25) / 2 + 25
            }
            if (chance > 50) {
                chance = (chance - 50) / 2 + 50
            }

            chance -= obj.level * 2

            when (obj.item_type) {
                ItemType.Food -> {
                }
                ItemType.DrinkContainer -> if (obj.value[0] == obj.value[1]) {
                    return
                }
                else -> return
            }

            chance = URANGE(5, chance, 95)

            if (randomPercent() > chance) {
                return
            }

            obj.value[3] = 1
        }
    }
}


fun shock_effect(vo: Any, level: Int, dam: Int, target: Int) {
    when (target) {
        TARGET_ROOM -> (vo as ROOM_INDEX_DATA).objects.forEach { shock_effect(it, level, dam, TARGET_OBJ) }
        TARGET_CHAR -> {
            val victim = vo as CHAR_DATA

            /* daze and confused? */
            if (!saves_spell(level / 4 + dam / 20, victim, DT.Lightning)) {
                send_to_char("Your muscles stop responding.\n", victim)
                DAZE_STATE(victim, Math.max(12, level / 4 + dam / 20))
            }

            /* toast some gear */
            victim.carrying.forEach { shock_effect(it, level, dam, TARGET_OBJ) }
            return
        }
        TARGET_OBJ -> {
            val obj = vo as Obj

            if (IS_OBJ_STAT(obj, ITEM_BURN_PROOF)
                    || IS_OBJ_STAT(obj, ITEM_NOPURGE)
                    || obj.pIndexData.limit != -1
                    || number_range(0, 4) == 0) {
                return
            }

            var chance = level / 4 + dam / 10

            if (chance > 25) {
                chance = (chance - 25) / 2 + 25
            }
            if (chance > 50) {
                chance = (chance - 50) / 2 + 50
            }

            if (IS_OBJ_STAT(obj, ITEM_BLESS)) {
                chance -= 5
            }

            chance -= obj.level * 2

            val msg: String
            when (obj.item_type) {
                ItemType.Wand, ItemType.Staff -> {
                    chance += 10
                    msg = "\$p overloads and explodes!"
                }
                ItemType.Jewelry -> {
                    chance -= 10
                    msg = "\$p is fused into a worthless lump."
                }
                else -> return
            }

            chance = URANGE(5, chance, 95)

            if (randomPercent() > chance) {
                return
            }

            val carried_by = obj.carried_by
            if (carried_by != null) {
                act(msg, carried_by, obj, null, TO_ALL)
            } else {
                val in_room = obj.in_room
                if (in_room != null) {
                    act(msg, in_room, obj, null, TO_ALL)
                }
            }

            extract_obj(obj)
        }
    }

}

fun sand_effect(vo: Any, level: Int, dam: Int, target: Int) {
    when (target) {
    /* nail objects on the floor */
        TARGET_ROOM -> (vo as ROOM_INDEX_DATA).objects.forEach { obj -> sand_effect(obj, level, dam, TARGET_OBJ) }
        TARGET_CHAR -> { /* do the effect on a victim */
            val victim = vo as CHAR_DATA

            if (!IS_AFFECTED(victim, AFF_BLIND) && !saves_spell(level / 4 + dam / 20, victim, DT.Cold)) {
                act("\$n is blinded by flying sands!", victim, null, null, TO_ROOM)
                act("Your eyes tear up from sands...you can't see a thing!", victim, null, null, TO_CHAR)

                val af = Affect()
                af.type = Skill.sand_storm
                af.level = level
                af.duration = number_range(0, level / 10)
                af.location = Apply.Hitroll
                af.modifier = -4
                af.bitvector = AFF_BLIND

                affect_to_char(victim, af)
            }

            /* let's toast some gear */
            victim.carrying.forEach { obj -> sand_effect(obj, level, dam, TARGET_OBJ) }
        }
        TARGET_OBJ -> { /* toast an object */
            val obj = vo as Obj

            if (IS_OBJ_STAT(obj, ITEM_BURN_PROOF)
                    || IS_OBJ_STAT(obj, ITEM_NOPURGE)
                    || obj.pIndexData.limit != -1
                    || number_range(0, 4) == 0) {
                return
            }

            var chance = level / 4 + dam / 10

            if (chance > 25) {
                chance = (chance - 25) / 2 + 25
            }
            if (chance > 50) {
                chance = (chance - 50) / 2 + 50
            }

            if (IS_OBJ_STAT(obj, ITEM_BLESS)) {
                chance -= 5
            }

            chance -= obj.level * 2

            val msg: String
            when (obj.item_type) {
                ItemType.Container, ItemType.CorpsePc, ItemType.CorpseNpc -> {
                    chance += 50
                    msg = "\$p is filled with sand and evaporates."
                }
                ItemType.Armor -> {
                    chance -= 10
                    msg = "\$p is etched by sand"
                }
                ItemType.Clothing -> msg = "\$p is corroded by sands."
                ItemType.Wand -> {
                    chance = 50
                    msg = "\$p mixes with crashing sands."
                }
                ItemType.Scroll -> {
                    chance += 20
                    msg = "\$p is surrouned by sand."
                }
                ItemType.Potion -> {
                    chance += 10
                    msg = "\$p is broken into peace by crashing sands."
                }
                else -> return
            }

            chance = URANGE(5, chance, 95)

            if (randomPercent() > chance) {
                return
            }

            val carried_by = obj.carried_by
            val in_room = obj.in_room
            if (carried_by != null) {
                act(msg, carried_by, obj, null, TO_ALL)
            } else if (in_room != null) {
                act(msg, in_room, obj, null, TO_ALL)
            }

            if (obj.item_type == ItemType.Armor) { /* etch it */
                var af_found = false
                affect_enchant(obj)
                var paf = obj.affected
                while (paf != null) {
                    if (paf.location == Apply.Ac) {
                        af_found = true
                        paf.type = Skill.general_purpose
                        paf.modifier += 1
                        paf.level = Math.max(paf.level, level)
                        break
                    }
                    paf = paf.next
                }

                if (!af_found) { /* needs a new affect */
                    paf = Affect()
                    paf.type = Skill.general_purpose
                    paf.level = level
                    paf.duration = level
                    paf.location = Apply.Ac
                    paf.modifier = 1
                    paf.next = obj.affected
                    obj.affected = paf
                }

                if (carried_by != null && obj.wear_loc.isOn()) {
                    for (i in 0..3) carried_by.armor[i] += 1
                }
                return
            }

            /* get rid of the object */
            loop@ for (t_obj in obj.contains) {
                obj_from_obj(t_obj)
                when {
                    in_room != null -> obj_to_room(t_obj, in_room)
                    carried_by != null -> obj_to_room(t_obj, carried_by.room)
                    else -> {
                        extract_obj(t_obj)
                        continue@loop
                    }
                }

                sand_effect(t_obj, level / 2, dam / 2, TARGET_OBJ)
            }
            extract_obj(obj)
        }
    }
}

fun scream_effect(vo: Any, level: Int, dam: Int, target: Int) {
    when (target) {
        TARGET_ROOM -> (vo as ROOM_INDEX_DATA).objects.forEach { scream_effect(it, level, dam, TARGET_OBJ) }
        TARGET_CHAR -> {
            val victim = vo as CHAR_DATA
            if (!saves_spell(level / 4 + dam / 20, victim, DT.Sound)) {
                act("\$n can't hear anything!", victim, null, null, TO_ROOM)
                act("You can't hear a thing!", victim, null, null, TO_CHAR)

                val af = Affect()
                af.type = Skill.scream
                af.level = level
                af.bitvector = AFF_SCREAM
                affect_to_char(victim, af)
            }

            /* daze and confused? */
            if (!saves_spell(level / 4 + dam / 20, victim, DT.Sound)) {
                send_to_char("You can't hear anything!.\n", victim)
                DAZE_STATE(victim, Math.max(12, level / 4 + dam / 20))
            }

            /* getting thirsty */
            if (victim.pcdata != null) {
                gain_condition(victim, COND_THIRST, dam / 20)
            }

            /* let's toast some gear! */
            victim.carrying.forEach { scream_effect(it, level, dam, TARGET_OBJ) }
            return
        }
        TARGET_OBJ -> /* toast an object */ {
            val obj = vo as Obj
            if (IS_OBJ_STAT(obj, ITEM_BURN_PROOF)
                    || IS_OBJ_STAT(obj, ITEM_NOPURGE)
                    || number_range(0, 4) == 0) {
                return
            }

            var chance = level / 4 + dam / 10

            if (chance > 25) {
                chance = (chance - 25) / 2 + 25
            }
            if (chance > 50) {
                chance = (chance - 50) / 2 + 50
            }

            if (IS_OBJ_STAT(obj, ITEM_BLESS)) {
                chance -= 5
            }
            chance -= obj.level * 2
            val msg: String
            when {
                check_material(obj, "ice") -> {
                    chance += 30
                    msg = "\$p breaks and evaporates!"
                }
                check_material(obj, "glass") -> {
                    chance += 30
                    msg = "\$p breaks into tiny small peaces"
                }
                else -> when (obj.item_type) {
                    ItemType.Potion -> {
                        chance += 25
                        msg = "Vial of \$p breaks and liquid spoils!"
                    }
                    ItemType.Scroll -> {
                        chance += 50
                        msg = "\$p breaks into tiny peaces!"
                    }
                    ItemType.DrinkContainer -> {
                        msg = "\$p breaks and liquid spoils!"
                        chance += 5
                    }
                    ItemType.Pill -> msg = "\$p breaks into peaces!"
                    else -> return
                }
            }

            chance = URANGE(5, chance, 95)

            if (randomPercent() > chance) {
                return
            }

            val carried_by = obj.carried_by
            val in_room = obj.in_room
            if (carried_by != null) {
                act(msg, carried_by, obj, null, TO_ALL)
            } else if (in_room != null) {
                act(msg, in_room, obj, null, TO_ALL)
            }


            loop@ for (t_obj in obj.contains) {
                obj_from_obj(t_obj)
                when {
                    in_room != null -> obj_to_room(t_obj, in_room)
                    carried_by != null -> obj_to_room(t_obj, carried_by.room)
                    else -> {
                        extract_obj(t_obj)
                        continue@loop
                    }
                }
                scream_effect(t_obj, level / 2, dam / 2, TARGET_OBJ)
            }
            extract_obj(obj)
        }
    }
}
