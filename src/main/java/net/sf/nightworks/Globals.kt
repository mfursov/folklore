package net.sf.nightworks

import net.sf.nightworks.model.Auction
import net.sf.nightworks.model.ROOM_INDEX_DATA

const val MAX_ALIAS = 50
const val MAX_TIME_LOG = 30

const val DEFAULT_PROMPT = "<{c%n{x: %h/%Hhp %m/%Mm %v/%Vmv tnl:%X {c%e{x Opp:%o> "

/*
*  COMMAND extra bits..
*/
const val CMD_KEEP_HIDE = 1
const val CMD_GHOST = 2

/*
* String and memory management parameters.
*/
const val MAX_INPUT_LENGTH = 256
const val PAGE_LEN = 22

/* RT ASCII conversions -- used so we can have letters in this file */
const val A: Long = 1
const val B = 1L shl 1
const val C = 1L shl 2
const val D = 1L shl 3
const val E = 1L shl 4
const val F = 1L shl 5
const val G = 1L shl 6
const val H = 1L shl 7

const val I = 1L shl 8
const val J = 1L shl 9
const val K = 1L shl 10
const val L = 1L shl 11
const val M = 1L shl 12
const val N = 1L shl 13
const val O = 1L shl 14
const val P = 1L shl 15

const val Q = 1L shl 16
const val R = 1L shl 17
const val S = 1L shl 18
const val T = 1L shl 19
const val U = 1L shl 20
const val V = 1L shl 21
const val W = 1L shl 22
const val X = 1L shl 23

const val Y = 1L shl 24
const val Z = 1L shl 25
const val BIT_26 = 1L shl 26
const val BIT_27 = 1L shl 27
const val BIT_28 = 1L shl 28
const val BIT_29 = 1L shl 29
const val BIT_30 = 1L shl 30
const val BIT_31 = 1L shl 31
const val BIT_32 = 1L shl 32
const val BIT_33 = 1L shl 33
const val BIT_34 = 1L shl 34
const val BIT_35 = 1L shl 35
const val BIT_36 = 1L shl 36
const val BIT_37 = 1L shl 37
const val BIT_38 = 1L shl 38
const val BIT_39 = 1L shl 39
const val BIT_40 = 1L shl 40
const val BIT_41 = 1L shl 41
const val BIT_42 = 1L shl 42
const val BIT_43 = 1L shl 43
const val BIT_44 = 1L shl 44
const val BIT_45 = 1L shl 45
const val BIT_46 = 1L shl 46
const val BIT_47 = 1L shl 47
const val BIT_48 = 1L shl 48
const val BIT_49 = 1L shl 49
const val BIT_50 = 1L shl 50
const val BIT_51 = 1L shl 51
const val BIT_52 = 1L shl 52
const val BIT_53 = 1L shl 53
const val BIT_54 = 1L shl 54

const val MPROG_BRIBE = A
const val MPROG_ENTRY = B
const val MPROG_GREET = C
const val MPROG_GIVE = D
const val MPROG_FIGHT = E
const val MPROG_DEATH = F
const val MPROG_AREA = G
const val MPROG_SPEECH = H

const val OPROG_WEAR = A
const val OPROG_REMOVE = B
const val OPROG_DROP = C
const val OPROG_SAC = D
const val OPROG_GIVE = E
const val OPROG_GREET = F
const val OPROG_FIGHT = G
const val OPROG_DEATH = H
const val OPROG_SPEECH = I
const val OPROG_ENTRY = J
const val OPROG_GET = K
const val OPROG_AREA = L

/*
* Game parameters.
*/
const val NIGHTWORKS_REBOOT = 0
const val NIGHTWORKS_SHUTDOWN = 1

const val MAX_SKILL = 426
const val MAX_CLASS = 13
const val MAX_LEVEL = 100
const val LEVEL_HERO = MAX_LEVEL - 9
const val LEVEL_IMMORTAL = MAX_LEVEL - 8

