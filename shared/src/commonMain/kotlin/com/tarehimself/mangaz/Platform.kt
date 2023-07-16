package com.tarehimself.mangaz

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform