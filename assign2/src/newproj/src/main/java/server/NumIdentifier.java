package server;

public class NumIdentifier implements Identifier<Integer> {
    int num;

    NumIdentifier(int num) {
        this.num = num;
    }

    @Override
    public Integer get() {
        return this.num;
    }
}
