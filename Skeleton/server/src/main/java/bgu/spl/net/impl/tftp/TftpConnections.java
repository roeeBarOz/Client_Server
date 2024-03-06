package bgu.spl.net.impl.tftp;

import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

public class TftpConnections<T> implements Connections<T> {

    private ConcurrentHashMap<Integer,ConnectionHandler> clientConnections = new ConcurrentHashMap<>();  

    @Override
    public void connect(int connectionId, ConnectionHandler<T> handler) {
        clientConnections.put(connectionId, handler);
    }

    @Override
    public boolean send(int connectionId, T msg) {
        if(clientConnections.containsKey(connectionId)){
            clientConnections.get(connectionId).send(msg);
            return true;
        }
        return false;
    }

    @Override
    public void disconnect(int connectionId) {
        clientConnections.remove(connectionId);
    }
    public ConcurrentHashMap<Integer, ConnectionHandler> getClientConnections(){
        return clientConnections;
    }
    
}
