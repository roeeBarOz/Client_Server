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
        String serverIP = /* args[0] */"127.0.0.1";
        ClientTftpProtocol protocol = new ClientTftpProtocol();
        ClientTftpEncoderDecoder encdec = new ClientTftpEncoderDecoder();
        int port = /* Integer.valueOf(args[1]) */7777;
        boolean shouldTerminate = false;
        Socket clientSocket = new Socket(serverIP, port);
        BufferedOutputStream outToServer = new BufferedOutputStream(clientSocket.getOutputStream());
        ListeningThread listener = new ListeningThread(clientSocket, encdec, protocol, Thread.currentThread());
        Thread listenerThread = new Thread(listener);
        listenerThread.start();
        System.out.println("Client started");
        while (!protocol.shouldTerminate()) {
            String input = keyboard.nextLine();
            if (isValid(input)) {
                try {
                    byte[] res = encdec.encode(protocol.processTo(input));
                    synchronized(outToServer){
                        outToServer.write(res);
                        outToServer.flush();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (input.equals("DISC")) {
                    try {
                        listenerThread.join();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    listener.close();
                    break;
                } else {
                    synchronized (Thread.currentThread()) {
                        try {
                            Thread.currentThread().wait();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private static boolean isValid(String input) {
        String[] temp = input.split("\\s+");
        if (temp.length == 0)
            return false;
        if (temp[0].equals("LOGRQ") || temp[0].equals("RRQ") || temp[0].equals("WRQ") || temp[0].equals("DELRQ")) {
            if (temp.length < 2) {
                System.out.println("Invalid command");
                return false;
            } else {
                return true;
            }
        }
        if (temp[0].equals("DIRQ") || temp[0].equals("DISC")) {
            if (temp.length < 1) {
                System.out.println("Invalid command");
                return false;
            } else {
                return true;
            }
        }
        System.out.println("Invalid command");
        return false;
    }
}