const val PULSE_PER_SCD = 6/* 6 for comm.c */
const val PULSE_PER_SECOND = 4/* for update.c */
const val PULSE_VIOLENCE = 2 * PULSE_PER_SECOND


const val PULSE_MOBILE = 4 * PULSE_PER_SECOND
const val PULSE_WATER_FLOAT = 4 * PULSE_PER_SECOND
const val PULSE_MUSIC = 6 * PULSE_PER_SECOND
const val PULSE_TRACK = 6 * PULSE_PER_SECOND
const val PULSE_TICK = 50 * PULSE_PER_SECOND/* 36 seconds */

/* room_affect_update (not room_update) */
const val PULSE_ROOM_AFFECT = 3 * PULSE_MOBILE
const val PULSE_AREA = 110 * PULSE_PER_SECOND/* 97 seconds */
const val FIGHT_DELAY_TIME = 20 * PULSE_PER_SECOND
const val PULSE_AUCTION = 10 * PULSE_PER_SECOND/* 10 seconds */

const val IMPLEMENTOR = MAX_LEVEL
const val CREATOR = MAX_LEVEL - 1
const val SUPREME = MAX_LEVEL - 2
const val DEITY = MAX_LEVEL - 3
const val GOD = MAX_LEVEL - 4
const val IMMORTAL = MAX_LEVEL - 5
const val DEMI = MAX_LEVEL - 6
const val ANGEL = MAX_LEVEL - 7
const val AVATAR = MAX_LEVEL - 8
const val HERO = LEVEL_HERO


/**
 * minimum pk level
 */
const val PK_MIN_LEVEL = 5

const val MAX_NEWBIES = 120   /* number of newbies allowed */
const val MAX_OLDIES = 999  /* number of oldies allowed */


/*
* Time and weather stuff.
*/
const val SUN_DARK = 0
const val SUN_RISE = 1
const val SUN_LIGHT = 2
const val SUN_SET = 3

const val SKY_CLOUDLESS = 0
const val SKY_CLOUDY = 1
const val SKY_RAINING = 2
const val SKY_LIGHTNING = 3


/*
* TO types for act.
*/
const val TO_ROOM = 0
const val TO_NOTVICT = 1
const val TO_VICT = 2
const val TO_CHAR = 3
const val TO_ALL = 4


const val NOTE_NOTE = 0
const val NOTE_IDEA = 1
const val NOTE_PENALTY = 2
const val NOTE_NEWS = 3
const val NOTE_CHANGES = 4
const val NOTE_INVALID = 100

/* where definitions */
const val TO_AFFECTS = 0
const val TO_OBJECT = 1
const val TO_IMMUNE = 2
const val TO_RESIST = 3
const val TO_VULN = 4
const val TO_WEAPON = 5
const val TO_ACT_FLAG = 6
const val TO_RACE = 8

/* where definitions for room */
const val TO_ROOM_AFFECTS = 0
const val TO_ROOM_CONST = 1
const val TO_ROOM_FLAGS = 2

/* room applies */
const val APPLY_ROOM_NONE = 0
const val APPLY_ROOM_HEAL = 1
const val APPLY_ROOM_MANA = 2

/* AREA FLAGS */
const val AREA_HOMETOWN = A
const val AREA_PROTECTED = B

/*
* ACT bits for mobs.  *ACT*
* Used in #MOBILES.
*/
const val ACT_IS_NPC = A/* Auto set for mobs    */
const val ACT_SENTINEL = B    /* Stays in one room    */
const val ACT_SCAVENGER = C     /* Picks up objects */
const val ACT_AGGRESSIVE = F      /* Attacks PC's     */
const val ACT_STAY_AREA = G   /* Won't leave area */
const val ACT_WIMPY = H
const val ACT_PET = I/* Auto set for pets    */
const val ACT_TRAIN = J /* Can train PC's   */
const val ACT_PRACTICE = K     /* Can practice PC's    */
const val ACT_HUNTER = L
const val ACT_UNDEAD = O
const val ACT_CLERIC = Q
const val ACT_MAGE = R
const val ACT_THIEF = S
const val ACT_WARRIOR = T
const val ACT_NOALIGN = U
const val ACT_NOPURGE = V
const val ACT_OUTDOORS = W
const val ACT_INDOORS = Y
const val ACT_RIDEABLE = Z
const val ACT_IS_HEALER = BIT_26
const val ACT_GAIN = BIT_27
const val ACT_UPDATE_ALWAYS = BIT_28
const val ACT_IS_CHANGER = BIT_29
const val ACT_NOTRACK = BIT_30

