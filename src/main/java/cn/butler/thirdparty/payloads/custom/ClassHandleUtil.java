package cn.butler.thirdparty.payloads.custom;

import javassist.*;
import java.io.IOException;

public class ClassHandleUtil {

    /**
     * 为字节码设置类名
     * @param classByteName
     * @param classByteCode
     * @param newClassName
     * @return
     */
    public static Object setClassNameForClass(String classByteName, byte[] classByteCode, String newClassName) throws Exception {
        ClassPool classPool = ClassPool.getDefault();
        classPool.appendSystemPath(); // 确保能找到基础类

        //检查ClassPool是否已经加载同名类。如果已加载,先将其解冻,再进行修改。
        CtClass ctClass;
        if (classPool.getOrNull(classByteName) != null) {
            ctClass = classPool.get(classByteName);
            if (ctClass.isFrozen()) {
                ctClass.defrost(); // 解冻已冻结的类
            }
        } else {
            ctClass = classPool.makeClass(new java.io.ByteArrayInputStream(classByteCode));
        }

        ctClass.setName(newClassName); // 设置类名
        // 冻结类，防止进一步修改
        ctClass.freeze();
        // 将修改后的类转换为字节码
        byte[] modifiedByteCode = ctClass.toBytecode();
        return new Object[]{newClassName,modifiedByteCode};
    }

    /**
     * 为字节码类添加父类
     * @param className 字节码类名
     * @param byteCode 字节码数组
     * @param superClassName 继承的父类
     * @return
     * @throws IOException
     * @throws NotFoundException
     * @throws CannotCompileException
     */
    public static Object setSuperClassForClass(String className,byte[] byteCode,String superClassName) throws IOException, NotFoundException, CannotCompileException {
        ClassPool classPool = ClassPool.getDefault();
        classPool.appendSystemPath(); // 确保能找到基础类
        classPool.importPackage(superClassName);

        //检查ClassPool是否已经加载同名类。如果已加载,先将其解冻,再进行修改。
        CtClass ctClass;
        if (classPool.getOrNull(className) != null) {
            ctClass = classPool.get(className);
            if (ctClass.isFrozen()) {
                ctClass.defrost(); // 解冻已冻结的类
            }
        } else {
            ctClass = classPool.makeClass(new java.io.ByteArrayInputStream(byteCode));
        }

        ctClass.setName(className); // 设置类名
        ctClass.setSuperclass(classPool.get(superClassName));
        // 冻结类，防止进一步修改
        ctClass.freeze();
        // 将修改后的类转换为字节码
        byte[] modifiedByteCode = ctClass.toBytecode();
        return new Object[]{className,modifiedByteCode};
    }

    /**
     * 为字节码类添加实现的接口
     * @param className 字节码类名
     * @param byteCode 字节码数组
     * @param interfaceClassName 实现的接口
     * @return
     * @throws IOException
     * @throws NotFoundException
     * @throws CannotCompileException
     */
    public static Object addInterfaceForClass(String className,byte[] byteCode,String interfaceClassName) throws IOException, NotFoundException, CannotCompileException {
        ClassPool classPool = ClassPool.getDefault();
        classPool.appendSystemPath(); // 确保能找到基础类
        classPool.importPackage(interfaceClassName);

        //检查ClassPool是否已经加载同名类。如果已加载,先将其解冻,再进行修改。
        CtClass ctClass;
        if (classPool.getOrNull(className) != null) {
            ctClass = classPool.get(className);
            if (ctClass.isFrozen()) {
                ctClass.defrost(); // 解冻已冻结的类
            }
        } else {
            ctClass = classPool.makeClass(new java.io.ByteArrayInputStream(byteCode));
        }

        ctClass.setName(className); // 设置接口名
        ctClass.addInterface(classPool.get(interfaceClassName));
        // 冻结类，防止进一步修改
        ctClass.freeze();
        // 将修改后的类转换为字节码
        byte[] modifiedByteCode = ctClass.toBytecode();
        return new Object[]{className,modifiedByteCode};
    }

    /**
     * 根据字节码获取类名
     * @param byteCode
     * @return
     * @throws IOException
     */
    public static String getClassNameByByteCode(byte[] byteCode) throws IOException {
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(byteCode));
        return ctClass.getName();
    }

    public static Object modifyJavaWrapperClass(String classByteName, byte[] classByteCode) {
        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(classByteCode));
            // 检查类名是否匹配
            if (!ctClass.getName().equals(classByteName)) {
                throw new IllegalArgumentException("Class name does not match the provided bytecode.");
            }
            // 获取 static 块
//            CtConstructor staticBlock = ctClass.getClassInitializer();
            // 创建新的 _main 方法
            String newMethodCode =
                "public static void _main(String[] args) { new " + classByteName + "();}";
            CtMethod mainMethod = CtNewMethod.make(newMethodCode, ctClass);
            ctClass.addMethod(mainMethod);
            // 将修改后的字节码写回到字节数组中
            byte[] modifiedClass = ctClass.toBytecode();
            ctClass.detach(); // 释放 CtClass 对象
            return new Object[]{classByteName, modifiedClass};
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object modifyCustomStaticMethodClass(String classByteName, byte[] classByteCode,String staticMethodName) {
        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(classByteCode));
            // 检查类名是否匹配
            if (!ctClass.getName().equals(classByteName)) {
                throw new IllegalArgumentException("Class name does not match the provided bytecode.");
            }
            // 创建新的名为 staticMethodName 的静态方法
            String newMethodCode =
                "public static void "+ staticMethodName +"() { new " + classByteName + "();}";
            CtMethod mainMethod = CtNewMethod.make(newMethodCode, ctClass);
            ctClass.addMethod(mainMethod);
            // 将修改后的字节码写回到字节数组中
            byte[] modifiedClass = ctClass.toBytecode();
            ctClass.detach(); // 释放 CtClass 对象
            return new Object[]{classByteName, modifiedClass};
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
