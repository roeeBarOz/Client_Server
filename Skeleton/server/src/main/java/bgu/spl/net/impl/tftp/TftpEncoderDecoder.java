package bgu.spl.net.impl.tftp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Text;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    // TODO: Implement here the TFTP encoder and decoder

    private byte[] bytes = new byte[1 << 10]; // start with 1k
    private int len = 0;
    private byte[] decodeBytes = new byte[1 << 10];
    private int opcode = 0;
    private int dataLength;

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        if (len == 0)
            decodeBytes = new byte[1 << 10];
        bytes[len++] = nextByte;
        if (len == 2)
            opcode = convertToShort(0, 1);
        if (opcode != 0) {
            switch (opcode) {
                case 1:
                    if (nextByte == (byte) 0) {
                        decodeBytes = Arrays.copyOf(bytes, len - 1);
                        bytes = new byte[1 << 10];
                        len = 0;
                        opcode = 0;
                        return decodeBytes;
                    }
                    break;
                case 2:
                    if (nextByte == (byte) 0) {
                        decodeBytes = Arrays.copyOf(bytes, len - 1);
                        bytes = new byte[1 << 10];
                        len = 0;
                        opcode = 0;
                        return decodeBytes;
                    }
                    break;
                case 3:
                    if(len == 2)
                        dataLength = 0;
                    if (len >= 4) {
                        dataLength = convertToShort(2, 3);
                    }
                    if (dataLength != 0) {
                        if (len == dataLength + 6) {
                            decodeBytes = Arrays.copyOf(bytes, len);
                            bytes = new byte[1 << 10];
                            len = 0;
                            opcode = 0;
                            return decodeBytes;
                        }
                    }
                    break;
                case 4:
                    if (len == 4) {
                        decodeBytes = Arrays.copyOf(bytes, len);
                        bytes = new byte[1 << 10];
                        len = 0;
                        opcode = 0;
                        return decodeBytes;
                    }
                    break;
                case 5:
                    if (nextByte == (byte) 0) {
                        decodeBytes = Arrays.copyOf(bytes, len - 1);
                        bytes = new byte[1 << 10];
                        len = 0;
                        opcode = 0;
                        return decodeBytes;
                    }
                    break;
                case 6:
                    if (len == 2) {
                        decodeBytes = Arrays.copyOf(bytes, len);
                        bytes = new byte[1 << 10];
                        len = 0;
                        opcode = 0;
                        return decodeBytes;
                    }
                    break;
                case 7:
                    if (nextByte == (byte) 0) {
                        decodeBytes = Arrays.copyOf(bytes, len - 1);
                        bytes = new byte[1 << 10];
                        len = 0;
                        opcode = 0;
                        return decodeBytes;
                    }
                    break;
                case 8:
                    if (nextByte == (byte) 0) {
                        decodeBytes = Arrays.copyOf(bytes, len - 1);
                        bytes = new byte[1 << 10];
                        len = 0;
                        opcode = 0;
                        return decodeBytes;
                    }
                    break;
                case 9:
                    if (nextByte == (byte) 0) {
                        decodeBytes = Arrays.copyOf(bytes, len - 1);
                        bytes = new byte[1 << 10];
                        len = 0;
                        opcode = 0;
                        return decodeBytes;
                    }
                    break;
                case 10:
                    if (len == 2) {
                        decodeBytes = Arrays.copyOf(bytes, len);
                        bytes = new byte[1 << 10];
                        len = 0;
                        opcode = 0;
                        return decodeBytes;
                    }
                    break;
                default:
                    break;
            }
        }
        return null; // not a line yet
    }

    @Override
    public byte[] encode(byte[] message) {
        return message;
    }

    private int convertToShort(int firstByte, int secondByte) {
        return (short) ((short)((bytes[firstByte] & 0xFF) << 8) | (short)(bytes[secondByte]& 0xFF));
    }
}