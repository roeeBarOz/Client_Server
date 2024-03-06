package bgu.spl.net.impl.tftp;

public class LOGRQPacket{
    private int opcode;
    private String userName;

    public LOGRQPacket(int opcode, String userName){
        this.opcode = opcode;
        this.userName = userName;
    }

    public int Opcode(){
        return opcode;
    }

    public String UserName(){
        return userName;
    }
}