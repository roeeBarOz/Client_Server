package bgu.spl.net.impl.tftp;

import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;

class holder {
    static ConcurrentHashMap<Integer, byte[]> loggedIn = new ConcurrentHashMap<>();
    static ConcurrentHashMap<byte[], Integer> files = new ConcurrentHashMap<>();
}

public class TftpProtocol implements BidiMessagingProtocol<byte[]> {
    private boolean shouldTerminate = false;
    private Connections<byte[]> activeConnections;
    private int ownerId;
    private final File filepath = new File("Skeleton/server/Flies");
    private File fileToRead;
    private File fileToWrite;
    private int currBlockRead = 1;
    private int currBlockWrite = 1;
    private final byte[] zero = { (byte) 0 };

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
        System.out.println(opcode);
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
            fileToRead = new File("Skeleton/server/Flies/" + file);
            if (!fileToRead.exists()) {
                createAndSendErrorPacket(ownerId, 1);
            } else {
                if (!holder.files.containsKey(filename)) {
                    holder.files.put(filename, 0);
                } else {
                    int workOn = holder.files.remove(filename);
                    holder.files.put(filename, workOn++);
                }
                try {
                    FileInputStream fin = new FileInputStream(fileToRead);
                    DataInputStream din = new DataInputStream(fin);
                    byte[] b = new byte[Math.min(512, (int) fileToRead.length())];
                    din.read(b);
                    din.close();
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
            fileToWrite = new File("Skeleton/server/Flies/" + file);
            try {
                if (!fileToWrite.createNewFile()) {
                    createAndSendErrorPacket(ownerId, 5);
                }
                // else: we just finished
                else {
                    holder.files.put(filename, 0);
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
                if (currBlockRead == blockNumber) {
                    try (FileWriter fw = new FileWriter(fileToWrite)) {
                        fw.write(bytesToString(data));
                        // fw.close();
                        if (data.length < 512) {
                            int workOn = holder.files.remove(fileToWrite.getName());
                            holder.files.put(fileToWrite.getName().getBytes("UTF-8"), workOn--);
                            fileToWrite = null;
                        }
                    } catch (IOException e) {
                        createAndSendErrorPacket(ownerId, 2);
                    }
                    currBlockRead++;
                    createAndSendAckPacket(ownerId, blockNumber);
                }
            } else {
                try {
                    holder.files.remove(fileToWrite.getName().getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
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
                try (FileInputStream fis = new FileInputStream("Skeleton/server/Flies/" + fileToRead.getName())) {
                    // FileInputStream fin = new FileInputStream(fileToRead);
                    // DataInputStream din = new DataInputStream(fin);
                    // byte[] b = new byte[Math.min(512, (int)fileToRead.length())];
                    // din.read(b);
                    // din.close();
                    // createAndSendDataPacket(b, ownerId, 1);
                    DataInputStream din = new DataInputStream(fis);
                    long skip = (blockNumber) * 512;
                    long skipped = fis.skip(skip);
                    if (skipped == skip) {
                        // Create a byte array to store the read data
                        if (fileToRead.length() - skip >= 0) {
                            byte[] buffer = new byte[Math.min(512, (int) (fileToRead.length() - skip))];

                            // Read 512 bytes from the current file position into the buffer
                            int bytesRead = din.read(buffer);
                            // Check if there is any data read
                            if (bytesRead > 0) {
                                // Process the read data (in this example, just print it as a string)
                                byte[] data = buffer;
                                /*
                                 * data = mergeArr(data, zero);
                                 * byte[] temp = convertToBytes((short) bytesRead);
                                 * data = mergeArr(data, temp);
                                 */
                                createAndSendDataPacket(data, ownerId, ++blockNumber);
                            }
                        }
                    } else {
                        int workOn = holder.files.remove(fileToRead.getName().getBytes("UTF-8"));
                        holder.files.put(fileToWrite.getName().getBytes("UTF-8"), ++workOn);
                    }
                } catch (IOException e) {
                    createAndSendErrorPacket(ownerId, 0);
                }
                currBlockWrite++;
            }
        } else {
            createAndSendErrorPacket(ownerId, 6);
        }
    }

    private void handleDIRQ(int ownerId) {
        if (holder.loggedIn.containsKey(ownerId)) {
            File dir = new File("Skeleton/server/Flies");
            File[] files = dir.listFiles();
            byte[] data = null;
            for (File f : files) {
                try {
                    byte[] temp;
                    temp = f.getName().getBytes("UTF-8");
                    data = Arrays.copyOf(temp, temp.length + 1);
                    data[data.length - 1] = (byte) 0;
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            data = Arrays.copyOf(data, data.length - 1);
            createAndSendDataPacket(data, ownerId, 1);
        } else {
            createAndSendErrorPacket(ownerId, 6);
        }
    }

    private void handleLOGRQ(byte[] username, int ownerId) {
        if (!holder.loggedIn.containsKey(ownerId)) {
            holder.loggedIn.put(ownerId, username);
            createAndSendAckPacket(ownerId, 0);
        } else {
            createAndSendErrorPacket(ownerId, 7);
        }
    }

    private void handleDELRQ(byte[] filename, int ownerId) {
        if (holder.loggedIn.containsKey(ownerId)) {
            File f = new File("Skeleton/server/Flies" + filename);
            if (f.delete()) {
                holder.files.remove(filename);
                createAndSendBCastPacket(1, filename);
            } else
                createAndSendErrorPacket(ownerId, 1);
        } else {
            createAndSendErrorPacket(ownerId, 6);
        }
    }

    private void handleDISC(int ownerId) {
        if (holder.loggedIn.containsKey(ownerId)) {
            holder.loggedIn.remove(ownerId);
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
        message = mergeArr(message, zero);
        try {
            message = mergeArr(message, errMessage.getBytes("UTF-8"));
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

    private void createAndSendBCastPacket(int delAdd, byte[] filename) {
        Enumeration<Integer> keys = holder.loggedIn.keys();
        byte[] op = { (byte) 0, (byte) 9 };
        byte[] delAddbytes = { (byte) delAdd };
        byte[] message = mergeArr(op, delAddbytes);
        message = mergeArr(message, filename);
        message = mergeArr(message, zero);
        while (keys.hasMoreElements()) {
            activeConnections.send(keys.nextElement(), message);
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
}