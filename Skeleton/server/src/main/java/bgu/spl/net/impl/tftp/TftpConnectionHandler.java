package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

public class TftpConnectionHandler implements Runnable, ConnectionHandler<byte[]> {

    private final BidiMessagingProtocol protocol;
    private final MessageEncoderDecoder<byte[]> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;
    private static int id = 0;

    public TftpConnectionHandler(Socket sock, MessageEncoderDecoder reader, BidiMessagingProtocol protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
        Connections<byte[]> c = new TftpConnections<>();
        c.connect(0,this);
        this.protocol.start(id++, c);
    }


    @Override
    public void run() {
        try (Socket sock = this.sock) { //just for automatic closing
            int read;
            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());
            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                byte[] nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                   protocol.process(nextMessage);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    @Override
    public void send(byte[] msg) {
        synchronized(out){
            try{
                out.write(msg);
                out.flush();
            } catch (IOException e) {}
        }
    }
    
}