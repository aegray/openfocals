package com.openfocals.focals.events;

import com.openfocals.focals.messages.BTMessageToBuddy;

import okio.Buffer;

public class FocalsBluetoothMessageEvent {
    //public LCompanion message;
    public BTMessageToBuddy message;
    public Buffer buffer;

    public FocalsBluetoothMessageEvent(BTMessageToBuddy msg, Buffer b)
    {
        message = msg;
        buffer = b;
    }

    public FocalsBluetoothMessageEvent(BTMessageToBuddy msg)
    {
        this(msg, null); //FocalsBluetoothMessageEvent(msg, null);
    }
}
