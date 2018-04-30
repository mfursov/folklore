package net.sf.nightworks.model

enum class Wear(val id: Int, val where: String, val acMult: Int = 1, val max: Int = 1) {
    None(-1, "", acMult = 0, max = 0),
    Finger(0, "<worn on finger>    ", max = 4),
    Neck(1, "<worn around neck>  ", max = 2),
    Body(2, "<worn on torso>     ", acMult = 3),
    Head(3, "<worn on head>      ", acMult = 2),
    Legs(4, "<worn on legs>      ", acMult = 2),
    Feet(5, "<worn on feet>      "),
    Hands(6, "<worn on hands>     "),
    Arms(7, "<worn on arms>      "),
    About(8, "<worn about body>   ", acMult = 2),
    Waist(9, "<worn about waist>  "),
    Wrist(10, "<worn around wrist> ", max = 2),
    Left(11, "<left hand holds>%c  "),
    Right(12, "<right hand holds>%c "),
    Both(13, "<both hands hold>   "),
    Float(14, "<floating nearby>   ", acMult = 0),
    Tattoo(15, "<scratched tattoo>  ", acMult = 0, max = 2),
    StuckIn(16, "<stuck in>          ", acMult = 0, max = 100);

    fun isHold() = this === Left || this === Right || this == Both
    fun isNone() = this === None
    fun isOn() = this != None

    companion object {
        fun of(id: Int, def: (w: Int) -> Wear) = values().firstOrNull { it.id == id } ?: def(id)

        fun ofOld(oldwear: Int) = when (oldwear) {
            1, 2 -> Finger
            3, 4 -> Neck
            5 -> Body
            6 -> Head
            7 -> Legs
            8 -> Feet
            9 -> Hands
            10 -> Arms
            11 -> Left
            12 -> About
            13 -> Waist
            14, 15 -> Wrist
            16 -> Right
            18 -> Float
            19 -> Tattoo
            21 -> StuckIn
            else -> None
        }
    }
}