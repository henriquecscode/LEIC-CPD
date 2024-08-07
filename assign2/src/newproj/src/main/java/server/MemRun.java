package server;

public class MemRun extends Thread {
    Memory memory;
    String fileName;
    Object data;

    MemRun(Memory memory, String fileName, Object data) {
        this.memory = memory;
        this.fileName = fileName;
        this.data = data;
    }

    public void run() {

    }

}
