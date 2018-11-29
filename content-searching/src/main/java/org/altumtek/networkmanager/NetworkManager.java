package org.altumtek.networkmanager;

import org.altumtek.Request.*;
import org.altumtek.communication.HeartBeatManager;
import org.altumtek.networkmanager.utils.IContentSearch;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.*;
import java.util.Random;

public class NetworkManager {

    private static final String BOOTSTRAP_SERVER_IP_STR = "10.8.97.58";
    private static final int BOOTSTRAP_SERVER_PORT = 55555;

    private final InetAddress BOOTSTRAP_SERVER_IP;
    private final InetAddress IP_ADDRESS;
    private final int PORT;
    private final int SEARCH_PORT;
    private final String USER_NAME;
    private final DatagramSocket networkManagerSocket;

    private static NetworkManager networkManager;
    private RouteTable routeTable;
    private GossipManager gossipManager;
    private HeartBeatManager heartBeatManager;
    private BootstrapManger bootstrapManger;
    private SearchManager searchManager;
    private JoinManager joinManager;

    private static final Logger logger = Logger.getLogger(NetworkManager.class);
//    private Map<String, BaseRequest> sendMessages; //send messages
//    private Map<String, BaseRequest> receiveMessages; //send messages


    private NetworkManager(String ip) throws UnknownHostException, SocketException {
        this.BOOTSTRAP_SERVER_IP = InetAddress.getByName(BOOTSTRAP_SERVER_IP_STR);
        this.IP_ADDRESS = findIP(ip);
        this.PORT = new Random().nextInt(10000) + 1200; // ports above 1200
        this.SEARCH_PORT = 8080;//new Random().nextInt(10000) + 1200; // ports above 1200
        this.USER_NAME = "Altumtek";
        this.networkManagerSocket = new DatagramSocket(this.PORT);
//        this.sendMessages = new ConcurrentHashMap<>();
//        this.receiveMessages = new ConcurrentHashMap<>();
    }
    public static NetworkManager getInstance(String ip) {
        try {
            networkManager = new NetworkManager(ip);
            return networkManager;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static NetworkManager getInstance() {
        return networkManager;

//        if (networkManager != null) return networkManager;
//
//        try {
//            networkManager = new NetworkManager();
////            networkManager.init();
//        } catch (UnknownHostException | SocketException e) {
//            e.printStackTrace();
//        }
//        return networkManager;
    }

    private void init() {

        this.routeTable = new RouteTable();

        this.listenMessages();

        this.gossipManager = new GossipManager();
        this.gossipManager.start();

        this.heartBeatManager = new HeartBeatManager();
        this.heartBeatManager.start();

        this.searchManager = new SearchManager();
        this.searchManager.start();

        this.joinManager = new JoinManager();
        this.joinManager.start();

        logger.info("Bootstrap connecting....");
        this.bootstrapManger = new BootstrapManger();
        this.bootstrapManger.connectBootstrapServer(BOOTSTRAP_SERVER_IP, BOOTSTRAP_SERVER_PORT);

    }

    private void listenMessages() {
        new Thread(() -> {
            while (true) {
                byte[] buffer = new byte[65536];
                DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
                try {
                    networkManagerSocket.receive(incoming);
                    BaseRequest request = BaseRequest.deserialize(incoming);

                    if (request instanceof GossipRequest) {
                        gossipManager.addGossipRequest((GossipRequest) request);
                    } else if (request instanceof HeartbeatRequest) {
                        heartBeatManager.queueHBMessage((HeartbeatRequest) request);
                    } else if (request instanceof BootstrapServerRequest) {
                        bootstrapManger.handleConnectResponse((BootstrapServerRequest) request);
                    } else if (request instanceof JoinRequest){
                        joinManager.addJoinRequestToQueue((JoinRequest) request);
                    }else if (request instanceof SearchRequest) {
                        searchManager.addSearchRequest((SearchRequest) request);
                    } else if (request instanceof LeaveRequest) {
                        bootstrapManger.handleConnectResponse(request);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void sendMessages(BaseRequest request, InetAddress ip, int port) {
        try {
            request.send(ip, port, networkManagerSocket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public InetAddress getIpAddress() {
        return IP_ADDRESS;
    }

    public int getPort() {
        return PORT;
    }

    public int getSearchPort() {
        return SEARCH_PORT;
    }

    public String getUserName() {
        return USER_NAME;
    }

    public RouteTable getRouteTable() {
        return routeTable;
    }

    public JoinManager getJoinManager(){
        return joinManager;
    }

    private static InetAddress findIP(String ip) throws UnknownHostException {
        return InetAddress.getByName(ip);
    }


    /**
     * Search the network
     *
     * @param name search query
     * @param app  app to send results
     */
    public void search(String name, IContentSearch app) {
        this.searchManager.sendSearchRequest(name.replaceAll(" ","-"), app);
    }

    public void stop() {
        this.bootstrapManger.disconnectBootstrapServer(BOOTSTRAP_SERVER_IP, BOOTSTRAP_SERVER_PORT);
        //TODO off different ports
    }

    public void resetInstance() {
        System.exit(0);
        networkManagerSocket.disconnect();
        networkManagerSocket.close();
        networkManager = null;
    }

    public void start() {
        this.init();
    }

    public String getIpPort() {
        return IP_ADDRESS.getHostAddress() + " : " + PORT;
    }

}
