package cn.butler;

import cn.butler.hessian.config.HessianConfig;
import cn.butler.payloads.config.Config;
import cn.butler.yso.config.YsoConfig;
import cn.butler.jndi.JndiConfig;
import cn.butler.jdbcattack.config.JdbcAttackConfig;
import cn.butler.snakeyaml.config.SnakeYamlConfig;
import cn.butler.template.config.SSTIAttackConfig;
import cn.butler.thirdparty.config.ThirdPartyConfig;
import cn.butler.xstream.config.XStreamConfig;
import org.apache.commons.cli.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.LogManager;

@SuppressWarnings("rawtypes")
public class GeneratePayload {
    public static YsoConfig ysoConfig = new YsoConfig();
    public static JndiConfig jndiConfig = new JndiConfig();
    public static HessianConfig hessianConfig = new HessianConfig();
    public static XStreamConfig xStreamConfig = new XStreamConfig();
    public static SnakeYamlConfig snakeYamlConfig = new SnakeYamlConfig();
    public static JdbcAttackConfig jdbcAttackConfig = new JdbcAttackConfig();
    public static SSTIAttackConfig sstiAttackConfig = new SSTIAttackConfig();
    public static ThirdPartyConfig thirdPartyConfig = new ThirdPartyConfig();

	public static void main(final String[] args) throws ParseException {
        //因为FakeMySQLPcapFile生成数据流时会携带输出日志,所以在这里关闭JUL日志就好
        LogManager.getLogManager().reset();
        Map<String, Options> allOptions = new HashMap<>();

        //设定配置参数
        Options helpOptions = new Options();
        helpOptions.addOption("m", "mode", true, "YsoAttack HessianAttack JdbcAttack XStreamAttack SnakeYamlAttack SSTIAttack JNDIAttack ThirdPartyAttack");
        helpOptions.addOption("h", "help", true, "Get help for specific modules (e.g., -help YsoAttack)");
        allOptions.put("help",helpOptions);

        //YsoAttack
        Options ysoOptions = new Options();
        ysoOptions.addOption("m", "mode", true, "YsoAttack");
        ysoOptions.addOption("g", "gadget", true, "Java deserialization gadget");
        ysoOptions.addOption("a", "args", true, "Gadget parameters");
        ysoOptions.addOption("ddl", "dirt-data-length", true, "Add the length of dirty data To Bypass WAF");
        ysoOptions.addOption("gz","gzip",false,"Gzip Java Serialize Payload");
        ysoOptions.addOption("um", "utf8-bytes-mix", true, "UTF-8 Overlong Encoding To Bypass WAF");
        ysoOptions.addOption("c", "compress", false, "Zip the TemplatesImpl Gadget");
        ysoOptions.addOption("sk", "shiro-key", true, "Shiro Encrypt AES Key");
        ysoOptions.addOption("sp", "shiro-encrypt-pattern", true, "Shiro Encrypt AES CBC/GCM Pattern");
        ysoOptions.addOption("swp", "shiro-base64WafBypass", true, "Shiro Base64 Obfuscation To Bypass WAF");
        ysoOptions.addOption("jsf","jsf-key",true,"JSF Faces AES Key");
        ysoOptions.addOption("mp","mysql-pcap",true,"Mysql Evil Pcap Generate");
        ysoOptions.addOption("en", "encode", true, "Base64,Hex encode");
        ysoOptions.addOption("wtf","writeToFile",true,"Save Serialized Yso Payload to File");
        allOptions.put("YsoAttack", ysoOptions);

        //HessianAttack
        Options hessianOptions = new Options();
        hessianOptions.addOption("m", "mode", true, "HessianAttack");
        hessianOptions.addOption("g", "gadget", true, "Hessian deserialization gadget");
        hessianOptions.addOption("a", "args", true, "Gadget parameters");
        hessianOptions.addOption("ddl", "dirt-data-length", true, "Add the length of dirty data To Bypass WAF");
        hessianOptions.addOption("ht","hessianType",true,"Hessian1 or Hessian2 Gadget Generate");
        hessianOptions.addOption("he","hessianExcept",false,"Hessian Except Gadget Generate");
        hessianOptions.addOption("um", "utf8-bytes-mix", false, "UTF-8 Overlong Encoding To Bypass WAF");
        hessianOptions.addOption("en", "encode", true, "Base64,Hex encode");
        hessianOptions.addOption("wtf","writeToFile",true,"Save Serialized Hessian Payload to File");
        allOptions.put("HessianAttack", hessianOptions);

        //XStreamAttack
        Options xStreamOptions = new Options();
        xStreamOptions.addOption("m", "mode", true, "XStreamAttack");
        xStreamOptions.addOption("g", "gadget", true, "Xstream deserialization gadget");
        xStreamOptions.addOption("a", "args", true, "Gadget parameters");
        xStreamOptions.addOption("wtf","writeToFile",true,"Save XStream Payload to File");
        allOptions.put("XStreamAttack", xStreamOptions);

        //SnakeYamlAttack
        Options snakeYamlOptions = new Options();
        snakeYamlOptions.addOption("m", "mode", true, "SnakeYamlAttack");
        snakeYamlOptions.addOption("g", "gadget", true, "xstream deserialization gadget");
        snakeYamlOptions.addOption("a", "args", true, "gadget parameters");
        snakeYamlOptions.addOption("wb","waf-bypass",true,"SnakeYaml Bypass WAF");
        snakeYamlOptions.addOption("wtf","writeToFile",true,"Save SnakeYaml Payload to File");
        allOptions.put("SnakeYamlAttack", snakeYamlOptions);

        //JDBCAttack
        Options jdbcOptions = new Options();
        jdbcOptions.addOption("m", "mode", true, "JDBCAttack");
        jdbcOptions.addOption("g", "gadget", true, "Jdbcattack gadget");
        jdbcOptions.addOption("a", "args", true, "Gadget parameters");
        jdbcOptions.addOption("wtf","writeToFile",true,"Save JDBCAttack Payload to File");
        allOptions.put("JdbcAttack", jdbcOptions);

        //SSTIAttack
        Options sstiOptions = new Options();
        sstiOptions.addOption("m", "mode", true, "SSTIAttack");
        sstiOptions.addOption("g", "gadget", true, "SSTI gadget");
        sstiOptions.addOption("a", "args", true, "Gadget parameters");
        sstiOptions.addOption("wtf","writeToFile",true,"Save SSTI Payload to File");
        allOptions.put("SSTIAttack", sstiOptions);

        //JNDIAttack
        Options jndiOptions = new Options();
        jndiOptions.addOption("m", "mode", true, "JNDIAttack");
        jndiOptions.addOption("i", "ip", true, "RMI/LDAP/WEBServer IP");
        jndiOptions.addOption("r", "rmi-port", true, "RMI Port");
        jndiOptions.addOption("l", "ldap-port", true, "LDAP Port");
        jndiOptions.addOption("w", "web-port", true, "HTTP Port");
        jndiOptions.addOption("u", "jndi-url", true, "JNDI URL");
        jndiOptions.addOption("ju","jndi-useReferenceOnly",false,"JNDI useReferenceOnly");
        allOptions.put("JNDIAttack", jndiOptions);

        //ThirdPartyAttack Deserialize
        Options thirdPartyOptions = new Options();
        thirdPartyOptions.addOption("m", "mode", true, "ThirdPartyAttack");
        thirdPartyOptions.addOption("g", "gadget", true, "Thirdparty gadget");
        thirdPartyOptions.addOption("a", "args", true, "Gadget parameters");
        thirdPartyOptions.addOption("c", "className", true, "ClassName");
        thirdPartyOptions.addOption("s", "superClassName", true, "Super ClassName");
        thirdPartyOptions.addOption("i", "interfaceName", true, "Interface ClassName");
        thirdPartyOptions.addOption("en", "encode", true, "Base64,Hex encode");
        thirdPartyOptions.addOption("jp", "jarPayload", true, "JarPayload Generate");
        thirdPartyOptions.addOption("wtf","writeToFile",true,"Save Class Bytes to File");
        thirdPartyOptions.addOption("cm","classModify",true,"Class Modify");
        thirdPartyOptions.addOption("fm","fileModify",true,"File Modify");
        allOptions.put("ThirdPartyAttack", thirdPartyOptions);
        //解析配置参数
        Config.parse(allOptions,args);
    }
}
