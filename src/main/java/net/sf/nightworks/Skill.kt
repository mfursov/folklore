package net.sf.nightworks

import net.sf.nightworks.act.spell.spell_absorb
import net.sf.nightworks.act.spell.spell_acid_breath
import net.sf.nightworks.act.spell.spell_animate_object
import net.sf.nightworks.act.spell.spell_astral_walk
import net.sf.nightworks.act.spell.spell_corruption
import net.sf.nightworks.act.spell.spell_detect_undead
import net.sf.nightworks.act.spell.spell_disenchant_armor
import net.sf.nightworks.act.spell.spell_disenchant_weapon
import net.sf.nightworks.act.spell.spell_disintegrate
import net.sf.nightworks.act.spell.spell_drain
import net.sf.nightworks.act.spell.spell_earthfade
import net.sf.nightworks.act.spell.spell_earthmaw
import net.sf.nightworks.act.spell.spell_fire_and_ice
import net.sf.nightworks.act.spell.spell_fire_breath
import net.sf.nightworks.act.spell.spell_firestream
import net.sf.nightworks.act.spell.spell_frost_breath
import net.sf.nightworks.act.spell.spell_frostbolt
import net.sf.nightworks.act.spell.spell_fumble
import net.sf.nightworks.act.spell.spell_gas_breath
import net.sf.nightworks.act.spell.spell_grounding
import net.sf.nightworks.act.spell.spell_hand_of_undead
import net.sf.nightworks.act.spell.spell_holy_word
import net.sf.nightworks.act.spell.spell_hurricane
import net.sf.nightworks.act.spell.spell_lightning_breath
import net.sf.nightworks.act.spell.spell_lightning_shield
import net.sf.nightworks.act.spell.spell_mist_walk
import net.sf.nightworks.act.spell.spell_poison
import net.sf.nightworks.act.spell.spell_shocking_trap
import net.sf.nightworks.act.spell.spell_soften
import net.sf.nightworks.act.spell.spell_solar_flight
import net.sf.nightworks.act.spell.spell_stone_skin
import net.sf.nightworks.act.spell.spell_summon
import net.sf.nightworks.act.spell.spell_summon_air_elm
import net.sf.nightworks.act.spell.spell_summon_earth_elm
import net.sf.nightworks.act.spell.spell_summon_fire_elm
import net.sf.nightworks.act.spell.spell_summon_light_elm
import net.sf.nightworks.act.spell.spell_summon_water_elm
import net.sf.nightworks.act.spell.spell_teleport
import net.sf.nightworks.act.spell.spell_tsunami
import net.sf.nightworks.act.spell.spell_ventriloquate
import net.sf.nightworks.act.spell.spell_weaken
import net.sf.nightworks.act.spell.spell_windwall
import net.sf.nightworks.act.spell.spell_word_of_recall
import net.sf.nightworks.model.Align
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.SG
import net.sf.nightworks.util.startsWith

typealias SpellFun = (level: Int, ch: CHAR_DATA, vo: Any, target: Int) -> Unit

private fun SLOT(n: Int) = n

