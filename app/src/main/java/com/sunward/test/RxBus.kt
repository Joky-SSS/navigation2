package com.sunward.test

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.processors.FlowableProcessor
import io.reactivex.rxjava3.processors.PublishProcessor
import io.reactivex.rxjava3.schedulers.Schedulers

class RxBus private constructor() {
    private val mBus: FlowableProcessor<Any> = PublishProcessor.create<Any>().toSerialized()

    fun post(m: Any) {
        mBus.onNext(m)
    }

    fun <T> toFlowable(eventType: Class<T>?): Flowable<T> {
        return mBus.ofType(eventType).onBackpressureBuffer().subscribeOn(Schedulers.computation())
    }

    fun hasSubscribers(): Boolean {
        return mBus.hasSubscribers()
    }

    private object Holder {
        val BUS = RxBus()
    }

    companion object {
        fun get(): RxBus {
            return Holder.BUS
        }
    }

}