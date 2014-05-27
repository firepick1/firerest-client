package org.firepick;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

// The following imports are required to run test content
import java.net.*;
import java.util.*;

public class DiscoverServicesTest extends TestCase {

  public DiscoverServicesTest( String testName ) {
    super( testName );
  }

  public static Test suite() {
    return new TestSuite( DiscoverServicesTest.class );
  }

  public void testLocal256() throws Exception {
    InetAddress addr255 = InetAddress.getByName("255.254.253.252");
    long long255 = 0xfffefdfcL;
    InetAddress addr1 =  InetAddress.getByName("1.2.3.4");
    long long1 = 0x01020304L;
    assertEquals(long255, IPv4Scanner.asLongAddress(addr255));
    assertEquals("255.254.253.252", addr255.getHostAddress());
    assertEquals("255.254.253.252", IPv4Scanner.asInetAddress(long255).getHostAddress());
    assertEquals(long1, IPv4Scanner.asLongAddress(addr1));
    assertEquals("1.2.3.4", IPv4Scanner.asInetAddress(long1).getHostAddress());
    assertEquals("1.2.3.4", addr1.getHostAddress());

    AbstractCollection<InetAddress> addresses = IPv4Scanner.scanLocal256(500);
    assertTrue(addresses.size() > 0); // localhost
    System.out.println("Addresses found:");
    for (InetAddress addr: addresses) {
      System.out.print(addr.getHostAddress());
      System.out.print(" ");
      System.out.println(addr.getCanonicalHostName());
    }
  }

}

