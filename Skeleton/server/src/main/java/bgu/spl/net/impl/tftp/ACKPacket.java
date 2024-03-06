package bgu.spl.net.impl.tftp;

public class ACKPacket{
    private int opcode;
    private int blockNumber;

    public ACKPacket(int opcode,int blockNumber){
        this.opcode = opcode;
        this.blockNumber = blockNumber;
    }

    public int Opcode(){
        return opcode;
    }

    public int blockNumber(){
        return blockNumber;
    }
}