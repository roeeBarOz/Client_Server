package bgu.spl.net.impl.tftp;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import bgu.spl.net.api.MessagingProtocol;

public class ClientTftpProtocol implements MessagingProtocol<byte[]> {

    private final String DIR_PATH = "/";
    private boolean shouldTerminate;
    private final byte[] zero = { (byte) 0 };
    private int blockNumber;
    private File fileToRead;
    private File fileToWrite;
    private int currBlockRead = 1;
    private int currBlockWrite = 1;
    private FileOutputStream fw;
    private FileInputStream fis;
    private DataInputStream dis;
    private boolean RRQorDIRQ; // true = RRQ, false = DIRQ
    private String currentOperation;

    @Override
    public byte[] process/* From */(byte[] msg) {
        System.out.println("Received: " + Arrays.toString(msg));
        int opcode = convertToShort(0, 1, msg);
        byte[] data;
        int block;
        switch (opcode) {
            case 3:
                int size = convertToShort(2, 3, msg);
                block = convertToShort(4, 5, msg);
                data = Arrays.copyOfRange(msg, 6, msg.length);
                return handleDATAFrom(size, block, data);
            case 4:
                block = convertToShort(2, 3, msg);
                return handleACKFrom(block);
            case 5:
                int errorCode = convertToShort(2, 3, msg);
                data = Arrays.copyOfRange(msg, 4, msg.length - 1);
                return handleERRORFrom(errorCode, data);
            case 9:
                int delAdd = msg[2];
                data = Arrays.copyOfRange(msg, 3, msg.length - 1);
                return handleBCASTFrom(delAdd, data);
            default:
                return null;
        }
    }

    public byte[] processTo(String msg) {
        String[] command = msg.split("\\s+");

        if (command.length == 0) {
            System.out.println("Invalid command");
            return null;
        }
        int opcode;
        String data = "";
        currentOperation = command[0];
        switch (command[0]) {
            case "RRQ":
                opcode = 1;
                data = msg.substring(command[0].length() + 1);
                fileToWrite = new File(DIR_PATH, data);
                try {
                    fw = new FileOutputStream(fileToWrite);
                } catch (FileNotFoundException e) {
                }
                currBlockWrite = 1;
                break;
            case "WRQ":
                opcode = 2;
                data = msg.substring(command[0].length() + 1);
                fileToRead = new File(DIR_PATH, data);
                try {
                    fis = new FileInputStream(fileToRead);
                    dis = new DataInputStream(fis);
                } catch (FileNotFoundException e) {
                }
                currBlockRead = 1;
                break;
            case "DATA":
                opcode = 3;
                data = msg.substring(command[0].length() + 1);
                break;
            case "ACK":
                opcode = 4;
                data = msg.substring(command[0].length() + 1);
                break;
            case "DIRQ":
                opcode = 6;
                break;
            case "LOGRQ":
                opcode = 7;
                data = msg.substring(command[0].length() + 1);
                break;
            case "DELRQ":
                opcode = 8;
                data = msg.substring(command[0].length() + 1);
                break;
            case "DISC":
                opcode = 10;
                break;
            default:
                System.out.println("Invalid command");
                return null;
        }
        return handleCommandTo(opcode, data);
    }

    private byte[] handleCommandTo(int opcode, String data) {
        byte[] message = convertToBytes((short) opcode);
        byte[] byteData = {};
        try {
            byteData = data.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        switch (opcode) {
            case 1:
                message = mergeArr(message, byteData);
                message = mergeArr(message, zero);
                break;
            case 2:
                message = mergeArr(message, byteData);
                message = mergeArr(message, zero);
                break;
            case 3:
                message = mergeArr(message, convertToBytes((short) byteData.length));
                message = mergeArr(message, convertToBytes((short) blockNumber));
                message = mergeArr(message, byteData);
                break;
            case 4:
                message = mergeArr(message, convertToBytes((short) blockNumber));
                break;
            case 6:
                break;
            case 7:
                message = mergeArr(message, byteData);
                message = mergeArr(message, zero);
                break;
            case 8:
                message = mergeArr(message, byteData);
                message = mergeArr(message, zero);
                break;
            case 10:
                break;
            default:
                break;
        }
        return message;
    }

    private byte[] handleACKFrom(int block) {
        System.out.println("Handling ACK");
        if (block == 0)
            return new byte[] { 0x0, 0x4, 0x0, 0x0 };
        if (currBlockRead == block) {
            if (fileToRead != null) {
                return createDataPacket();
            } else {
                System.out.println("problem with file");
            }
            currBlockRead++;
        } else {
            System.out.println("Wrong block number received for data");
        }
        return null;
    }

    private byte[] handleDATAFrom(int size, int block, byte[] data) {
        System.out.println("Handling DATA");
        if (RRQorDIRQ) { // means data id from RRQ
            if (block == currBlockWrite) {
                try {
                    fw.write(data);
                } catch (IOException e) {
                    System.out.println("i dont know what happened");
                    return null;
                }
            } else {
                System.out.println("Wrong block number received for data");
                return null;
            }
        } else { // means data is from DIRQ
            for (byte b : data) {
                List<Byte> file = new LinkedList();
                if (b != 0x0) {
                    file.add(b);
                } else {
                    byte[] fileToPrint = new byte[file.size()];
                    for (int i = 0; i < fileToPrint.length; i++) {
                        fileToPrint[i] = file.get(i);
                    }
                    System.out.println(bytesToString(fileToPrint));
                }
            }
        }
        byte[] opcode = { 0x0, 0x4 };
        return mergeArr(opcode, convertToBytes((short) currBlockWrite++));
    }

    private byte[] handleBCASTFrom(int delAdd, byte[] data) {
        System.out.println("Handling BCAST");
        String print = "BCAST ";
        print += delAdd == 1 ? "add" : "del";
        print += " " + bytesToString(data);
        System.out.println(print);
        return null;
    }

    private byte[] handleERRORFrom(int errorCode, byte[] data) {
        System.out.println("Handling ERROR");
        String print = "Error " + errorCode + "\n" + bytesToString(data);
        System.out.println(print);
        return null;
    }

    private byte[] createDataPacket() {
        byte[] data;
        try {
            data = new byte[Math.max(512, dis.available())];
            int read = dis.read(data);
            byte[] opcode = { 0x0, 0x3 };
            data = mergeArr(opcode, data);
            return data;
        } catch (IOException e) {
        }
        return null;
    }

    @Override
    public boolean shouldTerminate() {
        // TODO Auto-generated method stub
        return shouldTerminate;
    }

    private byte[] mergeArr(byte[] b1, byte[] b2) {
        byte[] res = new byte[b1.length + b2.length];
        for (int i = 0; i < b1.length; i++)
            res[i] = b1[i];
        for (int i = 0; i < b2.length; i++)
            res[i + b1.length] = b2[i];
        return res;
    }

    private byte[] convertToBytes(short num) {
        return new byte[] { (byte) (num >> 8), (byte) (num & 0xff) };
    }

    private int convertToShort(int firstByte, int secondByte, byte[] bytes) {
        return (short) ((short) ((bytes[firstByte] & 0xFF) << 8) | (short) (bytes[secondByte] & 0xFF));
    }

    private String bytesToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