/* OFF bits for mobiles *OFF  */
const val OFF_AREA_ATTACK = A
const val OFF_BACKSTAB = B
const val OFF_BASH = C
const val OFF_BERSERK = D
const val OFF_DISARM = E
const val OFF_DODGE = F
const val OFF_FADE = G
const val OFF_FAST = H
const val OFF_KICK = I
const val OFF_KICK_DIRT = J
const val OFF_PARRY = K
const val OFF_RESCUE = L
const val OFF_TAIL = M
const val OFF_TRIP = N
const val OFF_CRUSH = O
const val ASSIST_ALL = P
const val ASSIST_ALIGN = Q
const val ASSIST_RACE = R
const val ASSIST_PLAYERS = S
const val ASSIST_GUARD = T
const val ASSIST_VNUM = U

/* return values for check_imm */
const val IS_NORMAL = 0
const val IS_IMMUNE = 1
const val IS_RESISTANT = 2
const val IS_VULNERABLE = 3

/* IMM bits for mobs */
const val IMM_SUMMON = A
const val IMM_CHARM = B
const val IMM_MAGIC = C
const val IMM_WEAPON = D
const val IMM_BASH = E
const val IMM_PIERCE = F
const val IMM_SLASH = G
const val IMM_FIRE = H
const val IMM_COLD = I
const val IMM_LIGHTNING = J
const val IMM_ACID = K
const val IMM_POISON = L
const val IMM_NEGATIVE = M
const val IMM_HOLY = N
const val IMM_ENERGY = O
const val IMM_MENTAL = P
const val IMM_DISEASE = Q
const val IMM_DROWNING = R
const val IMM_LIGHT = S
const val IMM_SOUND = T
const val IMM_WOOD = X
const val IMM_SILVER = Y
const val IMM_IRON = Z

/* RES bits for mobs *RES */
const val RES_SUMMON = A
const val RES_CHARM = B
const val RES_MAGIC = C
const val RES_WEAPON = D
const val RES_BASH = E
const val RES_PIERCE = F
const val RES_SLASH = G
const val RES_FIRE = H
const val RES_COLD = I
const val RES_LIGHTNING = J
const val RES_ACID = K
const val RES_POISON = L
const val RES_NEGATIVE = M
const val RES_HOLY = N
const val RES_ENERGY = O
const val RES_MENTAL = P
const val RES_DISEASE = Q
const val RES_DROWNING = R
const val RES_LIGHT = S
const val RES_SOUND = T
const val RES_WOOD = X
const val RES_SILVER = Y
const val RES_IRON = Z

/* VULN bits for mobs */
const val VULN_SUMMON = A
const val VULN_CHARM = B
const val VULN_MAGIC = C
const val VULN_WEAPON = D
const val VULN_BASH = E
const val VULN_PIERCE = F
const val VULN_SLASH = G
const val VULN_FIRE = H
const val VULN_COLD = I
const val VULN_LIGHTNING = J
const val VULN_ACID = K
const val VULN_POISON = L
const val VULN_NEGATIVE = M
const val VULN_HOLY = N
const val VULN_ENERGY = O
const val VULN_MENTAL = P
const val VULN_DISEASE = Q
const val VULN_DROWNING = R
const val VULN_LIGHT = S
const val VULN_SOUND = T
const val VULN_WOOD = X
const val VULN_SILVER = Y
const val VULN_IRON = Z

