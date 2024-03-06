package bgu.spl.net.impl.tftp;

import java.net.Socket;
import java.nio.charset.StandardCharsets;

import javax.swing.filechooser.FileNameExtensionFilter;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;
import java.io.File;
import java.io.IOException;

public class TftpProtocol implements BidiMessagingProtocol<byte[]> {
    private boolean shouldTerminate = false;
    private Connections<byte[]> activeConnections;
    private int ownerId;

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        // TODO implement this
        activeConnections = connections;
        ownerId = connectionId;
        // activeConnections.connect(connectionId, new
        // BlockingConnectionHandler<byte[]>(new Socket(),new
        // TftpEncoderDecoder(),));
    }

    @Override
    public void process(byte[] message) {
        // TODO implement this
        int opcode = convert(0, 1, message);
        String info;
        String response;
        int blockNumber;
        int errorCode;
        int delAdd;
        ERRORPacket error;
        switch (opcode) {
            case 1:
                info = relevantByteTostring(2, message);
                if(!createResponseToRRQ(info)){
                    error = new ERRORPacket(5, 5, info);
                    
                }
                break;
            case 2:
                info = relevantByteTostring(2, message);
                //response = createResponseToWRQ(info);
                break;
            case 3:
                info = relevantByteTostring(6, message);
                blockNumber = convert(4,5,message);
                break;
            case 4:
                blockNumber = convert(2,3,message);
                break;
            case 5:
                errorCode = convert(2,3,message);
                info = relevantByteTostring(4, message);
                break;
            case 6:
                break;
            case 7:
                info = relevantByteTostring(2, message);
                break;
            case 8:
                info = relevantByteTostring(2, message);
                break;
            case 9:
                info = relevantByteTostring(2, message);
                delAdd = message[2];
                break;
            case 10:
                shouldTerminate = true;
                break;
            default:
                break;
            
        }
    }

    private boolean createResponseToRRQ(String filename) {
        File f = new File("/Skeleton/server/Flies/"+filename);
        if(!f.exists()){
            return false;
        }
        return true;
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

    private int convert(int firstByte, int secondByte, byte[] bytes) {
        short b_short = (short) (((short) bytes[firstByte]) << 8 | (short) (bytes[secondByte]));
        return (int) b_short;
    }

    private boolean createResponseToWRQ(String filename){
        File f = new File("/Skeleton/server/Flies/"+filename);
        try {
            if(f.createNewFile()){
                return true;
            }
            else{
                return false; // file already exists
            }
        } catch (IOException e) {}
        return false;
    }
}
