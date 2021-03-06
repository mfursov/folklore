package net.sf.nightworks

import net.sf.nightworks.util.one_argument

object Tables {

    /* various flag tables */
    val act_flags = arrayOf(
            flag_type("npc", A, false),
            flag_type("sentinel", B),
            flag_type("scavenger", C),
            flag_type("aggressive", F),
            flag_type("stay_area", G),
            flag_type("wimpy", H),
            flag_type("pet", I),
            flag_type("train", J),
            flag_type("practice", K),
            flag_type("undead", O),
            flag_type("cleric", Q),
            flag_type("mage", R),
            flag_type("thief", S),
            flag_type("warrior", T),
            flag_type("noalign", U),
            flag_type("nopurge", V),
            flag_type("outdoors", W),
            flag_type("indoors", Y),
            flag_type("healer", BIT_26),
            flag_type("gain", BIT_27),
            flag_type("update_always", BIT_28),
            flag_type("changer", BIT_29))

    val plr_flags = arrayOf(
            flag_type("npc", A, false),
            flag_type("autoassist", C, false),
            flag_type("autoexit", D, false),
            flag_type("autoloot", E, false),
            flag_type("autosac", F, false),
            flag_type("autogold", G, false),
            flag_type("autosplit", H, false),
            flag_type("holylight", N, false),
            flag_type("can_loot", P, false),
            flag_type("nosummon", Q, false),
            flag_type("nofollow", R, false),
            flag_type("permit", U),
            flag_type("log", W, false),
            flag_type("deny", X, false),
            flag_type("freeze", Y, false),
            flag_type("thief", Z, false),
            flag_type("killer", BIT_26, false),
            flag_type("questor", BIT_27, false),
            flag_type("vampire", BIT_28, false))

    val affect_flags = arrayOf(
            flag_type("blind", A),
            flag_type("invisible", B),
            flag_type("sanctuary", H),
            flag_type("faerie_fire", I),
            flag_type("infrared", J),
            flag_type("curse", K),
            flag_type("poison", M),
            flag_type("protect_evil", N),
            flag_type("protect_good", O),
            flag_type("sneak", P),
            flag_type("hide", Q),
            flag_type("sleep", R),
            flag_type("charm", S),
            flag_type("flying", T),
            flag_type("pass_door", U),
            flag_type("haste", V),
            flag_type("calm", W),
            flag_type("plague", X),
            flag_type("weaken", Y),
            flag_type("wstun", Z),
            flag_type("berserk", BIT_26),
            flag_type("swim", BIT_27),
            flag_type("regeneration", BIT_28),
            flag_type("slow", BIT_29),
            flag_type("camouflage", BIT_30))

    val off_flags = arrayOf(
            flag_type("area_attack", A),
            flag_type("backstab", B),
            flag_type("bash", C),
            flag_type("berserk", D),
            flag_type("disarm", E),
            flag_type("dodge", F),
            flag_type("fade", G),
            flag_type("fast", H),
            flag_type("kick", I),
            flag_type("dirt_kick", J),
            flag_type("parry", K),
            flag_type("rescue", L),
            flag_type("tail", M),
            flag_type("trip", N),
            flag_type("crush", O),
            flag_type("assist_all", P),
            flag_type("assist_align", Q),
            flag_type("assist_race", R),
            flag_type("assist_players", S),
            flag_type("assist_guard", T),
            flag_type("assist_vnum", U))

    val imm_flags = arrayOf(
            flag_type("summon", A),
            flag_type("charm", B),
            flag_type("magic", C),
            flag_type("weapon", D),
            flag_type("bash", E),
            flag_type("pierce", F),
            flag_type("slash", G),
            flag_type("fire", H),
            flag_type("cold", I),
            flag_type("lightning", J),
            flag_type("acid", K),
            flag_type("poison", L),
            flag_type("negative", M),
            flag_type("holy", N),
            flag_type("energy", O),
            flag_type("mental", P),
            flag_type("disease", Q),
            flag_type("drowning", R),
            flag_type("light", S),
            flag_type("sound", T),
            flag_type("wood", X),
            flag_type("silver", Y),
            flag_type("iron", Z))