/* body form */
const val FORM_EDIBLE = A
const val FORM_POISON = B
const val FORM_MAGICAL = C
const val FORM_INSTANT_DECAY = D
const val FORM_OTHER = E  /* defined by material bit */

/* actual form */
const val FORM_ANIMAL = G
const val FORM_SENTIENT = H
const val FORM_UNDEAD = I
const val FORM_CONSTRUCT = J
const val FORM_MIST = K
const val FORM_INTANGIBLE = L

const val FORM_BIPED = M
const val FORM_CENTAUR = N
const val FORM_INSECT = O
const val FORM_SPIDER = P
const val FORM_CRUSTACEAN = Q
const val FORM_WORM = R
const val FORM_BLOB = S

const val FORM_MAMMAL = V
const val FORM_BIRD = W
const val FORM_REPTILE = X
const val FORM_SNAKE = Y
const val FORM_DRAGON = Z
const val FORM_AMPHIBIAN = BIT_26
const val FORM_FISH = BIT_27
const val FORM_COLD_BLOOD = BIT_28

/* body parts */
const val PART_HEAD = A
const val PART_ARMS = B
const val PART_LEGS = C
const val PART_HEART = D
const val PART_BRAINS = E
const val PART_GUTS = F
const val PART_HANDS = G
const val PART_FEET = H
const val PART_FINGERS = I
const val PART_EAR = J
const val PART_EYE = K
const val PART_LONG_TONGUE = L
const val PART_EYESTALKS = M
const val PART_TENTACLES = N
const val PART_FINS = O
const val PART_WINGS = P
const val PART_TAIL = Q
/* for combat */
const val PART_CLAWS = U
const val PART_FANGS = V
const val PART_HORNS = W
const val PART_SCALES = X
const val PART_TUSKS = Y

/*
* Bits for 'affected_by'.  *AFF*
* Used in #MOBILES.
*/
const val AFF_BLIND = A
const val AFF_INVISIBLE = B
const val AFF_IMP_INVIS = C
const val AFF_FADE = D
const val AFF_SCREAM = E
const val AFF_BLOODTHIRST = F
const val AFF_STUN = G
const val AFF_SANCTUARY = H
const val AFF_FAERIE_FIRE = I
const val AFF_INFRARED = J
const val AFF_CURSE = K
const val AFF_CORRUPTION = L
const val AFF_POISON = M
const val AFF_PROTECT_EVIL = N
const val AFF_PROTECT_GOOD = O
const val AFF_SNEAK = P
const val AFF_HIDE = Q
const val AFF_SLEEP = R
const val AFF_CHARM = S
const val AFF_FLYING = T
const val AFF_PASS_DOOR = U
const val AFF_HASTE = V
const val AFF_CALM = W
const val AFF_PLAGUE = X
const val AFF_WEAKEN = Y
const val AFF_WEAK_STUN = Z
const val AFF_BERSERK = BIT_26
const val AFF_SWIM = BIT_27
const val AFF_REGENERATION = BIT_28
const val AFF_SLOW = BIT_29
const val AFF_CAMOUFLAGE = BIT_30

const val AFF_DETECT_IMP_INVIS = BIT_31
const val AFF_DETECT_FADE = BIT_32
const val AFF_DETECT_EVIL = BIT_33
const val AFF_DETECT_INVIS = BIT_34
const val AFF_DETECT_MAGIC = BIT_35
const val AFF_DETECT_HIDDEN = BIT_36
const val AFF_DETECT_GOOD = BIT_37
const val AFF_DETECT_SNEAK = BIT_38
const val AFF_DETECT_UNDEAD = BIT_39

