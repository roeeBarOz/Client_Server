package bgu.spl.net.impl.tftp;

public class DIRQPacket{
    private int opcode;

    public DIRQPacket(int opcode){
        this.opcode = opcode;
    }

    public int Opcode(){
        return opcode;
    }
}