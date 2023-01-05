package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import server.Device;

public class Chat {

	public static void main(String[] args) throws SocketException, UnknownHostException {
		new Chat();
	}
	
	public InetAddress stunServerAddr;
	public DatagramPacket holePunchPack;
	public DatagramSocket socket;
	public boolean running = true;
	public boolean blocked = true;
	List<Device> devices;
	
	public Device peer;
	public Scanner sc = new Scanner(System.in);
	public String myName;
	
	public Chat() throws SocketException, UnknownHostException {
		socket = new DatagramSocket();
		devices = new ArrayList<>();
		
		System.out.print("What is your name: ");
		myName = sc.nextLine();
		
		System.out.print("What is the IP address of the STUN server: ");
		stunServerAddr = InetAddress.getByName(sc.nextLine());
		
		byte[] data = new byte[1];
		data[0] = -2;
		holePunchPack = new DatagramPacket(data, data.length);
		
		new Thread(this::stun).start();
		new Thread(this::waitConsole).start();
		new Thread(this::receive).start();
	}
	
	public void waitConsole() {
		while(running) {
			String message = sc.nextLine();
			
			if(peer == null) {
				int idx = Integer.parseInt(message);
				
				if(idx >= 0 && idx < devices.size()) {
					peer = devices.get(idx);
					
					System.out.println("Now chatting with: " + peer.name + " (" + peer.address + ":" + peer.port + ")");
					holePunch();
				}else{
					System.err.println("Peer number " + idx + " does not exist");
				}
				
				continue;
			}
			
			byte[] buf = message.getBytes();
			byte[] toSend = new byte[buf.length + 1];
			System.arraycopy(buf, 0, toSend, 1, buf.length);
			toSend[0] = -1;
			DatagramPacket data = new DatagramPacket(toSend, toSend.length, peer.address, peer.port);
			
			try {
				socket.send(data);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		sc.close();
	}
	
	public void holePunch() {
		holePunchPack.setAddress(peer.address);
		holePunchPack.setPort(peer.port);
		try {
			socket.send(holePunchPack);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void receive() {
		byte[] buf = new byte[1024];
		DatagramPacket data = new DatagramPacket(buf, buf.length);
		while(running) {
			try {
				socket.receive(data);
				
				switch(data.getData()[0]) {
				case -2:
					//ignore -- this is where we do hole punching/keepalive
					break;
				case -1:
					incomingMessage(data.getData(), data.getLength());
					break;
				default:
					peers(data.getData(), data.getLength());
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void incomingMessage(byte[] data, int length) {
		String message = new String(data, 1, length - 1);
		
		if(peer == null)
			System.out.println("<Unknown peer> : " + message);
		else
			System.out.println(peer.name + ": " + message);
	}
	
	public void peers(byte[] data, int length) {
		int num = data[0];
		
		List<Device> devices = new ArrayList<>();
		
		int pos = 1;
		for(int i = 0; i < num; i++) {
			int addrLength = data[pos++];
			byte[] addr = new byte[addrLength];
			for(int j = 0; j < addrLength; j++) {
				addr[j] = data[pos++];
			}
			
			int portNum = 0;
        	portNum += 0xFF & data[pos++];
        	portNum <<= 8;
        	portNum += 0xFF & data[pos++];
        	portNum <<= 8;
        	portNum += 0xFF & data[pos++];
        	portNum <<= 8;
        	portNum += 0xFF & data[pos++];
        	
        	int nameLen = data[pos++];
        	byte[] name = new byte[nameLen];
        	
        	for(int j = 0; j < nameLen; j++) {
        		name[j] = data[pos++];
        	}
        	
			try {
				devices.add(new Device(portNum, InetAddress.getByAddress(addr), 0, new String(name)));
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		
		if(!devices.equals(this.devices)) {
			this.devices = devices;
			
			System.out.println("---New devices available to chat!---");
			//or the alternative, where some devices that used to be online are now offline
			int idx = 0;
			for(Device d: devices) {
				System.out.println(idx + ". " + d.name);
				idx++;
			}
			System.out.println("-------------------------------------");
		}
	}

	public void stun() {
		String request = "add " + myName + "\0";
		byte[] bytes = request.getBytes();
		
		try {
			DatagramPacket packet = new DatagramPacket(bytes, bytes.length, stunServerAddr, 5000);
			
			while(running) {
				socket.send(packet);
				
				if(peer != null)
					holePunch();
				
				Thread.sleep(5 * 1000);
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	

}