const val AFF_AURA_CHAOS = BIT_40
const val AFF_PROTECTOR = BIT_41
const val AFF_SUFFOCATE = BIT_42
const val AFF_EARTHFADE = BIT_43
const val AFF_FEAR = BIT_44
const val AFF_FORM_TREE = BIT_45
const val AFF_FORM_GRASS = BIT_46
const val AFF_WEB = BIT_47
const val AFF_LION = BIT_48
const val AFF_GROUNDING = BIT_49
const val AFF_ABSORB = BIT_50
const val AFF_SPELLBANE = BIT_51

const val AFF_DETECT_LIFE = BIT_52
const val AFF_DARK_VISION = BIT_53
const val AFF_ACUTE_VISION = BIT_54

/*
* *AFF* bits for rooms
*/
const val AFF_ROOM_SHOCKING = A
const val AFF_ROOM_L_SHIELD = B
const val AFF_ROOM_THIEF_TRAP = C
const val AFF_ROOM_RANDOMIZER = D
const val AFF_ROOM_ESPIRIT = E
const val AFF_ROOM_PREVENT = F
const val AFF_ROOM_CURSE = K
const val AFF_ROOM_POISON = M
const val AFF_ROOM_SLEEP = R
const val AFF_ROOM_PLAGUE = X
const val AFF_ROOM_SLOW = BIT_29

/* AC types */
const val AC_PIERCE = 0
const val AC_BASH = 1
const val AC_SLASH = 2
const val AC_EXOTIC = 3

/* dice */
const val DICE_NUMBER = 0
const val DICE_TYPE = 1
const val DICE_BONUS = 2


/*
* Extra flags.  *EXT*
* Used in #OBJECTS.
*/
const val ITEM_GLOW = A
const val ITEM_HUM = B
const val ITEM_DARK = C
const val ITEM_LOCK = D
const val ITEM_EVIL = E
const val ITEM_INVIS = F
const val ITEM_MAGIC = G
const val ITEM_NODROP = H
const val ITEM_BLESS = I
const val ITEM_ANTI_GOOD = J
const val ITEM_ANTI_EVIL = K
const val ITEM_ANTI_NEUTRAL = L
const val ITEM_NOREMOVE = M
const val ITEM_INVENTORY = N
const val ITEM_NOPURGE = O
const val ITEM_ROT_DEATH = P
const val ITEM_VIS_DEATH = Q
const val ITEM_NOSAC = R
const val ITEM_NONMETAL = S
const val ITEM_NOLOCATE = T
const val ITEM_MELT_DROP = U
const val ITEM_HAD_TIMER = V
const val ITEM_SELL_EXTRACT = W
const val ITEM_BURN_PROOF = Y
const val ITEM_NOUNCURSE = Z
const val ITEM_NOSELL = BIT_26
const val ITEM_BURIED = BIT_27

/*
* Wear flags.   *WEAR*
* Used in #OBJECTS.
*/
const val ITEM_TAKE = A
const val ITEM_WEAR_FINGER = B
const val ITEM_WEAR_NECK = C
const val ITEM_WEAR_BODY = D
const val ITEM_WEAR_HEAD = E
const val ITEM_WEAR_LEGS = F
const val ITEM_WEAR_FEET = G
const val ITEM_WEAR_HANDS = H
const val ITEM_WEAR_ARMS = I
const val ITEM_WEAR_SHIELD = J
const val ITEM_WEAR_ABOUT = K
const val ITEM_WEAR_WAIST = L
const val ITEM_WEAR_WRIST = M
const val ITEM_WIELD = N
const val ITEM_HOLD = O
const val ITEM_NO_SAC = P
const val ITEM_WEAR_FLOAT = Q
const val ITEM_WEAR_TATTOO = R

/* weapon types */
const val WEAPON_FLAMING = A
const val WEAPON_FROST = B
const val WEAPON_VAMPIRIC = C
const val WEAPON_SHARP = D
const val WEAPON_VORPAL = E
const val WEAPON_TWO_HANDS = F
const val WEAPON_SHOCKING = G
const val WEAPON_POISON = H
const val WEAPON_HOLY = I
const val WEAPON_KATANA = J