    val form_flags = arrayOf(
            flag_type("edible", FORM_EDIBLE),
            flag_type("poison", FORM_POISON),
            flag_type("magical", FORM_MAGICAL),
            flag_type("instant_decay", FORM_INSTANT_DECAY),
            flag_type("other", FORM_OTHER),
            flag_type("animal", FORM_ANIMAL),
            flag_type("sentient", FORM_SENTIENT),
            flag_type("undead", FORM_UNDEAD),
            flag_type("construct", FORM_CONSTRUCT),
            flag_type("mist", FORM_MIST),
            flag_type("intangible", FORM_INTANGIBLE),
            flag_type("biped", FORM_BIPED),
            flag_type("centaur", FORM_CENTAUR),
            flag_type("insect", FORM_INSECT),
            flag_type("spider", FORM_SPIDER),
            flag_type("crustacean", FORM_CRUSTACEAN),
            flag_type("worm", FORM_WORM),
            flag_type("blob", FORM_BLOB),
            flag_type("mammal", FORM_MAMMAL),
            flag_type("bird", FORM_BIRD),
            flag_type("reptile", FORM_REPTILE),
            flag_type("snake", FORM_SNAKE),
            flag_type("dragon", FORM_DRAGON),
            flag_type("amphibian", FORM_AMPHIBIAN),
            flag_type("fish", FORM_FISH),
            flag_type("cold_blood", FORM_COLD_BLOOD))

    val part_flags = arrayOf(
            flag_type("head", PART_HEAD),
            flag_type("arms", PART_ARMS),
            flag_type("legs", PART_LEGS),
            flag_type("heart", PART_HEART),
            flag_type("brains", PART_BRAINS),
            flag_type("guts", PART_GUTS),
            flag_type("hands", PART_HANDS),
            flag_type("feet", PART_FEET),
            flag_type("fingers", PART_FINGERS),
            flag_type("ear", PART_EAR),
            flag_type("eye", PART_EYE),
            flag_type("long_tongue", PART_LONG_TONGUE),
            flag_type("eyestalks", PART_EYESTALKS),
            flag_type("tentacles", PART_TENTACLES),
            flag_type("fins", PART_FINS),
            flag_type("wings", PART_WINGS),
            flag_type("tail", PART_TAIL),
            flag_type("claws", PART_CLAWS),
            flag_type("fangs", PART_FANGS),
            flag_type("horns", PART_HORNS),
            flag_type("scales", PART_SCALES),
            flag_type("tusks", PART_TUSKS))

    val comm_flags = arrayOf(
            flag_type("quiet", COMM_QUIET),
            flag_type("deaf", COMM_DEAF),
            flag_type("nowiz", COMM_NOWIZ),
            flag_type("nogossip", COMM_NOGOSSIP),
            flag_type("noquestion", COMM_NOQUESTION),
            flag_type("nomusic", COMM_NOMUSIC),
            flag_type("noquote", COMM_NOQUOTE),
            flag_type("shoutsoff", COMM_SHOUTSOFF),
            flag_type("true_trust", COMM_true_TRUST),
            flag_type("compact", COMM_COMPACT),
            flag_type("brief", COMM_BRIEF),
            flag_type("prompt", COMM_PROMPT),
            flag_type("combine", COMM_COMBINE),
            flag_type("telnet_ga", COMM_TELNET_GA),
            flag_type("show_affects", COMM_SHOW_AFFECTS),
            flag_type("nograts", COMM_NOGRATS),
            flag_type("noemote", COMM_NOEMOTE, false),
            flag_type("noshout", COMM_NOSHOUT, false),
            flag_type("notell", COMM_NOTELL, false),
            flag_type("nochannels", COMM_NOCHANNELS, false),
            flag_type("snoop_proof", COMM_SNOOP_PROOF, false),
            flag_type("afk", COMM_AFK))

