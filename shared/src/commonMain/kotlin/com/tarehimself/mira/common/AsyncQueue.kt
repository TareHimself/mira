package com.tarehimself.mira.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.suspendCoroutine

class AsyncQueue<T> {

    private val dequeue = ArrayDeque<T>()
    private val getMutex = Mutex()
    private val putMutex = Mutex()

    private var awaitingPutCallback: ((data: T) -> Unit)? = null

    val pending: Int
        get() {
            return dequeue.size
        }

    suspend fun get(): T {
        return getMutex.withLock {
            if(dequeue.isEmpty()){
                suspendCoroutine { continuation -> awaitingPutCallback = {
                    continuation.resumeWith(Result.success(it))
                } }
            }
            else
            {
                dequeue.removeFirst()
            }
        }
    }

    suspend fun put(data: T){
        putMutex.withLock {
            awaitingPutCallback?.let {
                it(data)
                awaitingPutCallback = null
            } ?: run {
                dequeue.addLast(data);
            }
        }
    }

    fun remove(data: T): Boolean {
        return dequeue.remove(data)
    }

    fun has(data: T): Boolean {
        return dequeue.contains(data)
    }

    fun toList(): List<T>{
        return dequeue.toList()
    }
}