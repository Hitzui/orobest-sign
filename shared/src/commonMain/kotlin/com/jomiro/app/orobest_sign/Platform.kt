package com.jomiro.app.orobest_sign

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform