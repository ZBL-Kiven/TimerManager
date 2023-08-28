package com.zj.timer

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Suppress("MemberVisibilityCanBePrivate", "unused")
object TimerManager {

    private const val TIME_INTERVAL = 16L

    private var timer: Timer? = null
    private var timerTask: CCTimerTask? = null
    private var isInit: Boolean = false
        get() {
            return field && (timerTask != null && timer != null)
        }

    private val timeNodeSubscribers = ConcurrentHashMap<Long, TimeSubscriber>()

    /**
     * If it is not necessary, don't use the same KEY to add to different time-lines to avoid notification confusion
     * */
    fun addObserver(lw: LifecycleOwner, interval: Long, key: String, observer: Observer<Long>) {
        startTimer()
        val timeSubscriber: TimeSubscriber = timeNodeSubscribers[interval] ?: run {
            val ts = TimeSubscriber()
            timeNodeSubscribers[interval] = ts
            ts
        }
        runInMainThread {
            timeSubscriber.observer(lw, key, observer)
        }
    }

    fun addObserverForever(interval: Long, key: String, observer: Observer<Long>) {
        removeObserver(key, observer)
        startTimer()
        val timeSubscriber: TimeSubscriber = timeNodeSubscribers[interval] ?: run {
            val ts = TimeSubscriber()
            timeNodeSubscribers[interval] = ts
            ts
        }
        runInMainThread {
            timeSubscriber.observerForever(key, observer)
        }
    }

    /**
     * add mutually exclusive observers. Only one observer of the same key is allowed to exist,
     * Always remove similar keys regardless of whether they are exactly the same as the newly injected observer
     * */
    fun addObserverUnique(lw: LifecycleOwner, interval: Long, key: String, observer: Observer<Long>) {
        removeObserver(lw, key)
        startTimer()
        val timeSubscriber: TimeSubscriber = timeNodeSubscribers[interval] ?: run {
            val ts = TimeSubscriber()
            timeNodeSubscribers[interval] = ts
            ts
        }
        runInMainThread {
            timeSubscriber.observer(lw, key, observer)
        }
    }

    fun hasObservers(interval: Long, key: String) {
        timeNodeSubscribers[interval]?.hasObservers(key)
    }

    fun pause(interval: Long, key: String) {
        timeNodeSubscribers[interval]?.pause(key)
    }

    fun pause(key: String) {
        timeNodeSubscribers.forEach { (_, v) ->
            v.pause(key)
        }
    }

    fun resume(interval: Long, key: String) {
        timeNodeSubscribers[interval]?.resume(key)
    }

    fun resume(key: String) {
        timeNodeSubscribers.forEach { (_, v) ->
            v.resume(key)
        }
    }

    /**
     * Remove all observers by key
     * */
    fun removeObserver(lw: LifecycleOwner, key: String) {
        runInMainThread {
            val keys = mutableListOf<Long>()
            timeNodeSubscribers.forEach { (k, v) ->
                if (v.remove(lw, key)) keys.add(k)
            }
            keys.forEach {
                timeNodeSubscribers.remove(it)
            }
        }
    }

    fun removeObserver(key: String, observer: Observer<Long>) {
        runInMainThread {
            val keys = mutableListOf<Long>()
            timeNodeSubscribers.forEach { (k, v) ->
                if (v.remove(key, observer)) keys.add(k)
            }
            keys.forEach {
                timeNodeSubscribers.remove(it)
            }
        }
    }

    fun removeObserver(observer: Observer<Long>) {
        runInMainThread {
            val keys = mutableListOf<Long>()
            timeNodeSubscribers.forEach { (k, v) ->
                if (v.remove(observer)) keys.add(k)
            }
            keys.forEach {
                timeNodeSubscribers.remove(it)
            }
        }
    }

    private fun startTimer() {
        if (isInit) return
        if (timer == null) {
            timer = Timer("cc_global_timer_task")
        }
        timerTask?.cancel()
        timerTask = CCTimerTask()
        timer?.schedule(timerTask, TIME_INTERVAL, TIME_INTERVAL)
        isInit = true
    }

    fun release(clearObserver: Boolean = false) {
        try {
            timer?.cancel()
            timerTask?.cancel()
            timerTask = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        timer = null
        isInit = false
        if (clearObserver) timeNodeSubscribers.clear()
    }

    private class CCTimerTask : TimerTask() {
        override fun run() {
            synchronized(timeNodeSubscribers) syncRun@{
                if (timeNodeSubscribers.isNullOrEmpty()) return@syncRun
                timeNodeSubscribers.forEach { (k, v) ->
                    v.increasesTo(k, TIME_INTERVAL)
                }
            }
        }
    }

    fun onAppStateChanged(inBackground: Boolean) {
        if (inBackground) release(false) else startTimer()
    }

    private fun runInMainThread(r: () -> Unit) {
        if (Thread.currentThread() != Looper.getMainLooper().thread) {
            Handler(Looper.getMainLooper()).post {
                r()
            }
        } else r()
    }

}