/* gate flags */
const val GATE_NORMAL_EXIT = A
const val GATE_NOCURSE = B
const val GATE_GOWITH = C
const val GATE_BUGGY = D
const val GATE_RANDOM = E

/* furniture flags */
const val STAND_AT = A
const val STAND_ON = B
const val STAND_IN = C
const val SIT_AT = D
const val SIT_ON = E
const val SIT_IN = F
const val REST_AT = G
const val REST_ON = H
const val REST_IN = I
const val SLEEP_AT = J
const val SLEEP_ON = K
const val SLEEP_IN = L
const val PUT_AT = M
const val PUT_ON = N
const val PUT_IN = O
const val PUT_INSIDE = P

/*
* Values for containers (value[1]).
* Used in #OBJECTS.
*/
const val CONT_CLOSEABLE: Long = 1
const val CONT_PICKPROOF: Long = 2
const val CONT_CLOSED: Long = 4
const val CONT_LOCKED: Long = 8
const val CONT_PUT_ON: Long = 16
const val CONT_FOR_ARROW: Long = 32
const val CONT_ST_LIMITED: Long = 64

/*
* Room flags.
* Used in #ROOMS.
*/
const val ROOM_DARK = A
const val ROOM_NO_MOB = C
const val ROOM_INDOORS = D
const val ROOM_PRIVATE = J
const val ROOM_SAFE = K
const val ROOM_SOLITARY = L
const val ROOM_PET_SHOP = M
const val ROOM_NO_RECALL = N
const val ROOM_IMP_ONLY = O
const val ROOM_GODS_ONLY = P
const val ROOM_HEROES_ONLY = Q
const val ROOM_NEWBIES_ONLY = R
const val ROOM_LAW = S
const val ROOM_NOWHERE = T
const val ROOM_BANK = U
const val ROOM_NO_MAGIC = W
const val ROOM_NOSUMMON = X
const val ROOM_REGISTRY = BIT_27

/*
* Directions.
* Used in #ROOMS.
*/
const val DIR_NORTH = 0
const val DIR_EAST = 1
const val DIR_SOUTH = 2
const val DIR_WEST = 3
const val DIR_UP = 4
const val DIR_DOWN = 5

/*
* Exit flags.
* Used in #ROOMS.
*/
const val EX_IS_DOOR = A
const val EX_CLOSED = B
const val EX_LOCKED = C
const val EX_NO_FLEE = D
const val EX_PICK_PROOF = F
const val EX_NO_PASS = G
const val EX_EASY = H
const val EX_HARD = I
const val EX_INFURIATING = J
const val EX_NO_CLOSE = K
const val EX_NO_LOCK = L

/*
* Sector types.
* Used in #ROOMS.
*/
const val SECT_INSIDE = 0
const val SECT_CITY = 1
const val SECT_FIELD = 2
const val SECT_FOREST = 3
const val SECT_HILLS = 4
const val SECT_MOUNTAIN = 5
const val SECT_WATER_SWIM = 6
const val SECT_WATER_NO_SWIM = 7
const val SECT_UNUSED = 8
const val SECT_AIR = 9
const val SECT_DESERT = 10
const val SECT_MAX = 11

/**
 * ************************************************************************
 * *
 * VALUES OF INTEREST TO AREA BUILDERS                   *
 * (End of this section ... stop here)                   *
 * *
 * *************************************************************************
 */

/* Conditions. */
const val COND_DRUNK = 0
const val COND_FULL = 1
const val COND_THIRST = 2
const val COND_HUNGER = 3
const val COND_BLOODLUST = 4
const val COND_DESIRE = 5


/*
* ACT bits for players.
*/
const val PLR_BOUGHT_PET = B

