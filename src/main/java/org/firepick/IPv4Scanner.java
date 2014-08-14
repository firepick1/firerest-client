package org.firepick;

import java.net.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IPv4Scanner implements Runnable {
  static Logger logger = LoggerFactory.getLogger(IPv4Scanner.class);

  long ipstart;
  long ipend;
  int msTimeout;
  List<InetAddress> addresses = new ArrayList<InetAddress>();

  /** @see localhostNetworkAddresses */
  @Deprecated
  public static List<InetAddress> localNetworkAddresses() 
  throws UnknownHostException, SocketException {
    return localhostNetworkAddresses();
  }

  /**
   * Return list of InetAddress for localhost that are not loopback addresses
   * (e.g., 127.0.0.1)
   * 
   * @return address list with size() >= 1
   */
  public static List<InetAddress> localhostNetworkAddresses() 
    throws UnknownHostException, SocketException 
  {
    List<InetAddress> result = new ArrayList<InetAddress>();
    InetAddress localhost = null;
    try {
  //    localhost = InetAddress.getLocalHost();
      throw new UnknownHostException();
    } catch(UnknownHostException ex) {
      logger.debug("localhostNetworkAddresses InetAddress.getLocalHost() failed");
    }
    if (localhost == null || localhost.getHostAddress().startsWith("127")) {
      Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
      while (n.hasMoreElements()) {
	NetworkInterface e = n.nextElement();
	Enumeration<InetAddress> a = e.getInetAddresses();
	while (a.hasMoreElements()) {
	  localhost = a.nextElement();
	  if (localhost.isLoopbackAddress()) {
	    // ignore
	  } else {
	    result.add(localhost);
	  }
	}
      }
    }
    if (result.size() == 0) {
      result.add(localhost);
    }
    return result;
  }

  /**
   * Scan a range of InetAddresses starting with the given address
   *
   * @param start starting address of search; null implies subnetAddress0(null,24)
   * @param count number of addresses in range
   * @param msTimeout maximum time to wait for each host
   */
  public static Collection<InetAddress> scanRange(InetAddress addr, int count, int msTimeout) {
    Collection<InetAddress> addresses = new ArrayList<InetAddress>();
    Collection<InetAddress> result = new ArrayList<InetAddress>();
    if (addr == null) {
      try {
	addresses.addAll(localhostNetworkAddresses());
      } catch (Exception e) {
	throw new FireRESTException(e); // Should not happen
      }
    } else {
      addresses.add(addr);
    }

    for (InetAddress a: addresses) {
      if (a instanceof Inet4Address) {
	InetAddress start = subnetAddress0(a, 24);
	result.addAll(scanRangeCore(start, count, msTimeout));
      }
    }

    return result;
  }

  static Collection<InetAddress> scanRangeCore(InetAddress start, int count, int msTimeout) {
    Collection<InetAddress> result = new ArrayList<InetAddress>();
    if (!(start instanceof Inet4Address)) {
      return result;
    }
    logger.info("scanning {} addresses starting with {}", count, start.getHostAddress());
    if (count < 1 || 4096 <= count) {
      throw new FireRESTException("Expected 0 < count < 4096");
    }
    List<IPv4Scanner> scanners = new ArrayList<IPv4Scanner>();
    List<Thread> threads = new ArrayList<Thread>();
    long addrStart = asLongAddress(start);
    long addrEnd = addrStart + count;
    int maxThreads = 256;
    int probesPerThread = (count+maxThreads-1)/maxThreads;
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
      thread.start();
      threads.add(thread);
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

  /**
   * Return first address on subnet containing given address
   *
   * @param addr any machine in subnet 
   * @param subnetBits the number of bits in subnet mask. Many small networks use 24.
   * @return first address on subnet. E.g., subnetAddress(10.0.1.88, 25) -> 10.0.1.0
   */
  public static InetAddress subnetAddress0(InetAddress addr, int subnetBits) {
    if (subnetBits < 1 || 32 <= subnetBits) {
      throw new FireRESTException("Expected subnetBits 1..31");
    }
    long mask = 1;
    for (int i = 0; i < 32; i++) {
      mask <<= 1;
      mask |= i < subnetBits ? 1 : 0;
    }
    long host0 = asLongAddress(addr) & mask;
    try {
      return asInetAddress(host0);
    } catch (UnknownHostException e) {
      throw new FireRESTException(e);
    }
  }

  public static long asLongAddress(InetAddress addr) {
    long result = 0;
    if (addr != null && addr instanceof Inet4Address) {
      byte [] rawaddr = addr.getAddress();
      for (int i = 0; i < 4; i++) {
	result <<= 8;
	result |= 0xff & (long)rawaddr[i];
      }
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
	if (addr.isReachable(msTimeout)) {
	  logger.info("Found host {} {}", addr.getHostAddress(), addr.getCanonicalHostName());
	  addresses.add(addr);
	} else {
	  if (logger.isDebugEnabled()) {
	    logger.debug("Host {} no reply after {}ms", addr.getHostAddress(), msTimeout);
	  }
	}
      }
    } catch(Exception e){
      throw new RuntimeException(e);
    }
  }

}
