package com.spcrey.blog.tools

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object MessageReceiving {
    interface CountCompleteListener {
        suspend fun run()
    }
    private var countCompleteListener: CountCompleteListener? = null

    fun setCountCompleteListener(listener: CountCompleteListener) {
        this.countCompleteListener = listener
    }

    interface CountCompleteAfterListener {
        suspend fun run()
    }
    private var countCompleteAfterListener: CountCompleteAfterListener? = null
    fun setCountCompleteAfterListener(listener: CountCompleteAfterListener) {
        this.countCompleteAfterListener = listener
    }


    fun clearCompleteListener() {
        countCompleteListener = null
    }

    fun clearCompleteAfterListener() {
        countCompleteAfterListener = null
    }

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    var max_count = 5
    var current_count = 0
    fun run() {
        job = scope.launch {
            while (true) {
                if (current_count >= max_count) {
                    countCompleteListener?.run()
                    countCompleteAfterListener?.run()
                    current_count = 0
                }
                delay(1000)
                current_count += 1
            }
        }
    }
    fun stop() {
        job?.cancel()
        current_count = 0
    }

    @JvmStatic
    fun main(args: Array<String>) {

        setCountCompleteListener(object : CountCompleteListener{
            override suspend fun run() {
                println(1)
                delay(1000)
                println(2)
                delay(1000)
                println(3)
            }
        })
        runBlocking {
            launch(Dispatchers.Default) {
                run()
                delay(6000)
                stop()
                run()
                delay(30000)
            }
        }
    }
}