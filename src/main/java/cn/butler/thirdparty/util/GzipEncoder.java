package cn.butler.thirdparty.util;

import org.apache.shiro.lang.codec.Base64;

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

public class GzipEncoder {
    public static String gzipBase64Bytes(byte[] data) throws Exception {
        if (data == null || data.length == 0) {
            return "";
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(bos);
        gzip.write(data);
        gzip.close();   // 一定要 close，否则 GZIP 结尾数据不会写入
        return Base64.encodeToString(bos.toByteArray());
    }
}
