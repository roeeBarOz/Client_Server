package bgu.spl.net.impl.tftp;

import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.filechooser.FileNameExtensionFilter;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;

class holder {
    static ConcurrentHashMap<Integer, byte[]> loggedIn = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, Integer> files = new ConcurrentHashMap<>();
}

public class TftpProtocol implements BidiMessagingProtocol<byte[]> {

    private final String DIR_PATH = "Flies/";
    private boolean shouldTerminate = false;
    private Connections<byte[]> activeConnections;
    private int ownerId;
    private final File filepath = new File(DIR_PATH);
    private FileOutputStream fw;
    private File fileToRead;
    private File fileToWrite;
    private int currBlockRead = 1;
    private int currBlockWrite = 1;
    private final byte[] zero = { (byte) 0 };
    private byte[][] sendDirq;
    private FileInputStream fis;
    private DataInputStream dis;

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        // TODO implement this
        activeConnections = connections;
        ownerId = connectionId;
        // activeConnections.connect(connectionId, new
        // TftpConnectionHandler(new Socket(),new
        // TftpEncoderDecoder(),));
    }

    @Override
    public void process(byte[] message) {
        // TODO implement this
        int opcode = convertToShort(0, 1, message);
        byte[] info;
        int blockNumber;
        int errorCode;
        int delAdd;
        // System.out.println(opcode);
        for (int i = 0; i < message.length; i++)
            System.out.println(message[i]);
        System.out.println("done " + opcode);
        switch (opcode) {
            case 1:
                info = relevantBytes(2, message);
                handleRRQ(info, ownerId);
                break;
            case 2:
                info = relevantBytes(2, message);
                handleWRQ(info, ownerId);
                break;
            case 3:
                info = relevantBytes(6, message);
                blockNumber = convertToShort(4, 5, message);
                handleDATA(ownerId, info, blockNumber);
                break;
            case 4:
                blockNumber = convertToShort(2, 3, message);
                handleACK(blockNumber, ownerId);
                break;
            case 5:
                // server will never get an ERROR packet
                // errorCode = convert(2,3,message);
                // info = relevantByteTostring(4, message);
                break;
            case 6:
                handleDIRQ(ownerId);
                break;
            case 7:
                info = relevantBytes(2, message);
                handleLOGRQ(info, ownerId);
                break;
            case 8:
                info = relevantBytes(2, message);
                handleDELRQ(info, ownerId);
                break;
            case 9:
                // server will never get a BCAST packet
                info = relevantBytes(2, message);
                delAdd = message[2];
                break;
            case 10:
                shouldTerminate = true;
                handleDISC(ownerId);
                break;
            default:
                createAndSendErrorPacket(ownerId, 4);
                break;
        }
    }

    private void handleRRQ(byte[] filename, int ownerId) {
        if (holder.loggedIn.containsKey(ownerId)) {
            String file = bytesToString(filename);
            fileToRead = new File(DIR_PATH + file);
            if (!fileToRead.exists()) {
                createAndSendErrorPacket(ownerId, 1);
            } else {
                if (!holder.files.containsKey(bytesToString(filename))) {
                    holder.files.put(bytesToString(filename), 0);
                } else {
                    int workOn = holder.files.remove(bytesToString(filename));
                    holder.files.put(bytesToString(filename), workOn++);
                }
                try {
                    fis = new FileInputStream(fileToRead);
                    dis = new DataInputStream(fis);
                    byte[] b = new byte[Math.min(512, (int) fileToRead.length())];
                    dis.read(b);
                    currBlockRead = 1;
                    createAndSendDataPacket(b, ownerId, 1);
                } catch (Exception e) {
                }
            }
        } else {
            createAndSendErrorPacket(ownerId, 6);
        }
    }

    private void handleWRQ(byte[] filename, int ownerId) {
        if (holder.loggedIn.containsKey(ownerId)) {
            String file = bytesToString(filename);
            fileToWrite = new File(DIR_PATH + file);
            try {
                if (!fileToWrite.createNewFile()) {
                    createAndSendErrorPacket(ownerId, 5);
                }
                // else: we just finished
                else {
                    currBlockWrite = 1;
                    holder.files.put(fileToWrite.getName(), 1);
                    fw = new FileOutputStream(fileToWrite);
                    createAndSendAckPacket(ownerId, 0);
                }
            } catch (IOException e) {
                createAndSendErrorPacket(ownerId, 0);
            }

        } else {
            createAndSendErrorPacket(ownerId, 6);
        }
    }

    private void handleDATA(int ownerId, byte[] data, int blockNumber) {
        if (holder.loggedIn.containsKey(ownerId)) {
            if (filepath.getUsableSpace() >= data.length) {
                if (currBlockWrite == blockNumber) {
                    try {
                        fw.write(data);
                        System.out.println(data.length);
                        if (data.length < 512) {
                            int workOn = holder.files.remove(fileToWrite.getName());
                            holder.files.put(fileToWrite.getName(), workOn--);
                            createAndSendAckPacket(ownerId, currBlockWrite);
                            createAndSendBCastPacket(ownerId, 1, fileToWrite.getName().getBytes("UTF-8"));
                            fileToWrite = null;
                        } else
                            createAndSendAckPacket(ownerId, currBlockWrite);
                        currBlockWrite++;
                    } catch (IOException e) {
                        createAndSendErrorPacket(ownerId, 2);
                    }
                }
            } else {
                holder.files.remove(fileToWrite.getName());
                fileToWrite.delete();
                createAndSendErrorPacket(ownerId, 3);
            }
        } else {
            createAndSendErrorPacket(ownerId, 6);
        }
    }

    private void handleACK(int blockNumber, int ownerId) {
        if (holder.loggedIn.containsKey(ownerId)) {
            if (blockNumber == 0)
                return;
            if (currBlockRead == blockNumber) {
                currBlockRead++;
                if (fileToRead != null) {
                    try {
                        // Create a byte array to store the read data
                        byte[] buffer = new byte[Math.min(512, dis.available())];

                        // Read 512 bytes from the current file position into the buffer
                        int bytesRead = dis.read(buffer);
                        // Check if there is any data read
                        if (bytesRead > 0) {
                            // Process the read data (in this example, just print it as a string)
                            byte[] data = buffer;
                            createAndSendDataPacket(data, ownerId, ++blockNumber);
                        }
                        if (bytesRead < 512) {
                            System.out.println(fileToRead.getName());
                            int workOn = holder.files.remove(fileToRead.getName());
                            holder.files.put(fileToRead.getName(), --workOn);
                        }
                    } catch (IOException e) {
                        createAndSendErrorPacket(ownerId, 0);
                    }
                } else {
                    if (this.sendDirq.length <= blockNumber)
                        return;
                    else if (this.sendDirq[blockNumber][511] != (byte) 0)
                        createAndSendDataPacket(this.sendDirq[blockNumber], ownerId, currBlockRead);
                    else {
                        int len = 0;
                        while (this.sendDirq[blockNumber][len] != (byte) 0) {
                            len++;
                        }
                        byte[] data = new byte[len];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = this.sendDirq[blockNumber][i];
                        }
                        createAndSendDataPacket(data, ownerId, blockNumber);
                    }
                }
            }
        } else {
            createAndSendErrorPacket(ownerId, 6);
        }
    }

    private void handleDIRQ(int ownerId) {
        if (holder.loggedIn.containsKey(ownerId)) {
            File dir = new File(DIR_PATH);
            File[] files = dir.listFiles();
            byte[] data = new byte[0];
            if (files != null && files.length != 0) {
                for (File f : files) {
                    try {
                        byte[] temp;
                        temp = f.getName().getBytes("UTF-8");
                        temp = mergeArr(temp, zero);
                        data = mergeArr(data, temp);
                    } catch (UnsupportedEncodingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                data = Arrays.copyOf(data, data.length - 1);
                sendDirq = new byte[data.length / 512 + 1][512];
                for (int i = 0; i < sendDirq.length; i++) {
                    for (int j = 0; j < sendDirq[i].length && i * 512 + j < data.length; j++) {
                        sendDirq[i][j] = data[i * 512 + j];
                    }
                }
                createAndSendDataPacket(data, ownerId, 1);
            } else
                createAndSendErrorPacket(ownerId, 0);
        } else {
            createAndSendErrorPacket(ownerId, 6);
        }
    }

    private void handleLOGRQ(byte[] username, int ownerId) {
        if (!holder.loggedIn.containsKey(ownerId) && !doesContain(username)) {
            holder.loggedIn.put(ownerId, username);
            createAndSendAckPacket(ownerId, 0);
        } else {
            createAndSendErrorPacket(ownerId, 7);
        }
    }

    private void handleDELRQ(byte[] filename, int ownerId) {
        if (holder.loggedIn.containsKey(ownerId)) {
            File f = new File(DIR_PATH + bytesToString(filename));
            if (f.delete()) {
                if (holder.files.containsKey(bytesToString(filename)))
                    holder.files.remove(bytesToString(filename));
                createAndSendAckPacket(ownerId, 0);
                createAndSendBCastPacket(ownerId, 0, filename);
            } else
                createAndSendErrorPacket(ownerId, 1);
        } else {
            createAndSendErrorPacket(ownerId, 6);
        }
    }

    private void handleDISC(int ownerId) {
        if (holder.loggedIn.containsKey(ownerId)) {
            holder.loggedIn.remove(ownerId);
            shouldTerminate = true;
            createAndSendAckPacket(ownerId, 0);
        } else {
            createAndSendErrorPacket(ownerId, 6);
        }
    }

    private void createAndSendDataPacket(byte[] data, int ownerId, int blockNumber) {
        byte[] op = { (byte) 0, (byte) 3 };
        byte[] size = convertToBytes((short) data.length);
        byte[] blocks = convertToBytes(((short) blockNumber));
        byte[] tempMsg = { op[0], op[1], size[0], size[1], blocks[0], blocks[1] };
        byte[] message = mergeArr(tempMsg, data);
        activeConnections.send(ownerId, message);
    }

    private void createAndSendErrorPacket(int ownerId, int errorCode) {
        String errMessage = "";
        byte[] op = { (byte) 0, (byte) 5 };
        byte[] errCode = convertToBytes((short) errorCode);
        switch (errorCode) {
            case 0:
                errMessage = "";
                break;
            case 1:
                // server will never get an ERROR packet
                // errorCode = convert(2,3,message);
                // info = relevantByteTostring(4, message);
                errMessage = "File not found - RRQ DELRQ of non-existing file.";
                break;
            case 2:
                errMessage = "Access violation - File cannot be written, read or deleted.";
                break;
            case 3:
                errMessage = "Disk full or allocation exceeded - No room in disk.";
                break;
            case 4:
                errMessage = "Illegal TFTP operation - Unknown Opcode.";
                break;
            case 5:
                errMessage = "File already exists - File name exists on WRQ.";
                break;
            case 6:
                errMessage = "User not logged in - Any opcode received before Login completes.";
                break;
            case 7:
                errMessage = "User already logged in - Login username already connected.";
                break;
            default:
                break;
        }
        byte[] message = mergeArr(op, errCode);
        try {
            message = mergeArr(message, errMessage.getBytes("UTF-8"));
            message = mergeArr(message, zero);
            activeConnections.send(ownerId, message);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void createAndSendAckPacket(int ownerId, int blockNumber) {
        byte[] op = { (byte) 0, (byte) 4 };
        byte[] blocks = convertToBytes(((short) blockNumber));
        byte[] message = { op[0], op[1], blocks[0], blocks[1] };
        activeConnections.send(ownerId, message);
    }

    private void createAndSendBCastPacket(int ownerId, int delAdd, byte[] filename) {
        Enumeration keys = holder.loggedIn.keys();
        byte[] op = { (byte) 0, (byte) 9, (byte) delAdd };
        byte[] message = mergeArr(op, filename);
        message = mergeArr(message, zero);
        while (keys.hasMoreElements()) {
            int sendTo = (int) keys.nextElement();
            if (sendTo != ownerId)
                activeConnections.send((int) sendTo, message);
        }
    }

    @Override
    public boolean shouldTerminate() {
        // TODO implement this
        return shouldTerminate;
    }

    private byte[] relevantBytes(int startingByte, byte[] message) {
        byte[] res = new byte[message.length - startingByte];
        int j = 0;
        for (int i = startingByte; i < message.length; i++) {
            res[j] = message[i];
            j++;
        }
        return res;
    }

    private int convertToShort(int firstByte, int secondByte, byte[] bytes) {
        return (short) ((short) ((bytes[firstByte] & 0xFF) << 8) | (short) (bytes[secondByte] & 0xFF));
    }

    private byte[] convertToBytes(short num) {
        return new byte[] { (byte) (num >> 8), (byte) (num & 0xff) };
    }

    private String bytesToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private byte[] mergeArr(byte[] b1, byte[] b2) {
        byte[] res = new byte[b1.length + b2.length];
        for (int i = 0; i < b1.length; i++)
            res[i] = b1[i];
        for (int i = 0; i < b2.length; i++)
            res[i + b1.length] = b2[i];
        return res;
    }

    private boolean doesContain(byte[] data){
        for(byte[] bytes : holder.loggedIn.values())
            if(Arrays.toString(bytes).equals(Arrays.toString(data)))
                return true;
        return false;
    }
}