package net.sf.nightworks

/** Damage Type */
enum class DT(val immuneFlag: Long = 0) {
    None,
    Bash(IMM_BASH),
    Pierce(IMM_PIERCE),
    Slash(IMM_SLASH),
    Fire(IMM_FIRE),
    Cold(IMM_COLD),
    Lightning(IMM_LIGHTNING),
    Acid(IMM_ACID),
    Poison(IMM_POISON),
    Negative(IMM_NEGATIVE),
    Holy(IMM_HOLY),
    Energy(IMM_ENERGY),
    Mental(IMM_MENTAL),
    Disease(IMM_DISEASE),
    Drowing(IMM_DROWNING),
    Light(IMM_LIGHT),
    Other,
    Harm,
    Charm(IMM_CHARM),
    Sound(IMM_SOUND),
    Thirst,
    Hunger,
    RoomLight,
    Trap;

    fun isWeapon() = this === Bash || this === Pierce || this === Slash
}