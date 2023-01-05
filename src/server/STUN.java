package server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class STUN {

	public static final long TTL = 1000 * 60;
	
	public static void main(String[] args) throws Exception {
		DatagramSocket socket = new DatagramSocket(5000);
				
		byte[] buf = new byte[256];
		
		boolean running = true;
		
		PriorityQueue<Device> pq = new PriorityQueue<>();
		Map<Device, Long> devices = new HashMap<>();
		
		while(running) {
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			
			InetAddress address = packet.getAddress();
            int port = packet.getPort();
            packet = new DatagramPacket(buf, buf.length, address, port);
            
            //System.out.println(address + ":" + port);
            
            long currentTime = System.currentTimeMillis();
            
            String received = new String(packet.getData(), packet.getOffset(), Math.min(packet.getLength(), first0Index(packet.getData())));
            
            if(received.indexOf(' ') == -1)
            	continue;
            
            Device newDev = new Device(port, address, currentTime, received.substring(received.indexOf(' ')));
            
            if(devices.containsKey(newDev))
            	devices.remove(newDev);
            
        	devices.put(newDev, currentTime);
            
            pq.add(newDev);
            while(!pq.isEmpty() && (currentTime - pq.element().timestamp) > TTL) {
            	Device d = pq.remove();
            	if(currentTime - devices.get(d) > TTL) {
            		devices.remove(d);
            	}
            }
                        
            switch(received.substring(0, received.indexOf(' '))) {
	            case "end":
	            	running = false;
	            	break;
	            case "add":
	            	buf[0] = (byte) devices.size();
	            	
	            	int offsetPos = 1;
	            	for(Device d: devices.keySet()) {
	            		buf[offsetPos++] = (byte) d.address.getAddress().length;
	            		for(int i = 0; i < d.address.getAddress().length; i++) {
	            			buf[offsetPos++] = d.address.getAddress()[i];
	            		}
	            		buf[offsetPos++] = (byte)(d.port >>> 24);
	            		buf[offsetPos++] = (byte)(d.port >>> 16);
	            		buf[offsetPos++] = (byte)(d.port >>> 8);
	            		buf[offsetPos++] = (byte) d.port;
	            		
	            		byte[] nameBytes = d.name.getBytes();
	            		buf[offsetPos++] = (byte) nameBytes.length;
	            		for(int i = 0; i < nameBytes.length; i++) {
	            			buf[offsetPos++] = nameBytes[i];
	            		}
	            	}
	            	
	            	packet.setData(buf, 0, offsetPos);
	            	
	                socket.send(packet);
	            	break;
	            case "chatrequest":
	            	int start = 12;
	            	int len = buf[start++];
	            	
	            	byte[] addr = new byte[len];
	            	for(int i = 0; i < len; i++) {
	            		addr[i] = buf[start++];
	            	}
	            	
	            	InetAddress ip = InetAddress.getByAddress(addr);
	            	
	            	int portNum = 0;
	            	portNum += 0xFF & buf[start++];
	            	portNum <<= 8;
	            	portNum += 0xFF & buf[start++];
	            	portNum <<= 8;
	            	portNum += 0xFF & buf[start++];
	            	portNum <<= 8;
	            	portNum += 0xFF & buf[start++];
	            	
	            	buf[0] = -1;
	            	buf[1] = (byte) addr.length;
	            	for(int i = 2; i < addr.length + 2; i++) {
	            		buf[i] = addr[i - 2];
	            	}
            		buf[addr.length + 2] = (byte)(portNum >>> 24);
            		buf[addr.length + 3] = (byte)(portNum >>> 16);
            		buf[addr.length + 4] = (byte)(portNum >>> 8);
            		buf[addr.length + 5] = (byte) portNum;
            		
            		packet.setAddress(ip);
            		packet.setPort(portNum);
            		packet.setData(buf, 0, addr.length + 5);
            		
            		socket.send(packet);
	            	
	            	break;
            }
            
		}
		
		socket.close();
	}
	
	public static int first0Index(byte[] b) {
		for(int i = 0; i < b.length; i++) {
			if(b[i] == 0)
				return i;
		}
		return b.length;
	}

}
