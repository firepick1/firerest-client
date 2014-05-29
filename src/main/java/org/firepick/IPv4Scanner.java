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

  /**
   * Scan a range of InetAddresses starting with the given address
   *
   * @param start starting address of search; null implies subnetAddress0(null,24)
   * @param count number of addresses in range
   * @param msTimeout maximum time to wait for each host
   */
  public static Collection<InetAddress> scanRange(InetAddress start, int count, int msTimeout) {
    if (start == null) {
      start = subnetAddress0(null, 24);
    }
    logger.info("scanning {} addresses starting with {}", count, start.getHostAddress());
    if (count < 1 || 4096 <= count) {
      throw new FireRESTException("Expected 0 < count < 4096");
    }
    Collection<InetAddress> result = new ArrayList<InetAddress>();
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
   * @param addr any machine in subnet or null for localhost
   * @param subnetBits the number of bits in subnet mask. Many small networks use 24.
   * @return first address on subnet. E.g., subnetAddress(10.0.1.88, 25) -> 10.0.1.0
   */
  public static InetAddress subnetAddress0(InetAddress addr, int subnetBits) {
    if (addr == null) {
      try {
	addr = InetAddress.getLocalHost();
      } catch (UnknownHostException e) {
	throw new FireRESTException(e); // Should not happen
      }
    }
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