    val vuln_flags = arrayOf(
            flag_type("", 0, false),
            flag_type("summon", VULN_SUMMON),
            flag_type("charm", VULN_CHARM),
            flag_type("magic", VULN_MAGIC),
            flag_type("weapon", VULN_WEAPON),
            flag_type("bash", VULN_BASH),
            flag_type("pierce", VULN_PIERCE),
            flag_type("slash", VULN_SLASH),
            flag_type("fire", VULN_FIRE),
            flag_type("cold", VULN_COLD),
            flag_type("lightning", VULN_LIGHTNING),
            flag_type("acid", VULN_ACID),
            flag_type("poison", VULN_POISON),
            flag_type("negative", VULN_NEGATIVE),
            flag_type("holy", VULN_HOLY),
            flag_type("energy", VULN_ENERGY),
            flag_type("mental", VULN_MENTAL),
            flag_type("disease", VULN_DISEASE),
            flag_type("drowning", VULN_DROWNING),
            flag_type("light", VULN_LIGHT),
            flag_type("sound", VULN_SOUND),
            flag_type("wood", VULN_WOOD),
            flag_type("silver", VULN_SILVER),
            flag_type("iron", VULN_IRON))

    val res_flags = arrayOf(
            flag_type("", 0, false),
            flag_type("summon", RES_SUMMON),
            flag_type("charm", RES_CHARM),
            flag_type("magic", RES_MAGIC),
            flag_type("weapon", RES_WEAPON),
            flag_type("bash", RES_BASH),
            flag_type("pierce", RES_PIERCE),
            flag_type("slash", RES_SLASH),
            flag_type("fire", RES_FIRE),
            flag_type("cold", RES_COLD),
            flag_type("lightning", RES_LIGHTNING),
            flag_type("acid", RES_ACID),
            flag_type("poison", RES_POISON),
            flag_type("negative", RES_NEGATIVE),
            flag_type("holy", RES_HOLY),
            flag_type("energy", RES_ENERGY),
            flag_type("mental", RES_MENTAL),
            flag_type("disease", RES_DISEASE),
            flag_type("drowning", RES_DROWNING),
            flag_type("light", RES_LIGHT),
            flag_type("sound", RES_SOUND),
            flag_type("wood", RES_WOOD),
            flag_type("silver", RES_SILVER),
            flag_type("iron", RES_IRON))
}

class flag_type constructor(var name: String, var bit: Long, var settable: Boolean = true) {

    constructor(name: String, bit: Int, settable: Boolean = true) : this(name, bit.toLong(), settable)

    companion object {
        fun parseFlagsValue(flagsArg: String, table: Array<flag_type>): Long {
            var flags = flagsArg
            var res: Long = 0
            while (flags.isNotEmpty()) {
                val nextFlag = StringBuilder()
                flags = one_argument(flags, nextFlag)
                val flag = table.firstOrNull { nextFlag.toString() == it.name }?.bit ?: 0
                res = res or flag
            }
            return res
        }
    }
}

/** Utter mystical words for an sn.*/
class syl_type(var old: String, var new: String)

val syl_table = arrayOf(
        syl_type(" ", " "),
        syl_type("ar", "abra"),
        syl_type("au", "kada"),
        syl_type("bless", "fido"),
        syl_type("blind", "nose"),
        syl_type("bur", "mosa"),
        syl_type("cu", "judi"),
        syl_type("de", "oculo"),
        syl_type("en", "unso"),
        syl_type("light", "dies"),
        syl_type("lo", "hi"),
        syl_type("mor", "zak"),
        syl_type("move", "sido"),
        syl_type("ness", "lacri"),
        syl_type("ning", "illa"),
        syl_type("per", "duda"),
        syl_type("ra", "gru"),
        syl_type("fresh", "ima"),
        syl_type("re", "candus"),
        syl_type("son", "sabru"),
        syl_type("tect", "infra"),
        syl_type("tri", "cula"),
        syl_type("ven", "nofo"),
        syl_type("a", "a"),
        syl_type("b", "b"),
        syl_type("c", "q"),
        syl_type("d", "e"),
        syl_type("e", "z"),
        syl_type("f", "y"),
        syl_type("g", "o"),
        syl_type("h", "p"),
        syl_type("i", "u"),
        syl_type("j", "y"),
        syl_type("k", "t"),
        syl_type("l", "r"),
        syl_type("m", "w"),
        syl_type("n", "i"),
        syl_type("o", "a"),
        syl_type("p", "s"),
        syl_type("q", "d"),
        syl_type("r", "f"),
        syl_type("s", "g"),
        syl_type("t", "h"),
        syl_type("u", "j"),
        syl_type("v", "z"),
        syl_type("w", "x"),
        syl_type("x", "n"),
        syl_type("y", "l"),
        syl_type("z", "k"),
        syl_type("", ""))
