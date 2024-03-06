package bgu.spl.net.impl.tftp;

public class DISCPacket{
    private int opcode;

    public DISCPacket(int opcode){
        this.opcode = opcode;
    }

    public int Opcode(){
        return opcode;
    }
}