enum class Skill(
        private val spellFun: SpellFun?,

        /** Legal targets */
        val target: Int,

        /** Slot for #OBJECT loading */
        val slot: Int,

        /**  Minimum mana used */
        val min_mana: Int,

        /** Waiting time after use */
        val beats: Int,

        /** Damage message */
        val noun_damage: String,

        /** Wear off message */
        val msg_off: String,

        /**  Wear off message for obects */
        val msg_obj: String,

        /** Skill group for practicing */
        val group: SG = SG.None,

        /** Position for caster / user */
        val minimum_position: Pos = Pos.Standing,

        /** Clan spells */
        val cabal: Clan = Clan.None,

        /** Alignment of spells */
        val align: Align? = null,

        /** Race spells */
        val race: Race? = null,

        /** Level needed by class */
        val skill_level: IntArray = IntArray(MAX_CLASS, { _ -> 100 }),

        /** How hard it is to learn */
        val rating: IntArray = IntArray(MAX_CLASS),

        var mod: IntArray = IntArray(MAX_CLASS)
) {

    reserved(null, TAR_IGNORE, SLOT(0), 0, 0, "", "", ""),

    /*
     * Magic spells.
    */
    absorb({ l, ch, _, _ -> spell_absorb(l, ch) }, TAR_CHAR_SELF, SLOT(707), 100, 12,
            "", "The energy field around you fades!", "\$p's energy field fades."),

    acetum_primus({ l, ch, vo, _ -> spell_acetum_primus(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(654), 20, 12, "acetum primus", "!acetum primus!", "", SG.Combat, Pos.Fighting),

    acid_arrow({ l, ch, vo, _ -> spell_acid_arrow(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(644), 20, 12, "acid arrow", "!Acid Arrow!", "", SG.Combat, Pos.Fighting),

    acid_blast({ l, ch, vo, _ -> spell_acid_blast(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(70), 40, 12, "acid blast", "!Acid Blast!", "", SG.Combat, Pos.Fighting),

    acute_vision({ l, ch, vo, _ -> spell_acute_vision(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(514), 10, 0, "", "Your vision seems duller.", ""),

    adamantite_golem({ l, ch, _, _ -> spell_adamantite_golem(l, ch) },
            TAR_IGNORE, SLOT(665), 500, 30, "", "You gained enough mana to make more golems now.", ""),

    aid({ l, ch, vo, _ -> spell_aid(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(680), 100, 12, "", "You can aid more people.", "", SG.Healing, Pos.Fighting),

    amnesia({ _, _, vo, _ -> spell_amnesia(vo as CHAR_DATA) }, TAR_CHAR_OFFENSIVE, SLOT(538), 100, 12, "", "!amnesia!", ""),

    animate_dead({ l, ch, vo, target -> spell_animate_dead(l, ch, vo, target) },
            TAR_OBJ_CHAR_OFF, SLOT(581), 50, 12, "", "You gain energy to animate new deads.", ""),

    animate_object({ l, ch, vo, _ -> spell_animate_object(l, ch, vo as Obj) },
            TAR_OBJ_CHAR_OFF, SLOT(709), 50, 12, "", "You gain energy to animate new objects.", ""),

    armor({ l, ch, vo, _ -> spell_armor(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(1), 5, 12, "", "You feel less armored.", "", SG.Protective),

    assist({ l, ch, vo, _ -> spell_assist(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(670), 100, 12, "", "You can assist more, now.", "", SG.Healing, Pos.Fighting),

    astral_walk({ l, ch, _, _ -> spell_astral_walk(l, ch) },
            TAR_IGNORE, SLOT(622), 80, 12, "", "!Astral Walk!", "", SG.Transportation, Pos.Fighting),

    attract_other({ l, ch, vo, _ -> spell_attract_other(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(580), 5, 12, "", "You feel your master leaves you.", "", SG.Beguiling),

    bark_skin({ l, ch, vo, _ -> spell_bark_skin(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(515), 40, 0, "", "The bark on your skin flakes off.", ""),

    black_death({ l, ch, _, _ -> spell_black_death(l, ch) },
            TAR_IGNORE, SLOT(677), 200, 24, "", "!black death!", "", SG.Maladictions),

    blade_barrier({ l, ch, vo, _ -> spell_blade_barrier(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(679), 40, 12, "blade barrier", "!Blade Barrier!", "", SG.Attack, Pos.Fighting),

    bless({ l, ch, vo, target -> spell_bless(l, ch, vo, target) },
            TAR_OBJ_CHAR_DEF, SLOT(3), 5, 12, "", "You feel less righteous.", "\$p's holy aura fades.", SG.Benedictions),

    bless_weapon({ l, ch, vo, _ -> spell_bless_weapon(l, ch, vo as Obj) },
            TAR_OBJ_INV, SLOT(637), 100, 24, "", "!Bless Weapon!", "", SG.Enchantment),

    blindness({ l, ch, vo, _ -> spell_blindness(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(4), 5, 12, "", "You can see again.", "", SG.Maladictions, Pos.Fighting),

    bluefire({ l, ch, vo, _ -> spell_bluefire(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(660), 20, 12, "torments", "!Bluefire!", "", SG.Attack, Pos.Fighting),

    burning_hands({ l, ch, vo, _ -> spell_burning_hands(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(5), 15, 12, "burning hands", "!Burning Hands!", "", SG.Combat, Pos.Fighting),

    call_lightning({ l, ch, _, _ -> spell_call_lightning(l, ch) },
            TAR_IGNORE, SLOT(6), 15, 12, "lightning bolt", "!Call Lightning!", "", SG.Weather, Pos.Fighting),

    calm({ l, ch, _, _ -> spell_calm(l, ch) }, TAR_IGNORE, SLOT(509), 30, 12, "",
            "You have lost your peace of mind.", "", SG.Benedictions, Pos.Fighting),

    cancellation({ l, ch, vo, _ -> spell_cancellation(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(507), 20, 12, "", "!cancellation!", "", SG.Protective, Pos.Fighting),

    cause_critical({ l, ch, vo, _ -> spell_cause_critical(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(63), 20, 12, "spell", "!Cause Critical!", "", SG.Harmful, Pos.Fighting),

    cause_light({ l, ch, vo, _ -> spell_cause_light(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(62), 15, 12, "spell", "!Cause Light!", "", SG.Harmful, Pos.Fighting),

    cause_serious({ l, ch, vo, _ -> spell_cause_serious(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(64), 17, 12, "spell", "!Cause Serious!", "", SG.Harmful, Pos.Fighting),

    caustic_font({ l, ch, vo, _ -> spell_caustic_font(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(653), 20, 12, "caustic font", "!caustic font!", "", SG.Combat, Pos.Fighting),

    chain_lightning({ l, ch, vo, _ -> spell_chain_lightning(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(500), 25, 12, "lightning", "!Chain Lightning!", "", SG.Combat, Pos.Fighting),

    charm_person({ l, ch, vo, _ -> spell_charm_person(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(7), 5, 12, "", "You feel more self-confident.", "", SG.Beguiling),

    chromatic_orb({ l, ch, vo, _ -> spell_chromatic_orb(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(714), 50, 12, "chromatic orb", "!Chromatic Orb!", "", SG.Combat, Pos.Fighting),

    control_undead({ l, ch, vo, _ -> spell_control_undead(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(669), 20, 12, "", "You feel more self confident.", ""),

    chill_touch({ l, ch, vo, _ -> spell_chill_touch(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(8), 15, 12, "chilling touch", "You feel less cold.", "", SG.Combat, Pos.Fighting),

    colour_spray({ l, ch, vo, _ -> spell_colour_spray(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(10), 15, 12, "colour spray", "!Colour Spray!", "", SG.Combat, Pos.Fighting),

    continual_light({ _, ch, _, _ -> spell_continual_light(ch) },
            TAR_IGNORE, SLOT(57), 7, 12, "", "!Continual Light!", "", SG.Creation),

    control_weather({ l, ch, _, _ -> spell_control_weather(l, ch) },
            TAR_IGNORE, SLOT(11), 25, 12, "", "!Control Weather!", "", SG.Weather),

    corruption({ l, ch, vo, _ -> spell_corruption(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(671), 20, 12, "corruption", "You feel yourself healthy again.", "", SG.None, Pos.Fighting),

    create_food({ l, ch, _, _ -> spell_create_food(l, ch) }, TAR_IGNORE, SLOT(12), 5, 12, "", "!Create Food!", "", SG.Creation),

    create_rose({ _, ch, _, _ -> spell_create_rose(ch) }, TAR_IGNORE, SLOT(511), 30, 12, "", "!Create Rose!", "", SG.Creation),

    create_spring({ l, ch, _, _ -> spell_create_spring(l, ch) },
            TAR_IGNORE, SLOT(80), 20, 12, "", "!Create Spring!", "", SG.Creation),

    create_water({ l, ch, vo, _ -> spell_create_water(l, ch, vo as Obj) },
            TAR_OBJ_INV, SLOT(13), 5, 12, "", "!Create Water!", "", SG.Creation),

    cure_blindness({ l, ch, vo, _ -> spell_cure_blindness(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(14), 5, 12, "", "!Cure Blindness!", "", SG.Curative, Pos.Fighting),

    cure_critical({ l, ch, vo, _ -> spell_cure_critical(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(15), 20, 12, "", "!Cure Critical!", "", SG.Healing, Pos.Fighting),

    cure_disease({ l, ch, vo, _ -> spell_cure_disease(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(501), 20, 12, "", "!Cure Disease!", "", SG.Curative),

    cure_light({ l, ch, vo, _ -> spell_cure_light(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(16), 10, 12, "", "!Cure Light!", "", SG.Healing, Pos.Fighting),

    cure_poison({ l, ch, vo, _ -> spell_cure_poison(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(43), 5, 12, "", "!Cure Poison!", "", SG.Curative),

    cure_serious({ l, ch, vo, _ -> spell_cure_serious(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(61), 15, 12, "", "!Cure Serious!", "", SG.Healing, Pos.Fighting),

    curse({ l, ch, vo, target -> spell_curse(l, ch, vo, target) },
            TAR_OBJ_CHAR_OFF, SLOT(17), 20, 12, "curse", "The curse wears off.", "\$p is no longer impure.", SG.Maladictions, Pos.Fighting),

    cursed_lands({ l, ch, _, _ -> spell_cursed_lands(l, ch) },
            TAR_IGNORE, SLOT(675), 200, 24, "", "!cursed lands!", "", SG.Maladictions),

    deadly_venom({ l, ch, _, _ -> spell_deadly_venom(l, ch) },
            TAR_IGNORE, SLOT(674), 200, 24, "", "!deadly venom!", "", SG.Maladictions),

    deafen({ l, ch, vo, _ -> spell_deafen(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(570), 40, 12, "deafen", "The ringing in your ears finally stops.", "", SG.None, Pos.Fighting),

    demonfire({ l, ch, vo, _ -> spell_demonfire(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(505), 20, 12, "torments", "!Demonfire!", "", SG.Attack, Pos.Fighting),

    desert_fist({ l, ch, vo, _ -> spell_desert_fist(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(681), 50, 12, "desert fist", "!desert fist!", "", SG.None, Pos.Fighting),

    detect_evil({ l, ch, vo, _ -> spell_detect_evil(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(18), 5, 12, "", "The red in your vision disappears.", "", SG.Detection),

    detect_good({ l, ch, vo, _ -> spell_detect_good(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(513), 5, 12, "", "The gold in your vision disappears.", "", SG.Detection),

    detect_hide({ l, ch, vo, _ -> spell_detect_hidden(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(0), 5, 12, "", "You feel less aware of your surroundings.", "", SG.Detection),

    detect_invis({ l, ch, vo, _ -> spell_detect_invis(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(19), 5, 12, "", "You no longer see invisible objects.", "", SG.Detection),

    detect_magic({ l, ch, vo, _ -> spell_detect_magic(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(20), 5, 12, "", "The detect magic wears off.", "", SG.Detection),

    detect_poison({ _, ch, vo, _ -> spell_detect_poison(ch, vo as Obj) },
            TAR_OBJ_INV, SLOT(21), 5, 12, "", "!Detect Poison!", "", SG.Detection),

    detect_undead({ l, ch, vo, _ -> spell_detect_undead(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(594), 5, 12, "", "You can't detect undeads anymore.", "", SG.Detection),

    disenchant_armor({ l, ch, vo, _ -> spell_disenchant_armor(l, ch, vo as Obj) },
            TAR_OBJ_INV, SLOT(705), 50, 24, "", "!disenchant armor!", ""),

    disenchant_weapon({ l, ch, vo, _ -> spell_disenchant_weapon(l, ch, vo as Obj) },
            TAR_OBJ_INV, SLOT(706), 50, 24, "", "!disenchant weapon!", ""),

    disintegrate({ l, ch, vo, _ -> spell_disintegrate(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(574), 100, 18, "thin light ray", "!disintegrate!", ""),

    /*TODO: dismantle(            "dismantle", Magic2::spell_dismantle,
            TAR_CHAR_SELF, Pos.Standing, SLOT(621), 200, 24, "", "!621!", "", Clan.None, null, ALIGN_NONE),*/

    dispel_evil({ l, ch, vo, _ -> spell_dispel_evil(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(22), 15, 12, "dispel evil", "!Dispel Evil!", "", SG.Attack, Pos.Fighting),

    dispel_good({ l, ch, vo, _ -> spell_dispel_good(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(512), 15, 12, "dispel good", "!Dispel Good!", "", SG.Attack, Pos.Fighting),

    dispel_magic({ l, ch, vo, _ -> spell_dispel_magic(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(59), 15, 12, "", "!Dispel Magic!", "", SG.Protective, Pos.Fighting),

    disruption({ l, ch, vo, _ -> spell_disruption(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(648), 20, 12, "disruption", "!disruption!", "", SG.Combat, Pos.Fighting),

    dragon_breath({ l, ch, vo, _ -> spell_dragon_breath(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(563), 75, 12, "blast of fire", "!dragon breath!", "", SG.None, Pos.Fighting),

    dragon_skin({ l, ch, vo, _ -> spell_dragon_skin(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(612), 50, 24, "", "Your skin becomes softer.", "", SG.Protective),

    dragon_strength({ l, ch, _, _ -> spell_dragon_strength(l, ch) },
            TAR_CHAR_SELF, SLOT(562), 75, 12, "", "You feel the strength of the dragon leave you.", "", SG.None, Pos.Fighting),

    dragons_breath({ l, ch, vo, _ -> spell_dragons_breath(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(576), 200, 24, "dragon breath", "Your get healtier again.", "", SG.Draconian, Pos.Fighting),

    drain({ _, ch, vo, _ -> spell_drain(ch, vo as Obj) }, TAR_OBJ_INV, SLOT(704), 5, 12, "", "!drain!", "", SG.Maladictions),

    earthfade({ l, ch, _, _ -> spell_earthfade(l, ch) },
            TAR_CHAR_OFFENSIVE, SLOT(702), 100, 12, "", "You slowly fade to your neutral form.", "", SG.None, Pos.Fighting),

    earthmaw({ l, ch, vo, _ -> spell_earthmaw(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(703), 30, 12, "earthmaw", "!earthmaw!", "", SG.None, Pos.Fighting),

    earthquake({ l, ch, _, _ -> spell_earthquake(l, ch) },
            TAR_IGNORE, SLOT(23), 15, 12, "earthquake", "!Earthquake!", "", SG.Attack, Pos.Fighting),

    elemental_sphere({ l, ch, vo, _ -> spell_elemental_sphere(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(719), 75, 12, "", "The protecting elemental sphere around you fades.", "", SG.None),

    enchant_armor({ l, ch, vo, _ -> spell_enchant_armor(l, ch, vo as Obj) },
            TAR_OBJ_INV, SLOT(510), 100, 24, "", "!Enchant Armor!", "", SG.Enchantment),

    enchant_weapon({ l, ch, vo, _ -> spell_enchant_weapon(l, ch, vo as Obj) },
            TAR_OBJ_INV, SLOT(24), 100, 24, "", "!Enchant Weapon!", "", SG.Enchantment),

    energy_drain({ l, ch, vo, _ -> spell_energy_drain(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(25), 35, 12, "energy drain", "!Energy Drain!", "", SG.Maladictions, Pos.Fighting),

    enhanced_armor({ l, ch, vo, _ -> spell_enhanced_armor(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(583), 20, 12, "", "You feel yourself unprotected.", "", SG.Protective, Pos.Fighting),

    enlarge({ l, ch, vo, _ -> spell_enlarge(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(711), 50, 12, "", "You return to your orginal size.", ""),

    etheral_fist({ l, ch, vo, _ -> spell_etheral_fist(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(645), 20, 12, "etheral fist", "!Etheral Fist!", "", SG.Combat, Pos.Fighting),

    faerie_fire({ l, _, vo, _ -> spell_faerie_fire(l, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(72), 5, 12, "faerie fire", "The pink aura around you fades away.", "", SG.Weather, Pos.Fighting),

    faerie_fog({ l, ch, _, _ -> spell_faerie_fog(l, ch) },
            TAR_IGNORE, SLOT(73), 12, 12, "faerie fog", "!Faerie Fog!", "", SG.Weather),

    farsight({ _, ch, _, _ -> spell_farsight(ch) }, TAR_IGNORE, SLOT(521), 20, 12,
            "farsight", "!Farsight!", "", SG.Detection),

    fear({ l, ch, vo, _ -> spell_fear(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(598), 50, 12, "", "You feel more brave.", "", SG.Illusion),

    fire_and_ice({ l, ch, _, _ -> spell_fire_and_ice(l, ch) },
            TAR_CHAR_OFFENSIVE, SLOT(699), 40, 12, "fire and ice", "!fire and ice!", "", SG.Combat, Pos.Fighting),

    fireball({ l, ch, _, _ -> spell_fireball(l, ch) },
            TAR_IGNORE, SLOT(26), 25, 12, "fireball", "!Fireball!", "", SG.Combat, Pos.Fighting),

    fireproof({ l, ch, vo, _ -> spell_fireproof(l, ch, vo as Obj) },
            TAR_OBJ_INV, SLOT(523), 10, 12, "", "", "\$p's protective aura fades.", SG.Enchantment),

    firestream({ l, ch, vo, _ -> spell_firestream(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(692), 20, 12, "", "", "", SG.Combat, Pos.Fighting),

    fire_shield({ _, ch, _, _ -> spell_fire_shield(ch) }, TAR_IGNORE, SLOT(601), 200, 24, "", "!fire shield!", ""),

    flamestrike({ l, ch, vo, _ -> spell_flamestrike(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(65), 20, 12, "flamestrike", "!Flamestrike!", "", SG.Attack, Pos.Fighting),

    fly({ l, ch, vo, _ -> spell_fly(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(56), 10, 18, "", "You slowly float to the ground.", "", SG.Transportation),

    floating_disc({ l, ch, _, _ -> spell_floating_disc(l, ch) },
            TAR_IGNORE, SLOT(522), 40, 24, "", "!Floating disc!", ""),

    forcecage({ l, ch, vo, _ -> spell_forcecage(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(717), 75, 12, "", "The forcecage around you fades.", ""),

    frenzy({ l, ch, vo, _ -> spell_frenzy(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(504), 30, 24, "", "Your rage ebbs.", "", SG.Benedictions),

    frostbolt({ l, ch, vo, _ -> spell_frostbolt(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(695), 20, 12, "frostbolt", "!frostbolt!", "", SG.Combat, Pos.Fighting),

    fumble({ l, ch, vo, _ -> spell_fumble(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(712), 25, 18, "", "You speed up and regain your strength!", "", SG.None, Pos.Fighting),

    galvanic_whip({ l, ch, vo, _ -> spell_galvanic_whip(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(655), 20, 12, "galvanic whip", "!galvanic whip!", "", SG.Combat, Pos.Fighting),

    gate({ l, ch, _, _ -> spell_gate(l, ch) },
            TAR_IGNORE, SLOT(83), 80, 12, "", "!Gate!", "", SG.Transportation, Pos.Fighting),

    giant_strength({ l, ch, vo, _ -> spell_giant_strength(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(39), 20, 12, "", "You feel weaker.", "", SG.Enhancement),

    grounding({ l, ch, vo, _ -> spell_grounding(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(700), 50, 12, "", "You lost your grounding against electricity", "", SG.None, Pos.Fighting),

    group_defense({ l, ch, _, _ -> spell_group_defense(l, ch) },
            TAR_IGNORE, SLOT(586), 100, 36, "", "You feel less protected.", "", SG.Benedictions),

    group_heal({ l, ch, _, _ -> spell_group_heal(l, ch) },
            TAR_CHAR_DEFENSIVE, SLOT(642), 500, 24, "", "!Group Heal!", "", SG.Healing, Pos.Fighting),

    hand_of_undead({ l, ch, vo, _ -> spell_hand_of_undead(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(647), 20, 24, "hand of undead", "!hand of undead!", "", SG.Combat, Pos.Fighting),

    harm({ l, ch, vo, _ -> spell_harm(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(27), 35, 12, "harm spell", "!Harm!", "", SG.Harmful, Pos.Fighting),

    haste({ l, ch, vo, _ -> spell_haste(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(502), 30, 12, "", "You feel yourself slow down.", "", SG.Enhancement, Pos.Fighting),

    heal({ l, ch, vo, _ -> spell_heal(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(28), 50, 12, "", "!Heal!", "", SG.Healing, Pos.Fighting),

    healing_light({ l, ch, _, _ -> spell_healing_light(l, ch) },
            TAR_IGNORE, SLOT(613), 200, 24, "", "You can light more rooms now.", "", SG.Benedictions),

    heat_metal({ l, ch, vo, _ -> spell_heat_metal(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(516), 25, 18, "spell", "!Heat Metal!", "", SG.None, Pos.Fighting),

    hellfire({ l, ch, vo, _ -> spell_hellfire(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(520), 20, 12, "hellfire", "!hellfire!", "", SG.Attack, Pos.Fighting),

    holy_aura({ l, ch, vo, _ -> spell_holy_aura(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(617), 75, 12, "", "Your holy aura vanishes.", ""),

    holy_fury({ l, ch, vo, _ -> spell_holy_fury(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(682), 50, 24, "", "You become more tolerable.", ""),

    holy_word({ l, ch, _, _ -> spell_holy_word(l, ch) },
            TAR_IGNORE, SLOT(506), 200, 24, "divine wrath", "!Holy Word!", "", SG.Benedictions, Pos.Fighting),

    hurricane({ l, ch, _, _ -> spell_hurricane(l, ch) },
            TAR_IGNORE, SLOT(672), 200, 24, "helical flow", "!Hurricane!", "", SG.None, Pos.Fighting),

    hydroblast({ l, ch, vo, _ -> spell_hydroblast(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(684), 50, 12, "water fist", "!Hydroblast!", "", SG.None, Pos.Fighting),

    iceball({ l, ch, _, _ -> spell_iceball(l, ch) },
            TAR_IGNORE, SLOT(513), 25, 12, "iceball", "!Iceball!", "", SG.Combat, Pos.Fighting),

    identify({ _, ch, vo, _ -> spell_identify(ch, vo as Obj) },
            TAR_OBJ_INV, SLOT(53), 12, 24, "", "!Identify!", "", SG.Detection),

    improved_detect({ l, ch, vo, _ -> spell_improved_detection(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(626), 20, 12, "", "You feel less aware of your surroundings.", "", SG.Detection),

    improved_invis({ l, _, vo, _ -> spell_improved_invis(l, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(627), 20, 12, "", "You are no longer invisible.", "\$p fades into view.", SG.Illusion),

    infravision({ l, ch, vo, _ -> spell_infravision(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(77), 5, 18, "", "You no longer see in the dark.", "", SG.Enhancement),

    insanity({ l, ch, vo, _ -> spell_insanity(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(616), 100, 24, "", "Now you feel yourself calm down.", ""),

    inspire({ l, ch, _, _ -> spell_inspire(l, ch) },
            TAR_IGNORE, SLOT(587), 75, 24, "", "You feel less inspired", "", SG.Benedictions),

    invisibility({ l, ch, vo, target -> spell_invis(l, ch, vo, target) },
            TAR_OBJ_CHAR_DEF, SLOT(29), 5, 12, "", "You are no longer invisible.", "\$p fades into view.", SG.Illusion),

    iron_body({ l, ch, vo, _ -> spell_iron_body(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(718), 75, 12, "", "The skin regains its softness.", ""),

    iron_golem({ l, ch, _, _ -> spell_iron_golem(l, ch) },
            TAR_IGNORE, SLOT(664), 400, 24, "", "You gained enough mana to make more golems now.", ""),

    knock({ _, ch, _, _ -> spell_knock(ch) }, TAR_IGNORE, SLOT(603), 20, 24, "", "!knock!", ""),

    know_alignment({ _, ch, vo, _ -> spell_know_alignment(ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(58), 9, 12, "", "!Know Alignment!", "", SG.Detection, Pos.Fighting),

    lesser_golem({ l, ch, _, _ -> spell_lesser_golem(l, ch) },
            TAR_IGNORE, SLOT(662), 200, 12, "", "You gained enough mana to make more golems now.", ""),

    lethargic_mist({ l, ch, _, _ -> spell_lethargic_mist(l, ch) },
            TAR_IGNORE, SLOT(676), 200, 24, "", "!lethargic mist!", "", SG.Maladictions),

    light_arrow({ l, ch, vo, _ -> spell_light_arrow(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(683), 40, 12, "light arrow", "!light arrow!", "", SG.Combat, Pos.Fighting),

    lightning_bolt({ l, ch, vo, _ -> spell_lightning_bolt(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(30), 15, 12, "lightning bolt", "!Lightning Bolt!", "", SG.Combat, Pos.Fighting),

    lightning_shield({ l, ch, _, _ -> spell_lightning_shield(l, ch) },
            TAR_IGNORE, SLOT(614), 150, 24, "lightning shield", "Now you can shield your room again.", ""),

    link({ _, ch, vo, _ -> spell_link(ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(588), 125, 18, "", "!link!", "", SG.Meditation),

    lion_help({ l, ch, _, _ -> spell_lion_help(l, ch) },
            TAR_IGNORE, SLOT(595), 100, 12, "", "Once again, you may send a slayer lion.", ""),

    locate_object({ l, ch, _, _ -> spell_locate_object(l, ch) },
            TAR_IGNORE, SLOT(31), 20, 18, "", "!Locate Object!", "", SG.Detection),

    love_potion({ l, ch, _, _ -> spell_love_potion(l, ch) },
            TAR_CHAR_SELF, SLOT(666), 10, 0, "", "You feel less dreamy-eyed.", ""),

    magic_jar({ l, ch, vo, _ -> spell_magic_jar(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(596), 20, 12, "", "!magic jar!", "", SG.Beguiling, Pos.Fighting),

    magic_missile({ l, ch, vo, _ -> spell_magic_missile(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(32), 15, 12, "magic missile", "!Magic Missile!", "", SG.Combat, Pos.Fighting),

    magic_resistance({ l, ch, _, _ -> spell_magic_resistance(l, ch) },
            TAR_CHAR_SELF, SLOT(605), 200, 24, "", "You are again defenseless to magic.", "", SG.Protective),

    magnetic_trust({ l, ch, vo, _ -> spell_magnetic_trust(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(656), 20, 12, "magnetic trust", "!magnetic trust!", "", SG.Combat, Pos.Fighting),

    mass_healing({ l, ch, _, _ -> spell_mass_healing(l, ch) },
            TAR_IGNORE, SLOT(508), 100, 36, "", "!Mass Healing!", "", SG.Healing),

    mass_invis({ l, ch, _, _ -> spell_mass_invis(l, ch) },
            TAR_IGNORE, SLOT(69), 20, 24, "", "You are no longer invisible.", "", SG.Illusion),

    mass_sanctuary({ l, ch, _, _ -> spell_mass_sanctuary(l, ch) },
            TAR_IGNORE, SLOT(589), 200, 24, "", "The white aura around your body fades.", "", SG.Protective),

    master_healing({ l, ch, vo, _ -> spell_master_heal(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(641), 300, 12, "", "!Master Heal!", "", SG.Healing, Pos.Fighting),

    meld_into_stone({ l, ch, vo, _ -> spell_meld_into_stone(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(584), 12, 18, "", "The stones on your skin crumble into dust.", "", SG.Protective, Pos.Standing, Clan.None, null, Race.ROCKSEER),

    mend({ _, ch, vo, _ -> spell_mend(ch, vo as Obj) }, TAR_OBJ_INV, SLOT(590), 150, 24, "", "!mend!", "", SG.Enchantment),

    mind_light({ l, ch, _, _ -> spell_mind_light(l, ch) }, TAR_IGNORE, SLOT(82), 200, 24, "", "You can booster more rooms now.", ""),

    mind_wrack({ l, ch, vo, _ -> spell_mind_wrack(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(650), 20, 12, "mind wrack", "!mind wrack!", "", SG.Combat, Pos.Fighting),

    mind_wrench({ l, ch, vo, _ -> spell_mind_wrench(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(651), 20, 12, "mind wrench", "!mind wrench!", "", SG.Combat, Pos.Fighting),

    mist_walk({ l, ch, _, _ -> spell_mist_walk(l, ch) },
            TAR_IGNORE, SLOT(658), 80, 12, "", "!Mist Walk!", "", SG.Transportation, Pos.Fighting),

    mummify({ l, ch, vo, target -> spell_mummify(l, ch, vo, target) },
            TAR_OBJ_CHAR_OFF, SLOT(715), 50, 12, "", "You gain energy to give live to new corpses.", ""),

    mysterious_dream({ l, ch, _, _ -> spell_mysterious_dream(l, ch) },
            TAR_IGNORE, SLOT(678), 200, 24, "", "!mysterous dream!", "", SG.Beguiling),

    nexus({ l, ch, _, _ -> spell_nexus(l, ch) },
            TAR_IGNORE, SLOT(520), 150, 36, "", "!Nexus!", "", SG.Transportation),

    pass_door({ l, ch, vo, _ -> spell_pass_door(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(74), 20, 12, "", "You feel solid again.", "", SG.Transportation),

    plague({ l, ch, vo, _ -> spell_plague(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(503), 20, 12, "sickness", "Your sores vanish.", "", SG.Maladictions, Pos.Fighting),

    poison({ l, ch, vo, target -> spell_poison(l, ch, vo, target) },
            TAR_OBJ_CHAR_OFF, SLOT(33), 10, 12, "poison", "You feel less sick.", "The poison on \$p dries up.", SG.Maladictions, Pos.Fighting),

    polymorph({ l, ch, _, _ -> spell_polymorph(l, ch) },
            TAR_IGNORE, SLOT(639), 250, 24, "", "You return to your own race.", "", SG.Benedictions),

    portal({ l, ch, _, _ -> spell_portal(l, ch) }, TAR_IGNORE, SLOT(519), 100, 24, "", "!Portal!", "", SG.Transportation),

    protection_cold({ l, ch, vo, _ -> spell_protection_cold(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(600), 5, 12, "", "You feel less protected", "", SG.Protective),

    protection_evil({ l, ch, vo, _ -> spell_protection_evil(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(34), 5, 12, "", "You feel less protected.", "", SG.Protective),

    protection_good({ l, ch, vo, _ -> spell_protection_good(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(666), 5, 12, "", "You feel less protected.", "", SG.Protective),

    protection_heat({ l, ch, vo, _ -> spell_protection_heat(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(599), 5, 12, "", "You feel less protected", "", SG.Protective),

    protection_negative({ l, ch, _, _ -> spell_protection_negative(l, ch) },
            TAR_CHAR_SELF, SLOT(636), 20, 12, "", "You feel less protected from your own attacks.", "", SG.Protective),

    protective_shield({ l, ch, vo, _ -> spell_protective_shield(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(572), 70, 12, "", "Your shield fades away.", "", SG.Protective),

    power_word_kill({ l, ch, vo, _ -> spell_power_kill(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(604), 200, 18, "powerful word", "You gain back your durability.", ""),

    power_word_stun({ l, _, vo, _ -> spell_power_stun(l, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(625), 200, 24, "", "You can move now.", ""),

    quantum_spike({ l, ch, vo, _ -> spell_quantum_spike(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(657), 20, 12, "quantum spike", "!quantum spike!", "", SG.Combat, Pos.Fighting),

    ranger_staff({ l, ch, _, _ -> spell_ranger_staff(l, ch) },
            TAR_IGNORE, SLOT(519), 75, 0, "", "!ranger staff!", "", SG.None, Pos.Fighting),

    ray_of_truth({ l, ch, vo, _ -> spell_ray_of_truth(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(518), 20, 12, "ray of truth", "!Ray of Truth!", "", SG.Benedictions, Pos.Fighting),

    recharge({ l, ch, vo, _ -> spell_recharge(l, ch, vo as Obj) },
            TAR_OBJ_INV, SLOT(517), 60, 24, "", "!Recharge!", "", SG.Enchantment),

    refresh({ l, ch, vo, _ -> spell_refresh(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(81), 12, 18, "refresh", "!Refresh!", "", SG.Healing),

    /* TODO: reincarnation(Magic2::spell_reincarnation,
            TAR_IGNORE, Pos.Standing, SLOT(668), 0, 0, "", "!!", "", Clan.None, null, ALIGN_NONE),*/

    remove_curse({ l, ch, vo, target -> spell_remove_curse(l, ch, vo, target) },
            TAR_OBJ_CHAR_DEF, SLOT(35), 5, 12, "", "!Remove Curse!", "", SG.Curative),

    remove_fear({ l, ch, vo, _ -> spell_remove_fear(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(582), 5, 12, "", "!Remove Fear!", ""),

    remove_tattoo({ _, ch, vo, _ -> spell_remove_tattoo(ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(552), 10, 0, "", "!remove tattoo!", ""),

    resilience({ l, ch, _, _ -> spell_resilience(l, ch) },
            TAR_CHAR_DEFENSIVE, SLOT(638), 50, 12, "", "You feel less armored to draining attacks.", "", SG.Protective),

    restoring_light({ l, ch, vo, _ -> spell_restoring_light(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(643), 50, 24, "", "!restoring light!", "", SG.Benedictions),

    sanctify_lands({ _, ch, _, _ -> spell_sanctify_lands(ch) },
            TAR_IGNORE, SLOT(673), 200, 24, "", "!sanctify lands!", "", SG.Benedictions),

    sanctuary({ l, ch, vo, _ -> spell_sanctuary(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(36), 75, 12, "", "The white aura around your body fades.", "", SG.Protective),

    sand_storm({ l, ch, _, _ -> spell_sand_storm(l, ch) },
            TAR_IGNORE, SLOT(577), 200, 24, "storm of sand", "The sands melts in your eyes.", "", SG.None, Pos.Fighting),

    scream({ l, ch, _, _ -> spell_scream(l, ch) },
            TAR_IGNORE, SLOT(578), 200, 24, "scream", "You can hear again.", "", SG.None, Pos.Fighting),

    severity_force({ l, ch, vo, _ -> spell_severity_force(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(622), 20, 12, "severity force", "!severity force!", "", SG.None, Pos.Fighting),

    shield({ l, ch, vo, _ -> spell_shield(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(67), 12, 18, "", "Your force shield shimmers then fades away.", "", SG.Protective),

    shielding({ l, ch, vo, _ -> spell_shielding(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(591), 250, 12, "", "You feel the glow of the true source in the distance", ""),

    shocking_grasp({ l, ch, vo, _ -> spell_shocking_grasp(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(53), 15, 12, "shocking grasp", "!Shocking Grasp!", "", SG.Combat, Pos.Fighting),

    shocking_trap({ l, ch, _, _ -> spell_shocking_trap(l, ch) },
            TAR_IGNORE, SLOT(615), 150, 24, "shocking trap", "Now you can trap more rooms with shocks.", ""),

    sleep({ l, _, vo, _ -> spell_sleep(l, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(38), 15, 12, "", "You feel less tired.", "", SG.Beguiling),

    slow({ l, ch, vo, _ -> spell_slow(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(515), 30, 12, "", "You feel yourself speed up.", "", SG.Maladictions, Pos.Fighting),

    soften({ l, _, vo, _ -> spell_soften(l, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(713), 75, 12, "soften", "Your skin regains its robustness.", "", SG.Weather, Pos.Fighting),

    solar_flight({ l, ch, _, _ -> spell_solar_flight(l, ch) },
            TAR_IGNORE, SLOT(659), 80, 12, "", "!Solar Flight!", "", SG.Transportation, Pos.Fighting),

    sonic_resonance({ l, ch, vo, _ -> spell_sonic_resonance(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(649), 20, 12, "sonic resonance", "!sonic resonance!", "", SG.Combat, Pos.Fighting),

    soul_bind({ _, ch, vo, _ -> spell_soul_bind(ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(716), 5, 12, "", "You feel more self-confident.", "", SG.Beguiling),

    spectral_furor({ l, ch, vo, _ -> spell_spectral_furor(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(646), 20, 12, "spectral furor", "!spectral furor!", "", SG.Combat, Pos.Fighting),

    stone_golem({ l, ch, _, _ -> spell_stone_golem(l, ch) },
            TAR_IGNORE, SLOT(663), 300, 18, "", "You gained enough mana to make more golems now.", ""),

    stone_skin({ l, ch, vo, _ -> spell_stone_skin(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(66), 12, 18, "", "Your skin feels soft again.", "", SG.Protective),

    suffocate({ l, ch, vo, _ -> spell_suffocate(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(714), 50, 12, "breathlessness", "You can breath again.", "", SG.None, Pos.Fighting),

    sulfurus_spray({ l, ch, vo, _ -> spell_sulfurus_spray(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(652), 20, 12, "sulfurus spray", "!sulfurus spray!", "", SG.Combat, Pos.Fighting),

    summon({ l, ch, _, _ -> spell_summon(l, ch) }, TAR_IGNORE, SLOT(40), 50, 12, "", "!Summon!", "", SG.Transportation),

    summon_air_elemental({ l, ch, _, _ -> spell_summon_air_elm(l, ch) },
            TAR_IGNORE, SLOT(696), 50, 12, "", "You gain back the energy to summon another air elemental.", ""),

    summon_earth_elemental({ l, ch, _, _ -> spell_summon_earth_elm(l, ch) },
            TAR_IGNORE, SLOT(693), 50, 12, "", "You gain back the energy to summon another earth elemental.", ""),

    summon_fire_elemental({ l, ch, _, _ -> spell_summon_fire_elm(l, ch) },
            TAR_IGNORE, SLOT(697), 50, 12, "", "You gain back the energy to summon another fire elemental.", ""),

    summon_lightning_elemental({ l, ch, _, _ -> spell_summon_light_elm(l, ch) },
            TAR_IGNORE, SLOT(710), 50, 12, "", "You gain back the energy to summon another lightning elemental.", ""),

    summon_water_elemental({ l, ch, _, _ -> spell_summon_water_elm(l, ch) },
            TAR_IGNORE, SLOT(698), 50, 12, "", "You gain back the energy to summon another water elemental.", ""),

    summon_shadow({ l, ch, _, _ -> spell_summon_shadow(l, ch) },
            TAR_CHAR_SELF, SLOT(620), 200, 24, "", "You can summon more shadows, now.", ""),

    superior_heal({ l, ch, vo, _ -> spell_super_heal(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_DEFENSIVE, SLOT(640), 100, 12, "", "!Super Heal!", "", SG.Healing, Pos.Fighting),

    tattoo({ _, ch, vo, _ -> spell_tattoo(ch, vo as CHAR_DATA) }, TAR_CHAR_DEFENSIVE, SLOT(551), 10, 0, "", "!tattoo!", ""),

    teleport({ l, ch, vo, _ -> spell_teleport(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(2), 35, 12, "", "!Teleport!", "", SG.Transportation, Pos.Fighting),

    tsunami({ l, ch, vo, _ -> spell_tsunami(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(701), 50, 12, "raging tidal wave", "!tsunami!", "", SG.None, Pos.Fighting),

    turn({ l, ch, _, _ -> spell_turn(l, ch) },
            TAR_IGNORE, SLOT(597), 50, 12, "", "You can handle turn spell again.", "", SG.None, Pos.Fighting),

    vampiric_blast({ l, ch, vo, _ -> spell_vampiric_blast(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(611), 20, 12, "vampiric blast", "!Vampiric Blast!", "", SG.None, Pos.Fighting),

    ventriloquate({ l, ch, _, _ -> spell_ventriloquate(l, ch) },
            TAR_IGNORE, SLOT(41), 5, 12, "", "!Ventriloquate!", "", SG.Illusion),

    web({ l, ch, vo, _ -> spell_web(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(592), 50, 12, "", "The webs around you dissolve.", "", SG.Attack, Pos.Fighting),

    windwall({ l, ch, _, _ -> spell_windwall(l, ch) },
            TAR_IGNORE, SLOT(694), 20, 12, "air blast", "Your eyes feel better.", "", SG.Combat, Pos.Fighting),

    witch_curse({ l, ch, vo, _ -> spell_witch_curse(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(602), 150, 24, "", "You gain back your durability.", ""),

    wrath({ l, ch, vo, _ -> spell_wrath(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(553), 20, 12, "heavenly wrath", "The curse wears off.", "", SG.Benedictions),

    weaken({ l, _, vo, _ -> spell_weaken(l, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(68), 20, 12, "spell", "You feel stronger.", "", SG.Maladictions, Pos.Fighting),

    word_of_recall({ _, ch, vo, _ -> spell_word_of_recall(ch, vo as CHAR_DATA) },
            TAR_CHAR_SELF, SLOT(42), 5, 12, "", "!Word of Recall!", "", SG.Transportation, Pos.Resting),

    acid_breath({ l, ch, vo, _ -> spell_acid_breath(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(200), 100, 24, "blast of acid", "!Acid Breath!", "", SG.Draconian, Pos.Fighting),

    /* TODO desert_heat(Magic2::spell_desert_heat,
            TAR_CHAR_OFFENSIVE, Pos.Fighting, SLOT(629), 200, 24, "cloud of blistering desert heat", "The smoke leaves your eyes.", "", Clan.None, null, ALIGN_NONE, SGroup.Draconian),*/

    fire_breath({ l, ch, vo, _ -> spell_fire_breath(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(201), 200, 24, "blast of flame", "The smoke leaves your eyes.", "", SG.Draconian, Pos.Fighting),

    frost_breath({ l, ch, vo, _ -> spell_frost_breath(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(202), 125, 24, "blast of frost", "!Frost Breath!", "", SG.Draconian, Pos.Fighting),

    gas_breath({ l, ch, _, _ -> spell_gas_breath(l, ch) },
            TAR_IGNORE, SLOT(203), 175, 24, "blast of gas", "!Gas Breath!", "", SG.Draconian, Pos.Fighting),

    lightning_breath({ l, ch, vo, _ -> spell_lightning_breath(l, ch, vo as CHAR_DATA) },
            TAR_CHAR_OFFENSIVE, SLOT(204), 150, 24, "blast of lightning", "!Lightning Breath!", "", SG.Draconian, Pos.Fighting),

    /* TODO lightning_stroke(, Magic::spell_light_stroke,
            TAR_CHAR_OFFENSIVE, Pos.Fighting, SLOT(632), 200, 24, "stroke of lightning", "!lightning stroke!", "", Clan.None, null, ALIGN_NONE, SGroup.Draconian),*/

    /* TODO luck_bonus(Magic2::spell_luck_bonus,
            TAR_CHAR_DEFENSIVE, Pos.Standing, SLOT(630), 20, 12, "", "You feel less armored against magic.", "", Clan.None, null, ALIGN_NONE, SGroup.Protective),*/

    /* TODO: paralyzation(, Magic2::spell_paralyzation,
            TAR_IGNORE, Pos.Fighting, SLOT(631), 200, 24, "gas of paralyzation", "You feel you can move again.", "", Clan.None, null, ALIGN_NONE, SGroup.Draconian),*/

    /* TODO repulsion(, Magic2::spell_repulsion,
            TAR_CHAR_OFFENSIVE, Pos.Fighting, SLOT(633), 200, 24, "repulsion", "!repulsion!", "", Clan.None, null, ALIGN_NONE, SGroup.Draconian),*/

    /* TODO sleep_gas(Magic2::spell_sleep_gas,
            TAR_IGNORE, Pos.Fighting, SLOT(628), 200, 24, "sleep gas", "You feel drained.", "", Clan.None, null, ALIGN_NONE, SGroup.Draconian),*/

    /* TODO slow_gas(Magic2::spell_slow_gas,
            TAR_CHAR_OFFENSIVE, Pos.Fighting, SLOT(634), 200, 24, "slow gas", "You can move faster now.", "", Clan.None, null, ALIGN_NONE, SGroup.Draconian),*/

    crush(null, TAR_IGNORE, SLOT(0), 0, 18, "crush", "!crush!", "", SG.FightMaster, Pos.Fighting),

    general_purpose(null, TAR_CHAR_OFFENSIVE, SLOT(401), 0, 12, "general purpose ammo",
            "!General Purpose Ammo!", "", SG.None, Pos.Fighting),

    high_explosive(null, TAR_CHAR_OFFENSIVE, SLOT(402), 0, 12, "high explosive ammo",
            "!High Explosive Ammo!", "", SG.None, Pos.Fighting),

    tail(null, TAR_IGNORE, SLOT(0), 0, 18, "tail", "!Tail!", "", SG.FightMaster, Pos.Fighting),

    /* combat and weapons skills*/

    arrow(null, TAR_IGNORE, SLOT(0), 0, 0, "arrow", "!arrow!", "", SG.WeaponMaster),

    axe(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Axe!", "", SG.WeaponMaster, Pos.Fighting),

    bow(null, TAR_IGNORE, SLOT(0), 0, 12, "bow", "!bow!", "", SG.WeaponMaster),

    dagger(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Dagger!", "", SG.WeaponMaster, Pos.Fighting),

    flail(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Flail!", "", SG.WeaponMaster, Pos.Fighting),

    lance(null, TAR_IGNORE, SLOT(0), 0, 0, "lance", "!lance!", "", SG.Cabal, Pos.Standing, Clan.Knight),

    mace(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Mace!", "", SG.WeaponMaster, Pos.Fighting),

    polearm(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Polearm!", "", SG.WeaponMaster, Pos.Fighting),

    shield_block(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Shield!", "", SG.Defensive, Pos.Fighting),

    spear(null, TAR_IGNORE, SLOT(0), 0, 12, "spear", "!Spear!", "", SG.WeaponMaster, Pos.Fighting),

    sword(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!sword!", "", SG.WeaponMaster, Pos.Fighting),

    whip(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Whip!", "", SG.WeaponMaster, Pos.Fighting),

    second_weapon(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!second weapon!", "", SG.WeaponMaster, Pos.Fighting),

    ambush(null, TAR_CHAR_OFFENSIVE, SLOT(0), 0, 12, "surprise attack", "!Ambush!", ""),

    area_attack(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Area Attack!", "", SG.FightMaster, Pos.Fighting),

    assassinate(null, TAR_IGNORE, SLOT(0), 0, 18, "assassination attempt", "!assassinate!", ""),

    backstab(null, TAR_IGNORE, SLOT(0), 0, 12, "backstab", "!Backstab!", ""),

    bash(null, TAR_IGNORE, SLOT(0), 0, 18, "bash", "!Bash!", "", SG.FightMaster, Pos.Fighting),

    bash_door(null, TAR_IGNORE, SLOT(0), 0, 18, "bash", "!Bash Door!", "", SG.FightMaster, Pos.Fighting),

    bear_call(null, TAR_IGNORE, SLOT(518), 50, 0, "", "You feel you can handle more bears now.", "", SG.None, Pos.Fighting),

    berserk(null, TAR_IGNORE, SLOT(0), 0, 24, "", "You feel your pulse slow down.", "", SG.None, Pos.Fighting),

    blackguard(null, TAR_CHAR_OFFENSIVE, SLOT(0), 0, 12, "blackguard", "Your blackguard fades away.", ""),

    blackjack(null, TAR_CHAR_OFFENSIVE, SLOT(0), 0, 8, "blackjack", "Your head feels better.", ""),

    blind_fighting(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!blind fighting!", "", SG.FightMaster),

    blindness_dust(null, TAR_IGNORE, SLOT(0), 20, 18, "", "!blindness dust!", "", SG.None, Pos.Fighting),

    blink(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Blink!", "", SG.None, Pos.Fighting),

    butcher(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!butcher!", ""),

    caltraps(null, TAR_IGNORE, SLOT(0), 0, 18, "caltraps", "Your feet feel less sore.", "", SG.None, Pos.Fighting),

    camouflage(null, TAR_IGNORE, SLOT(0), 0, 24, "", "!Camouflage!", ""),

    camouflage_move(null, TAR_IGNORE, SLOT(0), 0, 12, "", "!move camouflaged!", ""),

    camp(null, TAR_IGNORE, SLOT(0), 0, 24, "camp", "You can handle more camps now.", ""),

    circle(null, TAR_CHAR_OFFENSIVE, SLOT(0), 0, 18, "circle stab", "!Circle!", "", SG.None, Pos.Fighting),

    control_animal(null, TAR_CHAR_OFFENSIVE, SLOT(0), 5, 12, "", "You feel more self-confident.", "", SG.Beguiling),

    cleave(null, TAR_CHAR_OFFENSIVE, SLOT(0), 0, 24, "cleave", "!Cleave!", ""),

    concentrate(null, TAR_IGNORE, SLOT(0), 0, 18, "", "You can concentrate on new fights.", "", SG.FightMaster, Pos.Fighting),

    counter(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Counter!", "", SG.FightMaster),

    critical_strike(null, TAR_IGNORE, SLOT(0), 0, 18, "", "!critical strike!", ""),

    cross_block(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!cross block!", "", SG.Defensive, Pos.Fighting),

    detect_hidden(null, TAR_CHAR_SELF, SLOT(44), 5, 12, "", "You feel less aware of your surroundings.", "", SG.Detection),

    detect_sneak(null, TAR_CHAR_SELF, SLOT(0), 20, 18, "", "!detect sneak!", "", SG.Detection),

    dirt_kicking(null, TAR_IGNORE, SLOT(0), 0, 12, "kicked dirt", "You rub the dirt out of your eyes.", "", SG.None, Pos.Fighting),

    disarm(null, TAR_IGNORE, SLOT(0), 0, 18, "", "!Disarm!", "", SG.WeaponMaster, Pos.Fighting),

    dodge(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Dodge!", "", SG.Defensive, Pos.Fighting),

    dual_backstab(null, TAR_CHAR_OFFENSIVE, SLOT(0), 0, 0, "second backstab", "!dual backstab!", ""),

    enchant_sword(null, TAR_OBJ_INV, SLOT(0), 100, 24, "", "!Enchant sword!", ""),

    endure(null, TAR_CHAR_SELF, SLOT(0), 0, 24, "", "You feel susceptible to magic again.", "", SG.None, Pos.Fighting),

    enhanced_damage(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Enhanced Damage!", "", SG.FightMaster, Pos.Fighting),

    entangle({ l, ch, vo, _ -> spell_entangle(l, ch, vo as CHAR_DATA) }, TAR_CHAR_OFFENSIVE, SLOT(568), 40, 12, "entanglement", "You feel less entangled.", "", SG.None, Pos.Fighting),

    envenom(null, TAR_IGNORE, SLOT(0), 0, 12, "", "!Envenom!", "", SG.None, Pos.Resting),

    escape(null, TAR_IGNORE, SLOT(0), 0, 24, "", "!escape!", ""),

    explode(null, TAR_CHAR_OFFENSIVE, SLOT(0), 100, 24, "flame", "The smoke leaves your eyes.", "", SG.None, Pos.Fighting),

    ground_strike(null, TAR_IGNORE, SLOT(0), 0, 18, "", "!ground strike!", ""),

    hand_block(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!hand block!", "", SG.Defensive, Pos.Fighting),

    hand_to_hand(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Hand to Hand!", "", SG.FightMaster, Pos.Fighting),

    hara_kiri(null, TAR_IGNORE, SLOT(0), 50, 12, "", "You feel you gain your life again.", "", SG.None, Pos.Fighting),

    headguard(null, TAR_CHAR_OFFENSIVE, SLOT(0), 0, 12, "headguard", "Your headguard fades away.", ""),

    herbs(null, TAR_CHAR_DEFENSIVE, SLOT(0), 0, 30, "", "The herbs look more plentiful here.", ""),

    kick(null, TAR_CHAR_OFFENSIVE, SLOT(0), 0, 12, "kick", "!Kick!", "", SG.FightMaster, Pos.Fighting),

    lash(null, TAR_IGNORE, SLOT(0), 0, 4, "lash", "!Lash!", "", SG.FightMaster, Pos.Fighting),

    light_resistance(null, TAR_IGNORE, SLOT(0), 0, 0, "", "Light Resistance", "", SG.None, Pos.Sleeping),

    lion_call(null, TAR_CHAR_DEFENSIVE, SLOT(0), 50, 12, "", "!lion call!", "", SG.None, Pos.Fighting),

    make_arrow(null, TAR_IGNORE, SLOT(0), 50, 24, "", "!make arrow!", ""),

    make_bow(null, TAR_IGNORE, SLOT(0), 200, 24, "", "!make bow!", ""),

    mental_attack(null, TAR_CHAR_SELF, SLOT(0), 200, 24, "", "!mental attack!", ""),

    neckguard(null, TAR_CHAR_OFFENSIVE, SLOT(0), 0, 12, "neckguard", "Your neckguard fades away.", ""),

    nerve(null, TAR_CHAR_OFFENSIVE, SLOT(0), 0, 18, "", "Your nerves feel better.", "", SG.None, Pos.Fighting),

    parry(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Parry!", "", SG.Defensive, Pos.Fighting),

    perception(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!perception!", ""),

    push(null, TAR_IGNORE, SLOT(0), 0, 18, "push", "!push!", ""),

    rescue(null, TAR_IGNORE, SLOT(0), 0, 12, "", "!Rescue!", "", SG.Defensive, Pos.Fighting),

    sense_life(null, TAR_CHAR_SELF, SLOT(623), 20, 12, "", "You lost the power to sense life.", ""),

    settraps(null, TAR_CHAR_SELF, SLOT(0), 200, 24, "trap", "You can set more traps now.", ""),

    shield_cleave(null, TAR_CHAR_SELF, SLOT(0), 200, 24, "", "!shield cleave!", ""),

    smithing(null, TAR_IGNORE, SLOT(0), 10, 18, "", "!smithing!", ""),

    spell_craft(null, TAR_IGNORE, SLOT(0), 0, 0, "spell craft", "!spell craft!", "", SG.None, Pos.Fighting),

    strangle(null, TAR_CHAR_OFFENSIVE, SLOT(0), 0, 8, "strangulation", "Your neck feels better.", ""),

    swimming(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!swimming!", ""),

    Target(null, TAR_CHAR_OFFENSIVE, SLOT(0), 0, 12, "target", "!Kick!", "", SG.None, Pos.Fighting),

    Throw(null, TAR_CHAR_OFFENSIVE, SLOT(0), 0, 18, "throw", "!throw!", "", SG.None, Pos.Fighting),

    tiger_power(null, TAR_IGNORE, SLOT(0), 0, 12, "", "You feel your tigers escaped.", "", SG.None, Pos.Fighting),

    track(null, TAR_IGNORE, SLOT(0), 0, 12, "", "!track!", ""),

    trip(null, TAR_IGNORE, SLOT(0), 0, 12, "trip", "!Trip!", "", SG.FightMaster, Pos.Fighting),

    vampire(null, TAR_IGNORE, SLOT(0), 100, 12, "", "Now you are familer to other creatures.", ""),

    vampiric_bite(null, TAR_IGNORE, SLOT(0), 0, 12, "vampiric bite", "!vampiric bite!", ""),

    vampiric_touch(null, TAR_CHAR_OFFENSIVE, SLOT(0), 0, 12, "vampiric touch", "You wake up from nightmares.", ""),

    vanish(null, TAR_CHAR_SELF, SLOT(521), 25, 18, "", "!vanish!", "", SG.None, Pos.Fighting),

    warcry(null, TAR_IGNORE, SLOT(0), 30, 12, "", "Your warcry has worn off.", "", SG.None, Pos.Fighting),

    weapon_cleave(null, TAR_CHAR_SELF, SLOT(0), 200, 24, "", "!weapon cleave!", ""),

    second_attack(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Second Attack!", "", SG.FightMaster, Pos.Fighting),

    secondary_attack(null, TAR_CHAR_SELF, SLOT(0), 200, 24, "", "!secondary attack!", ""),

    third_attack(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Third Attack!", "", SG.FightMaster, Pos.Fighting),

    fourth_attack(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Fourth Attack!", "", SG.FightMaster, Pos.Fighting),

    fifth_attack(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Fifth Attack!", "", SG.FightMaster, Pos.Fighting),

    blue_arrow(null, TAR_IGNORE, SLOT(0), 50, 12, "", "!blue arrow!", ""),

    fast_healing(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Fast Healing!", "", SG.Meditation, Pos.Sleeping),

    green_arrow(null, TAR_IGNORE, SLOT(0), 50, 12, "", "!green arrow!", ""),

    grip(null, TAR_IGNORE, SLOT(0), 0, 18, "", "!Grip!", "", SG.WeaponMaster, Pos.Fighting),

    haggle(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Haggle!", "", SG.None, Pos.Resting),

    hide(null, TAR_IGNORE, SLOT(0), 0, 18, "", "!Hide!", "", SG.None, Pos.Resting),

    katana(null, TAR_OBJ_INV, SLOT(0), 100, 24, "", "You can now make another katana.", ""),

    lay_hands(null, TAR_IGNORE, SLOT(0), 0, 12, "", "You may heal more innocents now.", "", SG.None, Pos.Fighting),

    lore(null, TAR_IGNORE, SLOT(0), 0, 36, "", "!Lore!", "", SG.None, Pos.Resting),

    mastering_pound(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Master Hand!", "", SG.FightMaster, Pos.Fighting),

    mastering_sword(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!master sword!", "", SG.WeaponMaster, Pos.Fighting),

    meditation(null, TAR_IGNORE, SLOT(0), 0, 0, "", "Meditation", "", SG.Meditation, Pos.Sleeping),

    peek(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Peek!", ""),

    pick_lock(null, TAR_IGNORE, SLOT(0), 0, 12, "", "!Pick!", ""),

    poison_smoke(null, TAR_IGNORE, SLOT(0), 20, 18, "", "!poison smoke!", "", SG.None, Pos.Fighting),

    quiet_movement(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!quiet movement!", ""),

    recall(null, TAR_IGNORE, SLOT(0), 0, 24, "", "!Recall!", ""),

    red_arrow(null, TAR_IGNORE, SLOT(0), 50, 12, "", "!red arrow!", ""),

    sneak(null, TAR_IGNORE, SLOT(0), 0, 12, "", "You no longer feel stealthy.", ""),

    steal(null, TAR_IGNORE, SLOT(0), 0, 12, "", "!Steal!", ""),

    scrolls(null, TAR_IGNORE, SLOT(0), 0, 12, "", "!Scrolls!", "", SG.Wizard),

    staves(null, TAR_IGNORE, SLOT(0), 0, 12, "", "!Staves!", "", SG.Wizard),

    tame(null, TAR_CHAR_DEFENSIVE, SLOT(0), 0, 24, "", "!tame!", "", SG.None, Pos.Fighting),

    trance(null, TAR_IGNORE, SLOT(0), 0, 0, "", "", "", SG.Meditation, Pos.Sleeping),

    white_arrow(null, TAR_IGNORE, SLOT(0), 50, 12, "", "!white arrow!", ""),

    wands(null, TAR_IGNORE, SLOT(0), 0, 12, "", "!Wands!", "", SG.Wizard),

    mortal_strike(null, TAR_CHAR_SELF, SLOT(0), 200, 24, "mortal strike", "!mortal strike!", "", SG.None, Pos.Standing, Clan.BattleRager),

    disgrace({ l, ch, vo, _ -> spell_disgrace(l, ch, vo as CHAR_DATA) }, TAR_CHAR_OFFENSIVE, SLOT(619), 200, 12, "", "You feel yourself getting prouder.", "", SG.None, Pos.Standing, Clan.Chaos),

    evil_spirit({ l, ch, _, _ -> spell_evil_spirit(l, ch) }, TAR_IGNORE, SLOT(618), 800, 36, "evil spirit", "Your body regains its full spirit.", "", SG.None, Pos.Standing, Clan.Invader),

    ruler_aura({ l, ch, _, _ -> spell_ruler_aura(l, ch) }, TAR_CHAR_SELF, SLOT(667), 20, 12, "", "Your ruler aura fades.", "", SG.Cabal, Pos.Standing, Clan.Ruler),

    sword_of_justice({ l, ch, vo, _ -> spell_sword_of_justice(l, ch, vo as CHAR_DATA) }, TAR_CHAR_OFFENSIVE, SLOT(686), 50, 12, "sword of justice", "!sword of justice!", "", SG.Cabal, Pos.Fighting, Clan.Ruler),

    bandage(null, TAR_IGNORE, SLOT(0), 0, 0, "", "You feel less healthy.", "", SG.Cabal, Pos.Standing, Clan.BattleRager),

    cabal_recall(null, TAR_IGNORE, SLOT(0), 0, 24, "", "You may pray for transportation again.", "", SG.Cabal, Pos.Standing, Clan.BattleRager),

    wanted(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Wanted!", "", SG.Cabal, Pos.Dead, Clan.Ruler),

    judge(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Judge!", "", SG.Cabal, Pos.Dead, Clan.Ruler),

    bloodthirst(null, TAR_CHAR_SELF, SLOT(0), 0, 12, "", "Your bloody rage fades away.", "", SG.Cabal, Pos.Fighting, Clan.BattleRager),

    spellbane(null, TAR_CHAR_SELF, SLOT(0), 0, 12, "spellbane", "You feel less resistant to magic.", "", SG.Cabal, Pos.Standing, Clan.BattleRager),

    resistance(null, TAR_CHAR_SELF, SLOT(0), 0, 24, "", "You feel less tough.", "", SG.Cabal, Pos.Fighting, Clan.BattleRager),

    deathblow(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!deathblow!", "", SG.Cabal, Pos.Standing, Clan.BattleRager),

    transform({ l, ch, _, _ -> spell_transform(l, ch) }, TAR_CHAR_SELF, SLOT(522), 100, 24, "", "You feel less healthy.", "", SG.Cabal, Pos.Standing, Clan.Shalafi),

    mental_knife({ l, ch, vo, _ -> spell_mental_knife(l, ch, vo as CHAR_DATA) }, TAR_CHAR_OFFENSIVE, SLOT(524), 35, 12, "mental knife", "Your mental pain dissipates.", "", SG.Cabal, Pos.Fighting, Clan.Shalafi),

    demon_summon({ l, ch, _, _ -> spell_demon_summon(l, ch) }, TAR_CHAR_SELF, SLOT(525), 100, 12, "", "You feel your summoning power return.", "", SG.Cabal, Pos.Fighting, Clan.Shalafi),

    scourge({ l, ch, _, _ -> spell_scourge(l, ch) }, TAR_IGNORE, SLOT(526), 50, 18, "Scourge of the Violet Spider", "!scourge!", "", SG.Cabal, Pos.Fighting, Clan.Shalafi),

    manacles({ l, ch, vo, _ -> spell_manacles(l, ch, vo as CHAR_DATA) }, TAR_CHAR_OFFENSIVE, SLOT(528), 75, 12, "", "Your shackles dissolve.", "", SG.Cabal, Pos.Fighting, Clan.Ruler),

    shield_of_ruler({ l, ch, _, _ -> spell_shield_ruler(l, ch) }, TAR_IGNORE, SLOT(529), 100, 12, "", "!shield!", "", SG.Cabal, Pos.Fighting, Clan.Ruler),

    guard(null, TAR_IGNORE, SLOT(0), 0, 12, "", "", "", SG.Cabal, Pos.Standing, Clan.Knight),

    guard_call({ l, ch, _, _ -> spell_guard_call(l, ch) }, TAR_IGNORE, SLOT(530), 75, 12, "", "You may call more guards now.", "", SG.Cabal, Pos.Fighting, Clan.Ruler),

    nightwalker({ l, ch, _, _ -> spell_nightwalker(l, ch) }, TAR_IGNORE, SLOT(531), 75, 12, "", "You feel your summoning power return.", "", SG.Cabal, Pos.Fighting, Clan.Invader),

    eyes_of_intrigue({ _, ch, _, _ -> spell_eyes(ch) }, TAR_IGNORE, SLOT(532), 75, 12, "", "!eyes of intrigue!", "", SG.Cabal, Pos.Fighting, Clan.Invader),

    fade(null, TAR_IGNORE, SLOT(0), 0, 24, "", "!fade!", "", SG.Cabal, Pos.Standing, Clan.Invader),

    shadow_cloak({ l, ch, vo, _ -> spell_shadow_cloak(l, ch, vo as CHAR_DATA) }, TAR_CHAR_DEFENSIVE, SLOT(533), 10, 12, "", "The shadows no longer protect you.", "", SG.Cabal, Pos.Standing, Clan.Invader),

    nightfall({ l, ch, _, _ -> spell_nightfall(l, ch) }, TAR_IGNORE, SLOT(534), 50, 12, "", "You are now able to control lights.", "", SG.Cabal, Pos.Standing, Clan.Invader),

    aura_of_chaos({ l, ch, vo, _ -> spell_aura_of_chaos(l, ch, vo as CHAR_DATA) }, TAR_CHAR_DEFENSIVE, SLOT(720), 20, 12, "", "The gods of chaos no longer protect you.", "", SG.Cabal, Pos.Standing, Clan.Chaos),

    garble({ l, ch, vo, _ -> spell_garble(l, ch, vo as CHAR_DATA) }, TAR_CHAR_DEFENSIVE, SLOT(535), 30, 12, "", "Your tongue untwists.", "", SG.Cabal, Pos.Fighting, Clan.Chaos),

    mirror({ l, ch, vo, _ -> spell_mirror(l, ch, vo as CHAR_DATA) }, TAR_CHAR_DEFENSIVE, SLOT(536), 40, 12, "", "You fade away.", "", SG.Cabal, Pos.Standing, Clan.Chaos),

    confuse({ l, ch, vo, _ -> spell_confuse(l, ch, vo as CHAR_DATA) }, TAR_CHAR_OFFENSIVE, SLOT(537), 20, 12, "", "You feel less confused.", "", SG.Cabal, Pos.Fighting, Clan.Chaos),

    doppelganger({ l, ch, vo, _ -> spell_doppelganger(l, ch, vo as CHAR_DATA) }, TAR_CHAR_DEFENSIVE, SLOT(527), 75, 12, "", "You return to your native form.", "", SG.Cabal, Pos.Standing, Clan.Chaos),

    chaos_blade({ l, ch, _, _ -> spell_chaos_blade(l, ch) }, TAR_IGNORE, SLOT(550), 60, 12, "", "!chaos blade!", "", SG.Cabal, Pos.Standing, Clan.Chaos),

    stalker({ l, ch, _, _ -> spell_stalker(l, ch) }, TAR_IGNORE, SLOT(554), 100, 12, "", "You feel up to summoning another stalker.", "", SG.Cabal, Pos.Standing, Clan.Ruler),

    randomizer({ l, ch, _, _ -> spell_randomizer(l, ch) }, TAR_IGNORE, SLOT(555), 200, 24, "", "You feel your randomness regenerating.", "", SG.Cabal, Pos.Standing, Clan.Chaos),

    tesseract({ l, ch, _, _ -> spell_tesseract(l, ch) }, TAR_IGNORE, SLOT(556), 150, 12, "", "!tesseract!", "", SG.Cabal, Pos.Standing, Clan.Shalafi),

    trophy(null, TAR_IGNORE, SLOT(0), 30, 12, "", "You feel up to making another trophy.", "", SG.Cabal, Pos.Standing, Clan.BattleRager),

    truesight(null, TAR_IGNORE, SLOT(0), 50, 12, "", "Your eyes see less truly.", "", SG.Cabal, Pos.Standing, Clan.BattleRager),

    brew({ l, ch, vo, _ -> spell_brew(l, ch, vo as Obj) }, TAR_OBJ_INV, SLOT(557), 25, 12, "", "You feel like you can start brewing again.", "", SG.Cabal, Pos.Standing, Clan.Shalafi),

    shadowlife({ l, ch, vo, _ -> spell_shadowlife(l, ch, vo as CHAR_DATA) }, TAR_CHAR_OFFENSIVE, SLOT(558), 80, 12, "", "Your feel more shadowy.", "", SG.Cabal, Pos.Standing, Clan.Invader),

    ruler_badge({ l, ch, vo, _ -> spell_ruler_badge(l, ch, vo as CHAR_DATA) }, TAR_CHAR_SELF, SLOT(560), 50, 12, "", "!ruler badge!", "", SG.Cabal, Pos.Standing, Clan.Ruler),

    remove_badge({ _, ch, vo, _ -> spell_remove_badge(ch, vo as CHAR_DATA) }, TAR_CHAR_SELF, SLOT(561), 100, 12, "", "!remove badge!", "", SG.Cabal, Pos.Standing, Clan.Ruler),

    golden_aura({ l, ch, _, _ -> spell_golden_aura(l, ch) }, TAR_IGNORE, SLOT(564), 25, 12, "", "You feel the golden aura dissipate.", "", SG.Cabal, Pos.Standing, Clan.Knight),

    dragonplate({ l, ch, _, _ -> spell_dragonplate(l, ch) }, TAR_IGNORE, SLOT(565), 60, 12, "", "", "", SG.Cabal, Pos.Standing, Clan.Knight),

    squire({ l, ch, _, _ -> spell_squire(l, ch) }, TAR_IGNORE, SLOT(566), 100, 12, "", "You feel up to worrying about a new squire.", "", SG.Cabal, Pos.Standing, Clan.Knight),

    dragonsword({ l, ch, _, _ -> spell_dragonsword(l, ch) }, TAR_IGNORE, SLOT(567), 70, 12, "", "", "", SG.Cabal, Pos.Standing, Clan.Knight),

    holy_armor({ l, ch, _, _ -> spell_holy_armor(l, ch) }, TAR_CHAR_SELF, SLOT(569), 20, 12, "", "You are less protected from harm.", "", SG.Cabal, Pos.Resting, Clan.Knight),

    disperse({ l, ch, _, _ -> spell_disperse(l, ch) }, TAR_IGNORE, SLOT(573), 100, 24, "", "You feel up to doing more dispersing.", "", SG.Cabal, Pos.Fighting, Clan.Chaos),

    hunt(null, TAR_IGNORE, SLOT(0), 0, 6, "", "!hunt!", "", SG.Cabal, Pos.Standing, Clan.Hunter),

    find_object(null, TAR_IGNORE, SLOT(585), 20, 18, "", "!Find Object!", "", SG.Cabal, Pos.Standing, Clan.Hunter),

    path_find(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!endur!", "", SG.Cabal, Pos.Sleeping, Clan.Hunter),

    riding(null, TAR_IGNORE, SLOT(0), 0, 6, "", "!riding!", "", SG.None, Pos.Standing, Clan.Knight),

    wolf({ l, ch, _, _ -> spell_wolf(l, ch) }, TAR_IGNORE, SLOT(593), 100, 12, "", "You feel you can handle more wolfs now.", "", SG.Cabal, Pos.Standing, Clan.Hunter),

    wolf_spirit({ l, ch, _, _ -> spell_wolf_spirit(l, ch) }, TAR_CHAR_SELF, SLOT(685), 50, 12, "", "The blood in your vains start to flow as normal.", "", SG.Cabal, Pos.Standing, Clan.Hunter),

    armor_use(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!Armor Use!", "", SG.Cabal, Pos.Fighting, Clan.Hunter),

    world_find(null, TAR_IGNORE, SLOT(0), 0, 0, "", "!world find!", "", SG.Cabal, Pos.Sleeping, Clan.Hunter),

    take_revenge(null, TAR_IGNORE, SLOT(624), 50, 12, "", "!take revenge!", "", SG.Cabal, Pos.Standing, Clan.Hunter),

    mastering_spell(null, TAR_IGNORE, SLOT(0), 0, 0, "mastering spell", "!mastering spell!", "", SG.Cabal, Pos.Fighting, Clan.Shalafi),

    guard_dogs({ l, ch, _, _ -> spell_guard_dogs(l, ch) }, TAR_IGNORE, SLOT(687), 100, 12, "", "", "", SG.Cabal, Pos.Fighting, Clan.Lion),

    eyes_of_tiger({ _, ch, _, _ -> spell_eyes_of_tiger(ch) }, TAR_IGNORE, SLOT(688), 20, 12, "", "", "", SG.Cabal, Pos.Fighting, Clan.Lion),

    lion_shield({ l, ch, _, _ -> spell_lion_shield(l,ch) }, TAR_IGNORE, SLOT(689), 200, 12, "", "", "", SG.Cabal, Pos.Fighting, Clan.Lion),

    evolve_lion({ l, ch, _, _ -> spell_evolve_lion(l,ch) }, TAR_IGNORE, SLOT(690), 50, 12, "", "", "", SG.Cabal, Pos.Fighting, Clan.Lion),

    claw(null, TAR_IGNORE, SLOT(0), 50, 24, "claw", "", "", SG.Cabal, Pos.Fighting, Clan.Lion),

    prevent({ l, ch, _, _ -> spell_prevent(l,ch) }, TAR_IGNORE, SLOT(691), 75, 12, "", "", "", SG.Cabal, Pos.Fighting, Clan.Lion),

    terangreal({ l, _, vo, _ -> spell_terangreal(l, vo as CHAR_DATA) }, TAR_CHAR_OFFENSIVE, SLOT(607), 5, 12, "terangreal", "You are awake again.", ""),

    kassandra({ l, ch, _, _ -> spell_kassandra(l, ch) }, TAR_CHAR_SELF, SLOT(608), 5, 12, "", "You can heal yourself again.", ""),

    sebat({ l, ch, _, _ -> spell_sebat(l, ch) }, TAR_CHAR_SELF, SLOT(609), 5, 12, "", "You can protect yourself again.", ""),

    matandra({ l, ch, vo, _ -> spell_matandra(l, ch, vo as CHAR_DATA) }, TAR_CHAR_OFFENSIVE, SLOT(610), 5, 12, "holy word", "You can use kassandra again.", ""),

    demand(null, TAR_IGNORE, SLOT(0), 5, 12, "", "", ""),

    bury(null, TAR_IGNORE, SLOT(0), 5, 12, "", "", ""),

    x_hit(null, TAR_IGNORE, SLOT(0), 0, 0, "", "", ""),

    x_hunger(null, TAR_IGNORE, SLOT(0), 0, 0, "", "", "");

    val isSpell: Boolean = spellFun != null

    val skillName get() = name.toLowerCase().replace('_', ' ')

    fun spell_fun(level: Int, ch: CHAR_DATA, victim: CHAR_DATA, target: Int) = spellFun?.invoke(level, ch, victim, target)

    fun spell_fun(level: Int, ch: CHAR_DATA, obj: Obj, target: Int) = spellFun?.invoke(level, ch, obj, target)

    companion object {
        var MAX_SKILLS = Skill.values().size

        /** list of all skills. do not modify this list */
        internal val skills = Skill.values()

        private val skillMap = mutableMapOf<String, Skill>()

        init {
            skills.forEach { skillMap.put(it.skillName, it) }
        }

        fun skill_num_lookup(name: String) = lookup(name)?.ordinal ?: -1

        fun lookupNN(name: String) = lookup(name) ?: throw IllegalArgumentException("Skill not found: $name")

        /** Lookup a skill by name. */
        fun lookup(name: String) = skillMap[name] ?: Skill.skills.firstOrNull { startsWith(name, it.skillName) }

        /** Finds a spell the character can cast if possible */
        fun find_spell(ch: CHAR_DATA, name: String): Skill? {
            if (ch.pcdata == null) {
                return lookup(name)
            }

            var found: Skill? = null
            val skills = Skill.skills
            for (sn in skills) {
                if (startsWith(name, sn.skillName)) {
                    if (found == null) {
                        found = sn
                    }
                    if (skill_failure_nomessage(ch, sn, 0) == 0) {
                        return sn
                    }
                }
            }
            return found
        }
    }
}
