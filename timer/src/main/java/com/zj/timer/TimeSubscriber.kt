package com.zj.timer

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

internal class TimeSubscriber {

    private val subscribers = ConcurrentHashMap<String, Subscriber>()

    fun observer(lw: LifecycleOwner, key: String, observer: Observer<Long>) {
        val subscriber = subscribers[key] ?: run {
            val s = Subscriber()
            subscribers[key] = s
            s
        }
        subscriber.observer(lw, observer)
    }

    fun observerForever(key: String, observer: Observer<Long>) {
        val subscriber = subscribers[key] ?: run {
            val s = Subscriber()
            subscribers[key] = s
            s
        }
        subscriber.observerForever(observer)
    }

    fun pause(key: String) {
        subscribers[key]?.pause()
    }

    fun resume(key: String) {
        subscribers[key]?.resume()
    }

    fun remove(key: String, observer: Observer<Long>): Boolean {
        val result = subscribers[key]?.removeObserver(observer) ?: false
        if (result) subscribers.remove(key)
        return subscribers.isNullOrEmpty()
    }

    fun remove(observer: Observer<Long>): Boolean {
        val keys = mutableListOf<String>()
        subscribers.forEach { (k, v) ->
            if (v.removeObserver(observer)) keys.add(k)
        }
        keys.forEach {
            subscribers.remove(it)
        }
        return subscribers.isNullOrEmpty()
    }

    fun remove(lw: LifecycleOwner, key: String): Boolean {
        val result = subscribers[key]?.remove(lw) ?: false
        if (result) subscribers.remove(key)
        return subscribers.isNullOrEmpty()
    }

    fun hasObservers(key: String): Boolean {
        return subscribers[key]?.hasObservers() ?: false
    }

    fun increasesTo(interval: Long, period: Long) {
        subscribers.forEach { (_, v) ->
            v.increasesTo(interval, period)
        }
    }

    inner class Subscriber {
        private var isRunning = AtomicBoolean(true)
        private var lvData = MutableLiveData<Long>()
        private var increasesValue = 0L

        fun increasesTo(interval: Long, period: Long) {
            if (!isRunning.get()) return
            if (increasesValue + period >= interval) {
                increasesValue = 0L
                if (lvData.hasObservers()) {
                    lvData.postValue(interval)
                }
            } else {
                increasesValue += period
            }
        }

        fun hasObservers(): Boolean {
            return lvData.hasObservers()
        }

        fun pause() {
            isRunning.set(false)
        }

        fun resume() {
            isRunning.set(true)
        }

        fun observer(lw: LifecycleOwner, observer: Observer<Long>) {
            lvData.observe(lw, observer)
        }

        fun observerForever(observer: Observer<Long>) {
            lvData.observeForever(observer)
        }

        fun removeObserver(observer: Observer<Long>): Boolean {
            increasesValue = 0
            lvData.removeObserver(observer)
            return !hasObservers()
        }

        fun remove(lw: LifecycleOwner): Boolean {
            increasesValue = 0
            lvData.removeObservers(lw)
            return !hasObservers()
        }
    }
}