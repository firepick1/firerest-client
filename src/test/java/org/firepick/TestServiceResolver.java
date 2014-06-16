package org.firepick;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

// The following imports are required to run test content
import java.net.*;
import java.util.*;

public class TestServiceResolver extends TestCase {

  public TestServiceResolver( String testName ) {
    super( testName );
  }

  public static Test suite() {
    return new TestSuite( TestServiceResolver.class );
  }

  public void testServiceResolver() throws Exception {
    InetAddress host0 = IPv4Scanner.subnetAddress0(null, 24);
    Collection<InetAddress> addresses = IPv4Scanner.scanRange(host0, 255, 500);
    int count = 0;
    for (InetAddress addr: addresses) {
      ServiceResolver resolver = new ServiceResolver(addr);
      assertEquals(0, resolver.getAttempts());
      
      JSONResult result = resolver.getConfig();
      URL url = resolver.getURL();
      InetAddress iaddr = resolver.getAddress();
      if (result == null) {
	if (url == null) {
	  assertNotNull(iaddr);
	}
      } else {
        count++;
	JSONResult firerest = result.get("FireREST");
	assertNotNull(firerest);
	String title = firerest.get("title").getString();
	assertNotNull(firerest);
	String provider = firerest.get("provider").getString();
	assertNotNull(provider);
	String version = firerest.get("version").getString();
	assertNotNull(version);
	JSONResult cv = result.get("cv");
	assertNotNull(cv);
	assertNotNull(url);
      }
    }
  }

}

