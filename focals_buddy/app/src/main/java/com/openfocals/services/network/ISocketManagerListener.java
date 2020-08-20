package com.openfocals.services.network;

import okio.Buffer;

public interface ISocketManagerListener {
    public abstract void onSocketOpenResult(int id, boolean success, int error_code);
    public abstract void onSocketError(int id, int error_code);
    public abstract void onSocketData(int id, Buffer data);
    public abstract void onSocketCloseResult(int id, boolean success, int error_code);
}
