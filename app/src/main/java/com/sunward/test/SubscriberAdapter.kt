package com.sunward.test

import android.util.Log
import io.reactivex.rxjava3.subscribers.DisposableSubscriber

open class SubscriberAdapter<T> : DisposableSubscriber<T>() {
    override fun onComplete() = Unit
    override fun onError(e: Throwable) {
        Log.e("SubscriberAdapter", "uncaughtException:", e)
    }

    override fun onNext(t: T) = Unit
}