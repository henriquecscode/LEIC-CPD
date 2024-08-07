package server;

public class Log {
    public String counter;
    public String nodeId;
    public MembershipProtocolInterface.Ops operation;

    Log(MembershipProtocolInterface.Ops operation, String nodeId, Integer counter) {
        this.operation = operation;
        this.nodeId = nodeId;
        this.counter = Integer.toString(counter);
    }

    Log(String data) {
        String[] dataPieces = data.split(" ");
        this.operation = this.getOperationFromString(dataPieces[0]);
        this.nodeId = dataPieces[1];
        this.counter = dataPieces[2];
    }

    public Integer getCounter() {
        return Integer.parseInt(counter);
    }

    public String getNodeId() {
        return nodeId;
    }

    public String toString() {
        return operationString() + " " + nodeId + " " + counter;
    }

    public String operationString() {
        switch (operation) {
            case JOIN:
                return "JOIN";
            case LEAVE:
                return "LEAVE";
            case JOIN_FAIL:
                return "JOIN_FAIL";
        }
        return "";
    }

    public MembershipProtocolInterface.Ops getOperationFromString(String string) {
        if (string.equals("JOIN")) {
            return MembershipProtocolInterface.Ops.JOIN;
        } else if (string.equals("LEAVE")) {
            return MembershipProtocolInterface.Ops.LEAVE;
        } else { // string.equals("JOIN_FAIL")
            return MembershipProtocolInterface.Ops.JOIN_FAIL;
        }
    }

    public Boolean equals(Log log) {
        return log.getCounter().equals(counter) && log.getNodeId().equals(nodeId)
                && log.operationString().equals(operationString());
    }
}
