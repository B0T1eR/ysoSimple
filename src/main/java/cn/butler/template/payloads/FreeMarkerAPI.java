package cn.butler.template.payloads;

import cn.butler.payloads.ObjectPayload;
import cn.butler.payloads.PayloadRunner;

public class FreeMarkerAPI implements ObjectPayload<Object> {
    @Override
    public Object getObject(String command) throws Exception {

        return null;
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(FreeMarkerAPI.class, args);
    }
}
