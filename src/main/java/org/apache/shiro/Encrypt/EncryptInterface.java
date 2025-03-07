package org.apache.shiro.Encrypt;

import java.io.IOException;

public interface EncryptInterface {
    byte[] getBytes(Object obj) throws IOException;

    String encrypt(String key, byte[] objectBytes);

    String getName();
}
