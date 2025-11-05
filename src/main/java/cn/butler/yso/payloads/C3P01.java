package cn.butler.yso.payloads;

import cn.butler.payloads.ObjectPayload;
import cn.butler.payloads.PayloadRunner;
import cn.butler.payloads.annotation.Dependencies;

/**
 * C3P01 的jndi注入攻击 这个还没写过，先挖坑
 * com.mchange.v2.c3p0.impl.PoolBackedDataSourceBase#readObject()
 *   -IndirectlySerialized#getObject()
 *   -com.mchange.v2.naming.ReferenceIndirector#getObject()
 *     -InitialContext#lookup()
 */
@Dependencies( { "com.mchange:c3p0:0.9.5.2" ,"com.mchange:mchange-commons-java:0.2.11"} )
public class C3P01 implements ObjectPayload<Object> {
    @Override
    public Object getObject(String command) throws Exception {
        return null;
    }

    public static void main ( String[] args ) throws Exception {
        PayloadRunner.run(C3P01.class, args);
    }
}
