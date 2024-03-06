package bgu.spl.net.impl.tftp;

public class ERRORPacket{
    private int opcode;
    private int errorCode;
    private String fileName;

    public ERRORPacket(int opcode,int errorCode, String filename){
        this.opcode = opcode;
        this.errorCode = errorCode;
        this.fileName = filename;
    }

    public int Opcode(){
        return opcode;
    }

    public int ErrorCode(){
        return errorCode;
    }

    public String FileName(){
        return fileName;
    }
}