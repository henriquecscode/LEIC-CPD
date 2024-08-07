private void logJoin(String id, int counter)
{
    Log log = new Log(Ops.JOIN, id, counter);
    this.replaceLog(log, counter);
}

// Is the node the original node?
Boolean act =
!(op.equals(RequestHandler.PUT_INFORM)
|| op.equals(RequestHandler.DELETE_INFORM)
|| op.equals(RequestHandler.GET_INFORM));

//Do the operation
mem.del(hash);
//If is the original node then inform neighbors
if (act) {
    message.setHeader(0, RequestHandler.DELETE_INFORM);
    this.informNeighbours(message);// Inform prev
}

if (nodeData == membershipData.getPrev(data.getAddress()))
{
    this.nodeReplicateOwnKeysTo(data);
}
else if (nodeData == membershipData.getSucessor(data.getAddress()))
{
    this.nodeReplicateOwnKeysTo(data);
}

NodeData prev = membershipData.getPrev(leavingAddress);
NodeData suc = membershipData.getSucessor(leavingAddress);
if (nodeData.equals(prev)) {
    // If we are its predecessor
    // Send to its sucessor
    this.nodeReplicateOwnKeysTo(suc);
} else if (nodeData.equals(suc)) {
    // If we are its sucessor
    // Send to its predecessor
    this.nodeReplicateOwnKeysTo(prev);
}

NodeData prev =
    membershipData
    .getPrev(nodeData.getAddress());
NodeData suc =
    membershipData
    .getSucessor(nodeData.getAddress());
NodeData nextSuc =
    membershipData
    .getSucessor(suc.getAddress());
this.nodeReplicateOwnKeysTo(prev);
this.nodeReplicateOwnKeysTo(suc);
this.nodeReplicateOwnKeysTo(nextSuc);

Socket socket = null;
try {
socket = this.connectionMembershipProtocolSocket
        .accept();
} catch (IOException e) {
    e.printStackTrace();
}

InputStream input = null;

try {
    input = socket.getInputStream();
} catch (IOException e) {
    e.printStackTrace();
}

BufferedReader reader = new BufferedReader(new InputStreamReader(input));

while (true) {
	Message message = new Message(reader);
}

this.node.threadPool.submit(() -> {
    	this.processMessage(message);
});
