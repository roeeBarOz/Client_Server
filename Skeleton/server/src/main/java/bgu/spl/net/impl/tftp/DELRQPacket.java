package bgu.spl.net.impl.tftp;

public class DELRQPacket{
    private int opcode;
    private String fileName;

    public DELRQPacket(int opcode, String filename){
        this.opcode = opcode;
        this.fileName = filename;
    }

    public int Opcode(){
        return opcode;
    }

    public String FileName(){
        return fileName;
    }
}