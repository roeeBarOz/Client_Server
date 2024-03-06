package bgu.spl.net.impl.tftp;

public class WRQPacket{
    private int opcode;
    private String fileName;

    public WRQPacket(int opcode, String filename){
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