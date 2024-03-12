package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.Server;
import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.chrono.ThaiBuddhistChronology;
import java.util.function.Supplier;

import javax.print.attribute.standard.Chromaticity;


public class TftpServer implements Server<byte[]>{
    //TODO: Implement this

    private final int port;
    private final Supplier<BidiMessagingProtocol<byte[]>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<byte[]>> encdecFactory;
    private ServerSocket sock;

    public TftpServer(
            int port,
            Supplier<BidiMessagingProtocol<byte[]>> protocolFactory,
            Supplier<MessageEncoderDecoder<byte[]>> encdecFactory) {
                this.port = port;
                this.protocolFactory = protocolFactory;
                this.encdecFactory = encdecFactory;
                this.sock = null;
    }

    @Override
    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
			System.out.println("Server started");

            this.sock = serverSock; //just to be able to close

            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSock = serverSock.accept();
                TftpConnectionHandler handler = new TftpConnectionHandler(
                        clientSock,
                        encdecFactory.get(),
                        protocolFactory.get());

                execute(handler);
            }
        } catch (IOException ex) {
        }

        System.out.println("server closed!!!");
    }

    @Override
    public void close() throws IOException {
		if (sock != null){}
			//sock.close();
    }

    protected void execute(TftpConnectionHandler handler) {
        new Thread(handler).start();    
    }
    

    public static void main(String[] args) {

        // you can use any server... 
        Supplier<BidiMessagingProtocol<byte[]>> protocolSupplier = () -> new TftpProtocol();
        Supplier<MessageEncoderDecoder<byte[]>> encdecSupplier = () -> new TftpEncoderDecoder();
        TftpServer server = new TftpServer(Integer.valueOf(args[0]), protocolSupplier, //protocol factory
        encdecSupplier) ;//message encoder decoder factory);
        server.serve();
        try {
            server.close();
        } catch (IOException e) {}
    }

}