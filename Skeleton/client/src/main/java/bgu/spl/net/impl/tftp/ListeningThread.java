package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class ListeningThread implements Runnable {

    private final ClientTftpProtocol protocol;
    private final ClientTftpEncoderDecoder encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;
    private Thread keyboard;

    public ListeningThread(Socket sock, ClientTftpEncoderDecoder reader, ClientTftpProtocol protocol, Thread keyboard) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
        this.keyboard = keyboard;
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
                    byte[] response = protocol.process(nextMessage);
                    if (response != null) {
                            out.write(encdec.encode(response));
                            out.flush();
                    }
                }
                synchronized(keyboard){
                    keyboard.notifyAll();
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    public void send(byte[] msg) {
        //IMPLEMENT IF NEEDED
    }
    
}
