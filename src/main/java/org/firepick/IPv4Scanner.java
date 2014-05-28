package org.firepick;

import java.net.*;
import java.util.*;

public class IPv4Scanner implements Runnable {
  long ipstart;
  long ipend;
  int msTimeout;
  List<InetAddress> addresses = new ArrayList<InetAddress>();

  public static Collection<InetAddress> scanLocal256(int msTimeout) {
    InetAddress localhost;
		try {
			localhost = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			throw new FireRESTException(e); // Should not happen
		}
    //System.out.println("localhost: " + localhost.getHostAddress());

    long local0 = asLongAddress(localhost) & 0xFFFFFF00L;
		try {
			return scanRange(asInetAddress(local0), 255, msTimeout);
		} catch (UnknownHostException e) {
			throw new FireRESTException(e);
		}
	}

  public static Collection<InetAddress> scanRange(InetAddress start, int count, int msTimeout) {
    Collection<InetAddress> result = new ArrayList<InetAddress>();
    List<IPv4Scanner> scanners = new ArrayList<IPv4Scanner>();
    List<Thread> threads = new ArrayList<Thread>();
    long addrStart = asLongAddress(start);
		long addrEnd = addrStart + count;
		int probesPerThread = 8;
		for (long addr = addrStart; addr < addrEnd; addr += probesPerThread) {
			try {
				InetAddress iaddr = asInetAddress(addr);
				long nProbes = Math.min(addrEnd, addr + probesPerThread) - addr;
				IPv4Scanner scanner = new IPv4Scanner(iaddr, (int) nProbes, msTimeout);
				scanners.add(scanner);
			} catch (UnknownHostException e) {
				throw new FireRESTException(e); // should never happen since addr is always valid
			}
    }
    for (IPv4Scanner scanner: scanners) {
      Thread thread = new Thread(scanner);
      threads.add(thread);
      thread.start();
    }
    for (Thread thread: threads) {
			try {
				thread.join();
			} catch (Exception e) {
				// ignore ThreadInterruptedException
			}
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
