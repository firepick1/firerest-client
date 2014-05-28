package org.firepick;

import java.net.*;
import java.util.*;


public class IPv4Scanner implements Runnable {
  long ipstart;
  long ipend;
  int msTimeout;
  List<InetAddress> addresses = new ArrayList<InetAddress>();

  public static AbstractCollection<InetAddress> scanLocal256(int msTimeout) throws UnknownHostException, InterruptedException {
    AbstractCollection<InetAddress> result = new ArrayList<InetAddress>();
    InetAddress localhost = InetAddress.getLocalHost();
    System.out.println("localhost: " + localhost.getHostAddress());

    long local0 = asLongAddress(localhost) & 0xFFFFFF00L;
    System.out.println("localhost: " + Long.toHexString(local0));

    List<IPv4Scanner> scanners = new ArrayList<IPv4Scanner>();
    List<Thread> threads = new ArrayList<Thread>();
    long addr = local0;
    for (int i = 0; i < 32; i++) {
      InetAddress iaddr = asInetAddress(addr);
      IPv4Scanner scanner = new IPv4Scanner(iaddr, 8, msTimeout);
      scanners.add(scanner);
      addr += 8;
    }
    for (IPv4Scanner scanner: scanners) {
      Thread thread = new Thread(scanner);
      threads.add(thread);
      thread.start();
    }
    for (Thread thread: threads) {
      thread.join();
    }
    for (IPv4Scanner scanner: scanners) {
      result.addAll(scanner.getAddresses());
    }

    return result;
  }

  public IPv4Scanner(InetAddress iaddr, int count, int msTimeout) {
    this.ipstart = asLongAddress(iaddr);
    this.ipend = this.ipstart + count;
    this.msTimeout = msTimeout;
  }

  public static long asLongAddress(InetAddress addr) {
    byte [] rawaddr = addr.getAddress();
    long result = 0;
    for (int i = 0; i < 4; i++) {
      result <<= 8;
      result |= 0xff & (long)rawaddr[i];
    }
    return result;
  }

  public List<InetAddress> getAddresses() {
    return new ArrayList<InetAddress>(addresses);
  }

  public static InetAddress asInetAddress(long addr) throws UnknownHostException {
    byte [] rawaddr = new byte[4];
    rawaddr[3] = (byte)(addr & 0xff);
    addr >>= 8;
    rawaddr[2] = (byte)(addr & 0xff);
    addr >>= 8;
    rawaddr[1] = (byte)(addr & 0xff);
    addr >>= 8;
    rawaddr[0] = (byte)(addr & 0xff);
    return InetAddress.getByAddress(rawaddr);
  }

  public void run () {
    try {
      for (long ip=ipstart; ip < ipend; ip++) {
	  InetAddress addr = asInetAddress(ip);
	  //StringBuilder sb = new StringBuilder();
	  //sb.append(addr.getHostAddress());
	  //sb.append(" is ");
	  if (addr.isReachable(msTimeout)) {
	    //sb.append(addr.getCanonicalHostName());
	    //System.out.println(sb);
	    addresses.add(addr);
	  }
      }
    } catch(Exception e){
      throw new RuntimeException(e);
    }
  }
}
