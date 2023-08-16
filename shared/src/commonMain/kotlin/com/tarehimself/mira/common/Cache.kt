package com.tarehimself.mira.common

import io.github.aakira.napier.Napier
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

open class Cache<KeyType,ValueType>(
    private val maxSize: Float = Float.MAX_VALUE,
    val sizeOf: (ValueType) -> Float = { 1.0f }
) {
    var currentSize = 0.0f

    open  var hashmap: HashMap<KeyType,ValueType> = HashMap()
    open  var accessLog: LinkedHashSet<KeyType> = LinkedHashSet()
    private val accessLogMutex: Mutex = Mutex()

    open fun contains(key: KeyType): Boolean{
        return hashmap.contains(key)
    }

    open operator fun get(key: KeyType): ValueType?{
        return hashmap[key]
    }

    suspend fun put(key: KeyType, value: ValueType): Boolean{
        val newItemSize = sizeOf(value)
        accessLogMutex.withLock {

            if(newItemSize > maxSize){
                return false
            }

            if(accessLog.contains(key)){
                accessLog.remove(key)
                currentSize -= sizeOf(hashmap[key]!!)
                hashmap.remove(key)
            }

            while((currentSize + newItemSize) > maxSize){
                val toRemove = accessLog.firstOrNull()
                if(toRemove === null){
                    return false
                }
                accessLog.remove(toRemove)
                currentSize -= sizeOf(hashmap[toRemove]!!)
                hashmap.remove(toRemove)
            }


            accessLog.add(key)
            hashmap[key] = value
            currentSize += newItemSize

            return true
        }
    }

    fun putBlocking(key: KeyType, value: ValueType): Boolean{
        return runBlocking {
            put(key,value)
        }
    }

    val values get(): MutableCollection<ValueType>{
        return hashmap.values
    }

    val keys get(): MutableSet<KeyType>{
        return hashmap.keys
    }
}

//data class TimedValue<ValueType>(val value: ValueType,val time: Long)
//open class TimedCache<KeyType,ValueType>(inMaxSize: Int = Int.MAX_VALUE,inMaxCacheTime) : Cache<KeyType,TimedValue<ValueType>>(inMaxSize=inMaxSize){
//
//    val maxCacheTime = inMaxCacheTime
//
//    fun now(){
//
//    }
//    override operator fun get(key: KeyType): ValueType?{
//
//        return hashmap[key]
//    }
//    operator fun set(key: KeyType, value: ValueType){
//
//        super.set(key, TimedValue(value, ))
//    }
//}

