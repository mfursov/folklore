package net.sf.nightworks.model

import net.sf.nightworks.PULSE_AUCTION

class Auction(

        /** active item */
        var item: Obj,

        /** a pointer to the seller - which may NOT quit */
        var seller: CHAR_DATA,

        /** a pointer to the buyer - which may NOT quit */
        var buyer: CHAR_DATA? = null,

        /** last bet - or 0 if noone has bet anything */
        var bet: Int = 0,

        /** 1,2, sold */
        var going: Int = 0,

        /** how many pulses (.25 sec) until another call-out ? */
        var pulse: Int = PULSE_AUCTION
)
