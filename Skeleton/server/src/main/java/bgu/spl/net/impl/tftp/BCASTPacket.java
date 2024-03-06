package bgu.spl.net.impl.tftp;

public class BCASTPacket{
    private int opcode;
    private int delAdd;
    private String fileName;

    public BCASTPacket(int opcode,int delAdd, String filename){
        this.opcode = opcode;
        this.delAdd = delAdd;
        this.fileName = filename;
    }

    public int Opcode(){
        return opcode;
    }

    public int DelAdd(){
        return delAdd;
    }

    public String FileName(){
        return fileName;
    }
}