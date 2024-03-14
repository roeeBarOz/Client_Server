package bgu.spl.net.impl.tftp;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;

import bgu.spl.net.impl.echo.EchoClient;

public class TftpClient {
    // TODO: implement the main logic of the client, when using a thread per client
    // the main logic goes here
    
    public static void main(String[] args) throws IOException {
        Scanner keyboard = new Scanner(System.in);
        String serverIP = args[0];
        ClientTftpProtocol protocol = new ClientTftpProtocol();
        ClientTftpEncoderDecoder encdec = new ClientTftpEncoderDecoder();
        int port = Integer.valueOf(args[1]);
        boolean shouldTerminate = false;
        Socket clientSocket = new Socket(serverIP, port);
        BufferedOutputStream outToServer = new BufferedOutputStream(clientSocket.getOutputStream());
        
        Thread listener = new Thread(new ListeningThread(clientSocket,encdec, protocol));
        listener.start();
        System.out.println("Client started");
        while(!protocol.shouldTerminate()){
            String input = keyboard.nextLine();
             if(isValid(input)){
                    try{
                        byte[] res = encdec.encode(protocol.processTo(input));
                        outToServer.write(res);
                        outToServer.flush();
                        
                    }
                    catch(IOException e){
                        e.printStackTrace();;
                    }
             }
        }
        
        

        
        clientSocket.close();
    }


    private static boolean isValid(String input){
        String[] temp = input.split("\\s+");
        if(temp.length == 0)
            return false;
        if(temp[0].equals("LOGRQ") || temp[0].equals("RRQ") || temp[0].equals("WRQ") || temp[0].equals("DELRQ")){
            if(temp.length < 2){
                System.out.println("Invalid command");
                return false;
            }
            else{
                return true;
            }
        }
        if(temp[0].equals("DIRQ") || temp[0].equals("DISC")){
            if(temp.length < 1){
                System.out.println("Invalid command");
                return false;
            }
            else{
                return true;
            }
        }
        System.out.println("Invalid command");
        return false;
    }
}
