package bgu.spl.net.impl.tftp;

import java.util.function.Supplier;

import bgu.spl.net.srv.BaseServer;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.Server;


public class TftpServer extends BaseServer{
    //TODO: Implement this
    Connections<byte[]> connections = new TftpConnections<>();

    public TftpServer(int port, Supplier protocolFactory, Supplier encdecFactory) {
        super(port, protocolFactory, encdecFactory);
        //connections.connect(port, null);
    }

    @Override
    protected void execute(BlockingConnectionHandler handler) {
        new Thread(handler).start();    
    }
    

}
