package cn.butler.yso.utils.mysql.proto;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;

@Slf4j
public class FirstRespMessage {
    private byte[] data;

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getUsername() {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        if (data.length < 40) {
            log.error("data is too short");
            return null;
        }
        for (int i = 36; ; i++) {
            if (data[i] == 0) {
                break;
            }
            bao.write(data[i]);
        }
        return bao.toString();
    }
}