/* RT auto flags */
const val PLR_AUTO_ASSIST = C
const val PLR_AUTO_EXIT = D
const val PLR_AUTO_LOOT = E
const val PLR_AUTO_SAC = F
const val PLR_AUTO_GOLD = G
const val PLR_AUTO_SPLIT = H
const val PLR_COLOR = I
const val PLR_WANTED = J
const val PLR_NO_TITLE = K
/* RT personal flags */
const val PLR_NO_EXP = L
const val PLR_CHANGED_AFF = M
const val PLR_HOLYLIGHT = N
const val PLR_NOCANCEL = O
const val PLR_CANLOOT = P
const val PLR_NOSUMMON = Q
const val PLR_NOFOLLOW = R
const val PLR_CANINDUCT = S
const val PLR_GHOST = T

/* penalty flags */
const val PLR_PERMIT = U
const val PLR_REMORTED = V
const val PLR_LOG = W
const val PLR_DENY = X
const val PLR_FREEZE = Y
const val PLR_LEFTHAND = Z
const val PLR_CANREMORT = BIT_26
const val PLR_QUESTOR = BIT_27
const val PLR_VAMPIRE = BIT_28
const val PLR_HARA_KIRI = BIT_29
const val PLR_BLINK_ON = BIT_30

/* The Quests */
const val QUEST_EYE = B
const val QUEST_WEAPON = C
const val QUEST_GIRTH = D
const val QUEST_RING = E
const val QUEST_WEAPON2 = F
const val QUEST_GIRTH2 = G
const val QUEST_RING2 = H
const val QUEST_WEAPON3 = I
const val QUEST_GIRTH3 = J
const val QUEST_RING3 = K
const val QUEST_BACKPACK = L
const val QUEST_BACKPACK2 = M
const val QUEST_BACKPACK3 = N
const val QUEST_DECANTER = O
const val QUEST_DECANTER2 = P
const val QUEST_DECANTER3 = Q

const val QUEST_PRACTICE = S

/* time log problems */
const val TLP_NOLOG = A
const val TLP_BOOT = B

/* RT comm flags -- may be used on both mobs and chars */
const val COMM_QUIET = A
const val COMM_DEAF = B
const val COMM_NOWIZ = C
const val COMM_NOAUCTION = D
const val COMM_NOGOSSIP = E
const val COMM_NOQUESTION = F
const val COMM_NOMUSIC = G
const val COMM_NOQUOTE = I
const val COMM_SHOUTSOFF = J

/* display flags */
const val COMM_true_TRUST = K
const val COMM_COMPACT = L
const val COMM_BRIEF = M
const val COMM_PROMPT = N
const val COMM_COMBINE = O
const val COMM_TELNET_GA = P
const val COMM_SHOW_AFFECTS = Q
const val COMM_NOGRATS = R

/* penalties */
const val COMM_NOEMOTE = T
const val COMM_NOSHOUT = U
const val COMM_NOTELL = V
const val COMM_NOCHANNELS = W
const val COMM_SNOOP_PROOF = Y
const val COMM_AFK = Z


/*
*  Target types.
*/
const val TAR_IGNORE = 0
const val TAR_CHAR_OFFENSIVE = 1
const val TAR_CHAR_DEFENSIVE = 2
const val TAR_CHAR_SELF = 3
const val TAR_OBJ_INV = 4
const val TAR_OBJ_CHAR_DEF = 5
const val TAR_OBJ_CHAR_OFF = 6

const val TARGET_CHAR = 0
const val TARGET_OBJ = 1
const val TARGET_ROOM = 2
const val TARGET_NONE = 3

/*
* Global variables.
*/
val nw_config = Configuration()

var auction: Auction? = null

var top_affected_room: ROOM_INDEX_DATA? = null

var current_time: Long = 0
var limit_time: Long = 0
var boot_time: Long = 0
var fLogAll: Boolean = false
var total_levels: Int = 0
var reboot_counter: Int = 0
var time_sync: Int = 0
var max_newbies: Int = 0
var max_oldies: Int = 0
var iNumPlayers: Int = 0

val dir_name = arrayOf("north", "east", "south", "west", "up", "down")
val rev_dir = intArrayOf(2, 3, 0, 1, 5, 4)
