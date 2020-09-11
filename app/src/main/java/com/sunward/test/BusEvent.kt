package com.sunward.test

data class SendEvent(var command: Command, var data: Any? = null)

data class ReceiverEvent(var command: Command, var data: Any? = null)

data class LocationEvent(var command: Command)

enum class Command(val value: String) {
    DO_CONNECT("DO_CONNECT"),
    DO_DISCONNECT("DO_DISCONNECT"),
    STATE_CONNECTED("STATE_CONNECTED"),
    STATE_DISCONNECTED("STATE_DISCONNECTED"),
    REGISTER("REGISTER"),
    CONNECT_STATE("CONNECT_STATE"),
    HEADINGA("HEADINGA"),
    BESTPOSA("BESTPOSA"),
    GPYBM("GPYBM"),
    REFSTATIONA("REFSTATIONA"),
    SETTING_OK("SETTING_OK"),
    SETTING_ERROR("SETTING_ERROR"),
    VERSIONA("VERSIONA")
}