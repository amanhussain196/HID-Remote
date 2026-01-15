package com.example.hidremote

object KeyboardUtils {
    
    data class HidKey(val modifier: Byte, val keycode: Byte)

    private val charMap = HashMap<Char, HidKey>()

    init {
        // a-z
        for (c in 'a'..'z') {
            charMap[c] = HidKey(0, (c.code - 'a'.code + 0x04).toByte())
        }
        // A-Z
        for (c in 'A'..'Z') {
            charMap[c] = HidKey(0x02, (c.code - 'A'.code + 0x04).toByte()) // 0x02 is Left Shift
        }
        // 1-9
        for (c in '1'..'9') {
            charMap[c] = HidKey(0, (c.code - '1'.code + 0x1E).toByte())
        }
        charMap['0'] = HidKey(0, 0x27)
        
        charMap[' '] = HidKey(0, 0x2C)
        charMap['\n'] = HidKey(0, 0x28) // Enter
        charMap['\b'] = HidKey(0, 0x2A) // Backspace
        charMap['.'] = HidKey(0, 0x37)
        charMap[','] = HidKey(0, 0x36)
    }

    fun getHidKey(c: Char): HidKey? {
        return charMap[c]
    }
}
