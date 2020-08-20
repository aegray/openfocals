package com.openfocals.focals;

import java.io.IOException;

import kotlin.jvm.internal.ByteCompanionObject;
import okio.Buffer;

public class VarInt {
    public static final long MAX_UVARINT32_VALUE = 4294967295L;

    private VarInt() {
    }

    public static long readUVarInt32(Buffer buffer) throws IOException {
        byte b;
        long j = 0;
        long j2 = 0;
        do {
            b = buffer.getByte(j);
            j2 |= ((long) (b & ByteCompanionObject.MAX_VALUE)) << ((int) (7 * j));
            j++;
            if (b >= 0) {
                break;
            }
        } while (j < 5);
        if (b >= 0) {
            buffer.skip(j);
            return j2;
        }
        throw new IOException("Malformed VarInt.");
    }
}
