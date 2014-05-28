package org.firepick;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

// The following imports are required to run test content
import java.net.*;
import java.util.*;

public class ServiceDirectoryTest extends TestCase {

  public ServiceDirectoryTest( String testName ) {
    super( testName );
  }

  public static Test suite() {
    return new TestSuite( ServiceDirectoryTest.class );
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

  public void testServiceResolver() throws Exception {
    AbstractCollection<InetAddress> addresses = IPv4Scanner.scanLocal256(500);
    int count = 0;
    for (InetAddress addr: addresses) {
      ServiceResolver resolver = new ServiceResolver(addr);
      System.out.print(addr.getHostAddress());
      System.out.print(" ");
      System.out.print(addr.getCanonicalHostName());
      System.out.print(" => ");
      assertEquals(0, resolver.getAttempts());
      
      JSONResult result = resolver.getConfig();
      URL url = resolver.getURL();
      InetAddress iaddr = resolver.getAddress();
      if (result == null) {
        System.out.print("null");
	if (url == null) {
	  assertNotNull(iaddr);
	}
      } else {
        count++;
        System.out.print("title:");
	JSONResult firerest = result.get("FireREST");
	assertNotNull(firerest);
	String title = firerest.get("title").getString();
	assertNotNull(firerest);
	System.out.print(title);
	System.out.print(" provider:");
	String provider = firerest.get("provider").getString();
	assertNotNull(provider);
	System.out.print(provider);
	String version = firerest.get("version").getString();
	assertNotNull(version);
	System.out.print(" version:");
	System.out.println(version);
	JSONResult cv = result.get("cv");
	assertNotNull(cv);
	assertNotNull(url);
      }
      System.out.println();
    }
    assertTrue(count > 0);
  }

}

