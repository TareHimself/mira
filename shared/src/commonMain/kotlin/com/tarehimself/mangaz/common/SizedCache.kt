package com.tarehimself.mangaz.common

class SizedCache<KeyType,ValueType>(inMaxSize: Int = 70) {
    private var maxSize: Int = inMaxSize

    private var hashmap: HashMap<KeyType,ValueType> = HashMap<KeyType,ValueType>()
    private var keys: ArrayList<KeyType> = ArrayList()

    private fun moveKeyToTop(key: KeyType){
        keys.remove(key)
        keys.add(0,key)
    }

    private fun remove(key: KeyType): ValueType?{
        return hashmap.remove(key)
    }

    fun contains(key: KeyType): Boolean{
        return hashmap.contains(key)
    }

    operator fun get(key: KeyType): ValueType?{
        return hashmap[key]
    }

    operator fun set(key: KeyType, value: ValueType){
        if(keys.contains(key)){
            hashmap[key] = value
            moveKeyToTop(key)
            return
        }

        if(keys.size + 1 > maxSize){
            remove(keys.removeAt(keys.size - 1))
        }

        keys.add(0,key)
        hashmap[key] = value
    }
}