package server;

import java.net.InetAddress;

public class Device implements Comparable<Device> {

	public int port;
	public InetAddress address;
	public long timestamp;
	public String name;
	
	public Device(int port, InetAddress address, long timestamp, String name) {
		this.port = port;
		this.address = address;
		this.timestamp = timestamp;
		this.name = name;
	}
	
	@Override
	public int compareTo(Device o) {
		return Long.signum(timestamp - o.timestamp);
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Device))
			return false;
		
		Device d = (Device) o;
		
		return d.port == this.port && d.address.equals(this.address);
	}
	
	@Override
	public int hashCode() {
		return address.hashCode() + port;
	}
	
	@Override
	public String toString() {
		return address.toString() + ":" + port + " " + name;
	}
	
}
