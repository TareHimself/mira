package com.tarehimself.mira.common

actual interface Logger {
    // Add more logging methods as needed
    actual fun debug(tag: String, message: String)
}