package com.tarehimself.mira.common

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext

open class Cache<KeyType,ValueType>(
    private val maxSize: Float = Float.MAX_VALUE,
    val sizeOf: (value: ValueType) -> Float = { 1.0f },
    val onItemRemoved: (key: KeyType,value: ValueType) -> Unit = { _,_ -> },
    val onItemAdded: (key: KeyType,value: ValueType) -> Unit = { _,_ -> },
    val onFailedToAddItem: (key: KeyType,value: ValueType) -> Unit = { _,_ -> }
) {
    val size = mutableStateOf(0.0f)

    var hashmap: HashMap<KeyType,ValueType> = HashMap()

    var usage: LinkedHashSet<KeyType> = LinkedHashSet()


    private val mutex: Mutex = Mutex()

    fun contains(key: KeyType): Boolean{
        return hashmap.contains(key)
    }

    fun containsValue(value: ValueType): Boolean{
        return hashmap.containsValue(value)
    }

    fun containsValueWithLock(value: ValueType,result: (contains: Boolean) -> Unit){
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            mutex.withLock {
                result(containsValue(value))
            }
        }
    }

    operator fun get(key: KeyType): ValueType?{
        return runBlocking {
            getAsync(key)
        }
    }

    suspend fun getAsync(key: KeyType): ValueType ? {
        mutex.withLock {
            if(contains(key)){
                usage.remove(key)
                usage.add(key)
            }
            return hashmap[key]
        }
    }

    suspend fun put(key: KeyType, value: ValueType): Boolean{
        mutex.withLock {

            val newItemSize = sizeOf(value)

            if(newItemSize > maxSize){
                onFailedToAddItem(key,value)
                return false
            }

            if(usage.contains(key)){
                usage.remove(key)
                size.value -= sizeOf(hashmap[key]!!)
                hashmap.remove(key)?.let {
                    onItemRemoved(key,it)
                }
            }

            while((size.value + newItemSize) > maxSize){
                val toRemove = usage.firstOrNull()
                if(toRemove === null){
                    return false
                }
                usage.remove(toRemove)
                size.value -= sizeOf(hashmap[toRemove]!!)
                hashmap.remove(toRemove)?.let {
                    onItemRemoved(toRemove,it)
                }

            }


            usage.add(key)
            hashmap[key] = value
            size.value += newItemSize

            onItemAdded(key,value)

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

    suspend fun clear(){
        mutex.withLock {
            usage.forEach {
                hashmap.remove(it)?.let { value ->
                    onItemRemoved(it,value)
                }
            }
            usage.clear()
            size.value = 0.0f
        }
    }
}

