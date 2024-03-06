package bgu.spl.net.impl.tftp;

public class DATAPacket{
    private int opcode;
    private int packetSize;
    private int blockNumber;
    private String fileName;

    public DATAPacket(int opcode,int packetSize,int blockNumber, String filename){
        this.opcode = opcode;
        this.packetSize = packetSize;
        this.blockNumber = blockNumber;
        this.fileName = filename;
    }

    public int Opcode(){
        return opcode;
    }

    public int PacketSize(){
        return packetSize;
    }

    public int BlockNumber(){
        return blockNumber;
    }

    public String FileName(){
        return fileName;
    }
}