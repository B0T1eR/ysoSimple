package cn.butler.yso.payloads.util;


import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;

import java.io.*;

public class CommonUtil {
    public static byte[] getFileBytes(String file) {
        try {
            File f = new File(file);
            int length = (int) f.length();
            byte[] data = new byte[length];
            new FileInputStream(f).read(data);
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Class getClass(String className){
        Class clazz = null;
        try{
            clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
        }catch (Exception e){
            ClassPool pool = new ClassPool(true);
            CtClass targetClass = pool.makeClass(className);
            try {
                targetClass.getClassFile().setVersionToJava5();
                clazz = targetClass.toClass();
            } catch (CannotCompileException cannotCompileException) {
                cannotCompileException.printStackTrace();
            }
        }
        return clazz;
    }

    public static byte[] readFileByte(String filename) throws IOException {

        File f = new File(filename);
        if (!f.exists()) {
            throw new FileNotFoundException(filename);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream((int) f.length());
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(f));
            int buf_size = 1024;
            byte[] buffer = new byte[buf_size];
            int len = 0;
            while (-1 != (len = in.read(buffer, 0, buf_size))) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            bos.close();
        }
    }

    public static String fileContextToByteArrayString(String filePath) throws IOException {
        byte[] fileContent = CommonUtil.readFileByte(filePath);
        return byteToByteArrayString(fileContent);
    }

    public static String stringToByteArrayString(String str){
        byte[] byteString = str.getBytes();
        return byteToByteArrayString(byteString);
    }

    public static String byteToByteArrayString(byte[] strByte){
        StringBuffer sb = new StringBuffer();
        if(strByte.length > 0) {
            for (byte bStr : strByte) {
                sb.append(bStr);
                sb.append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }
}
