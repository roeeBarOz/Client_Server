package bgu.spl.net.impl.tftp;

public class ERRORPacket{
    private int opcode;
    private int errorCode;
    private String errorMsg;

    public ERRORPacket(int opcode,int errorCode, String errorMsg){
        this.opcode = opcode;
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public int Opcode(){
        return opcode;
    }

    public int ErrorCode(){
        return errorCode;
    }

    public String ErrorMsg(){
        return errorMsg;
    }
}