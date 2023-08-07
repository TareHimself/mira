package com.tarehimself.mira.common

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

open class Cache<KeyType,ValueType>(inMaxSize: Int = Int.MAX_VALUE) {
    open  var maxSize: Int = inMaxSize

    open  var hashmap: HashMap<KeyType,ValueType> = HashMap<KeyType,ValueType>()
    open  var accessLog: ArrayList<KeyType> = ArrayList()
    private val accessLogMutex: Mutex = Mutex()
    open  fun moveKeyToTop(key: KeyType){
        accessLog.remove(key)
        accessLog.add(0,key)
    }

    open fun remove(key: KeyType): ValueType?{
        return hashmap.remove(key)
    }

    open fun contains(key: KeyType): Boolean{
        return hashmap.contains(key)
    }

    open operator fun get(key: KeyType): ValueType?{
        return hashmap[key]
    }

    open operator fun set(key: KeyType, value: ValueType){

        runBlocking {
            accessLogMutex.withLock {
                if(accessLog.contains(key)){
                    hashmap[key] = value
                    moveKeyToTop(key)
                    return@runBlocking
                }

                if(accessLog.size + 1 > maxSize){
                    remove(accessLog.removeAt(accessLog.size - 1))
                }

                accessLog.add(0,key)
                hashmap[key] = value
            }
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

