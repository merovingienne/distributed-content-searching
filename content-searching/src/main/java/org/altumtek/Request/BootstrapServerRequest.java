package org.altumtek.Request;

import org.altumtek.networkmanager.NetworkManager;
import org.altumtek.networkmanager.RouteTable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class BootstrapServerRequest extends BaseRequest{

    private List<RouteTable.Node> neighbourList = new ArrayList<>();

    public BootstrapServerRequest(RequestType type, InetAddress myIP, int myPort, String myName) {
        if (type == RequestType.REG) {
            message = String.format("REG %s %d %s", this.senderIP, this.senderPort,
                    NetworkManager.getInstance().getUserName());
        } else if (type == RequestType.UNREG) {
            message = String.format("UNREG %s %d %s", myIP, myPort, myName);
        } else {
            throw new RuntimeException("Invalid request type"); // Todo replace with custom exception
        }


    }

    public BootstrapServerRequest(String incomingMessage) {
        StringTokenizer tokens = new StringTokenizer(incomingMessage, " ");
        String command = tokens.nextToken();

        if (command.equals("REGOK")) {
            this.type = RequestType.REGOK;
            int nodes = Integer.parseInt(tokens.nextToken());
            for (int i = 0; i < nodes; i++) {
                try {
                    InetAddress neighbourAddress = InetAddress.getByName(tokens.nextToken());
                    int neighbourPort = Integer.parseInt(tokens.nextToken());
                    neighbourList.add(new RouteTable.Node(true, neighbourAddress, neighbourPort));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        } else if(command.equals("UNROK")) {
            int value = Integer.parseInt(tokens.nextToken());
            if (value == 0) {
                this.type = RequestType.LEAVEOK;
            } else {
                this.type = RequestType.ERROR;
            }
        }
    }

    public List<RouteTable.Node> getNeighbourList() {
        return neighbourList;
    }
}
