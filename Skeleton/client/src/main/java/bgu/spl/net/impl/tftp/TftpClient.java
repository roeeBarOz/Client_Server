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
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        Socket clientSocket = new Socket(serverIP, port);
        BufferedOutputStream outToServer = new BufferedOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        
        while(!shouldTerminate){
            String input = keyboard.nextLine();
             if(isValid(input)){

             }
        }
        
        Thread listener = new Thread(new ListeningThread(clientSocket,encdec, protocol));
        listener.start();
        System.out.println("Client started");
        

        
        clientSocket.close();
    }


    private static boolean isValid(String input){
        String[] temp = input.split("");
        if(temp.length == 0)
            return false;
        if(temp[0] == "LOGRQ" || temp[0] == "RRQ" || temp[0] == "WRQ" || temp[0] == "DELRQ"){
            if(temp.length < 2){
                System.out.println("Invalid command");
                return false;
            }
            else{

            }
        }
        if(temp[0] == "DIRQ" || temp[0] == "DISC"){
            if(temp.length < 1){
                System.out.println("Invalid command");
                return false;
            }
            else{
                
            }
        }
        System.out.println("Invalid command");
        return false;
    }
}
