package bgu.spl.net.impl.tftp;

import java.net.Socket;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {
    private boolean shouldTerminate = false;
    private Connections<byte[]> activeConnections;

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        // TODO implement this
        activeConnections = connections;
        //activeConnections.connect(connectionId, new BlockingConnectionHandler<byte[]>(new Socket(),new TftpEncoderDecoder(),this));
    }

    @Override
    public void process(byte[] message) {
        // TODO implement this
        throw new UnsupportedOperationException("Unimplemented method 'process'");
    }

    @Override
    public boolean shouldTerminate() {
        // TODO implement this
        return shouldTerminate;
    } 
    
}
