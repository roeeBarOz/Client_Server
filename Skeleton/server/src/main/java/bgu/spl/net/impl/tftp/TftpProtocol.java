package bgu.spl.net.impl.tftp;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.filechooser.FileNameExtensionFilter;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

class holder {
    static ConcurrentHashMap<Integer, String> loggedIn = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, Integer> files = new ConcurrentHashMap<>();
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
        String info;
        int blockNumber;
        int errorCode;
        int delAdd;
        switch (opcode) {
            case 1:
                info = relevantByteTostring(2, message);
                handleRRQ(info, ownerId);
                break;
            case 2:
                info = relevantByteTostring(2, message);
                handleWRQ(info, ownerId);
                break;
            case 3:
                info = relevantByteTostring(6, message);
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
                info = relevantByteTostring(2, message);
                handleLOGRQ(info, ownerId);
                break;
            case 8:
                info = relevantByteTostring(2, message);
                handleDELRQ(info, ownerId);
                break;
            case 9:
                // server will never get a BCAST packet
                info = relevantByteTostring(2, message);
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

    @Override
    public boolean shouldTerminate() {
        // TODO implement this
        return shouldTerminate;
    }

    private String relevantByteTostring(int startingByte, byte[] message) {
        String res = "";
        for (int i = startingByte; i < message.length; i++) {
            byte[] relevant = { message[i] };
            res += new String(relevant, StandardCharsets.US_ASCII);
        }
        return res;
    }

    private int convertToShort(int firstByte, int secondByte, byte[] bytes) {
        return (short) ((short)((bytes[0] & 0xFF) << 8) | (short)(bytes[1]& 0xFF));
    }

    private byte[] convertToBytes(short num){
        return new byte[]{(byte) (num >> 8), (byte) (num & 0xff)};
    }

    private void handleRRQ(String filename, int ownerId) {
        if (holder.loggedIn.containsKey(ownerId)) {
            fileToRead = new File("Skeleton/server/Flies" + filename);
            if (!fileToRead.exists())
                createAndSendErrorPacket(ownerId, 1);
            else {
                int workOn = holder.files.remove(filename);
                holder.files.put(filename, workOn++);
                createAndSendDataPacket(filename, ownerId, 0);
            }
        } else {
            createAndSendErrorPacket(ownerId, 6);
        }
    }

    private void handleWRQ(String filename, int ownerId) {
        if (holder.loggedIn.containsKey(ownerId)) {
            fileToWrite = new File("Skeleton/server/Flies" + filename);
            try {
                if (!fileToWrite.createNewFile())
                    createAndSendErrorPacket(ownerId, 5);
                // else: we just finished
                else{
                    holder.files.put(filename, 0);
                    createAndSendBCastPacket(0, filename);
                }
            } catch (IOException e) {
                createAndSendErrorPacket(ownerId, 0);
            }

        } else {
            createAndSendErrorPacket(ownerId, 6);
        }
    }

    private void handleDATA(int ownerId, String data, int blockNumber) {
        if (holder.loggedIn.containsKey(ownerId)) {
            if (filepath.getUsableSpace() >= data.length()) {
                if (currBlockRead == blockNumber) {
                    try (FileWriter fw = new FileWriter(fileToWrite)) {
                        fw.write(data);
                        //fw.close();
                        if(data.length() < 512) {
                            int workOn = holder.files.remove(fileToWrite.getName());
                            holder.files.put(fileToWrite.getName(),workOn--);
                            fileToWrite = null;
                        }
                    } catch (IOException e) {
                        createAndSendErrorPacket(ownerId, 2);
                    }
                    currBlockRead++;
                    createAndSendAckPacket(ownerId, blockNumber);
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
            if (currBlockWrite == blockNumber) {
                try (FileInputStream fis = new FileInputStream("Skeleton/server/Flies" + fileToWrite.getName())) {
                    long skip = (blockNumber - 1) * 512;
                    long skipped = fis.skip(skip);
                    if (skipped == skip) {
                        // Create a byte array to store the read data
                        byte[] buffer = new byte[Math.min(512, (int) (fileToWrite.length() - skip))];

                        // Read 512 bytes from the current file position into the buffer
                        int bytesRead = fis.read(buffer);
                        // Check if there is any data read
                        if (bytesRead > 0) {
                            // Process the read data (in this example, just print it as a string)
                            String data = new String(buffer, 0, bytesRead);
                            createAndSendDataPacket(data, ownerId, blockNumber);
                        }
                    }
                    else{
                        int workOn = holder.files.remove(fileToWrite.getName());
                        holder.files.put(fileToWrite.getName(),workOn++); 
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
            String data = "";
            for (File f : files)
                data += f.getName() + "0";
            data = data.substring(0, data.length() - 1);
            createAndSendDataPacket(data, ownerId, 1);
        } else {
            createAndSendErrorPacket(ownerId, 6);
        }
    }

    private void handleLOGRQ(String username, int ownerId) {
        if (!holder.loggedIn.containsKey(ownerId)) {
            holder.loggedIn.put(ownerId, username);
            createAndSendAckPacket(ownerId, 0);
        } else {
            createAndSendErrorPacket(ownerId, 7);
        }
    }

    private void handleDELRQ(String filename, int ownerId) {
        if (holder.loggedIn.containsKey(ownerId)) {
            File f = new File("Skeleton/server/Flies" + filename);
            if (f.delete()){
                holder.files.remove(filename);
                createAndSendBCastPacket(1, filename);
            }
            else
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

    private void createAndSendDataPacket(String data, int ownerId, int blockNumber) {
        String message = "03" + (short) data.length() + (short) blockNumber + data;
        activeConnections.send(ownerId, message.getBytes());
    }

    private void createAndSendErrorPacket(int ownerId, int errorCode) {
        String message = "050" + (short) errorCode;
        switch (errorCode) {
            case 0:
                message += "0";
                break;
            case 1:
                // server will never get an ERROR packet
                // errorCode = convert(2,3,message);
                // info = relevantByteTostring(4, message);
                message += "File not found - RRQ DELRQ of non-existing file.0";
                break;
            case 2:
                message += "Access violation - File cannot be written, read or deleted.0";
                break;
            case 3:
                message += "Disk full or allocation exceeded - No room in disk.0";
                break;
            case 4:
                message += "Illegal TFTP operation - Unknown Opcode.0";
                break;
            case 5:
                message += "File already exists - File name exists on WRQ.0";
                break;
            case 6:
                message += "User not logged in - Any opcode received before Login completes.0";
                break;
            case 7:
                message += "User already logged in - Login username already connected.0";
                break;
            default:
                break;
        }
        activeConnections.send(ownerId, message.getBytes());
    }

    private void createAndSendAckPacket(int ownerId, int blockNumber) {
            byte[] op = convertToBytes((short)04);
            byte[] blocks = convertToBytes(((short) blockNumber));
            byte[] message = {op[0],op[1],blocks[0],blocks[1]};
            activeConnections.send(ownerId, message);
    }

    private void createAndSendBCastPacket(int delAdd, String filename) {
        Enumeration<Integer> keys = holder.loggedIn.keys();
        while (keys.hasMoreElements()) {
            activeConnections.send(keys.nextElement(), filename.getBytes());
        }
    }

}
