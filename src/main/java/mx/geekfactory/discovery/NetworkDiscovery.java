/*
 * @author Jesus Raul Santa Anna Zamudio
 */
package mx.geekfactory.com.networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkDiscovery {

    private static final boolean DEBUG_ENABLED=false;
    private static final HashMap<String, Byte> DEVICE_TYPES = new HashMap<>();
    private static final HashMap<String, Byte> MESSAGE_TYPES = new HashMap<>();
    private static final HashMap<String, Byte> PROTOCOL_VERSION = new HashMap<>();
    private static final ArrayList<String> FOUND_ADDRESSES = new ArrayList<>();
    private static DatagramSocket socket = null;

    static {
        PROTOCOL_VERSION.put("1", (byte) 0x01);
        DEVICE_TYPES.put("CLIENT_PC", (byte) 0xFF);
        DEVICE_TYPES.put("PROGRAMMABLE_TIMER", (byte) 0x01);
        MESSAGE_TYPES.put("DISCOVERY_REQUEST", (byte) 0x44);
        MESSAGE_TYPES.put("DISCOVERY_RESPONSE", (byte) 0x52);
        MESSAGE_TYPES.put("UNSOLICITED_UPDATE_MESSAGE", (byte) 0x55);
    }

    public static String[] broadcast() throws Exception {
        FOUND_ADDRESSES.clear();
        socket = new DatagramSocket();
        socket.setBroadcast(true);
        socket.setSoTimeout(500);
        byte[] sendData = {PROTOCOL_VERSION.get("1"), MESSAGE_TYPES.get("DISCOVERY_REQUEST"), DEVICE_TYPES.get("CLIENT_PC"), (byte) 0x00};
        Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();

            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                InetAddress broadcast = interfaceAddress.getBroadcast();
                if (broadcast == null) {
                    continue;
                }
                try {
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 30303);
                    socket.send(sendPacket);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        while (true) {
            try {
                byte[] recvBuf = new byte[1000];
                DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(receivePacket);
                if (receivePacket.getData()[1] == MESSAGE_TYPES.get("DISCOVERY_RESPONSE")) {
                    FOUND_ADDRESSES.add(receivePacket.getAddress().getHostAddress());
                    parseResponse(receivePacket.getData());
                }
                if (receivePacket.getLength() == 0) {
                    break;
                }
            } catch (IOException ex) {
                break;
            }
        }
        socket.close();
        String[] returnArray = new String[FOUND_ADDRESSES.size()];
        int i = 0;
        for (String s : FOUND_ADDRESSES) {
            returnArray[i] = FOUND_ADDRESSES.get(i);
            i++;
        }
        return returnArray;
    }
    
    public static ArrayList<Object> parseResponse(byte[] response){
        ArrayList<Object> data=new ArrayList();
        data.add(response[4]);
        data.add(Arrays.copyOfRange(response, 5,10));
        data.add(Arrays.copyOfRange(response, 11,26));
        return data;
    }

    public static List<InetAddress> listAllBroadcastAddresses() throws SocketException  {
        List<InetAddress> broadcastList = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces
                = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }
            networkInterface.getInterfaceAddresses().stream()
                    .map(a -> a.getBroadcast())
                    .filter(Objects::nonNull)
                    .forEach(broadcastList::add);
        }
        return broadcastList;
    }
}
