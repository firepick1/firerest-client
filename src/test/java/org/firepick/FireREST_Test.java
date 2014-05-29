package org.firepick;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

// The following imports are required to run test content
import java.io.*;
import java.net.*;

public class FireREST_Test extends TestCase {
  /**
   * Create the test case
   *
   * @param testName name of the test case
   */
  public FireREST_Test( String testName ) {
    super( testName );
  }

  /**
   * @return the suite of tests being tested
   */
  public static Test suite() {
    try {
      URL url = new URL("http://localhost:8080/");
      Object content = url.getContent();
    } catch (Exception e) {
      StringBuilder msg = new StringBuilder();
      msg.append("\n");
      msg.append("----------------ERROR---------------\n");
      msg.append("| Test requires FireREST server.   |\n");
      msg.append("| Launch the localhost server:     |\n");
      msg.append("|   node server/firerest.js        |\n");
      msg.append("------------------------------------\n");
      System.out.println(msg);
    }
    return new TestSuite( FireREST_Test.class );
  }

  public void testCalcOffset_model() {
    File file  = new File("src/test/resources/calcOffset-model.json");
    JSONResult result = new FireREST().getJSON(file);

    JSONResult stage = result.get("calcOffset-stage");
    JSONResult channel = stage.get("channels").get("0");
    assertEquals((Integer) 14, channel.get("dx").getInt());
    assertEquals((Integer) 0, channel.get("dy").getInt());
    assertEquals("0.978238", channel.get("match").getString());
    assertEquals(0.978238d, channel.get("match").getDouble(), 0);

    assertEquals((Integer) 400, stage.get("rects").get(0).get("x").getInt());
    assertEquals((Integer) 736, stage.get("rects").get(1).get("width").getInt());
  }

  public void testCalcOffset_notfound() {
    File file  = new File("src/test/resources/calcOffset-notfound.json");
    JSONResult result = new FireREST().getJSON(file);

    JSONResult channel = result.get("calcOffset-stage").get("channels").get("0");
    assertTrue(channel.isNull());
  }

  public void testProcessJson() throws MalformedURLException {
    URL processUrl = new URL("http://localhost:8080/firerest/cv/1/gray/cve/calc-offset/process.json");
    JSONResult result = new FireREST().getJSON(processUrl);

    JSONResult stage = result.get("model");
    JSONResult channel = stage.get("channels").get("0");
    assertEquals((Integer) 0, channel.get("dx").getInt());
    assertEquals((Integer) 0, channel.get("dy").getInt());
    assertEquals("0.997476", channel.get("match").getString());
    assertEquals((Double)0.997476d, channel.get("match").getDouble(), 0);

    assertEquals((Integer)400, stage.get("rects").get(0).get("x").getInt());
    assertEquals((Integer)164, stage.get("rects").get(1).get("width").getInt());
  }

  public void testBadUrl() throws MalformedURLException {
    Exception caughtException = null;
    try {
      URL processUrl = new URL("http://localhost:8080/firerest/cv/1/gray/cve/NOSUCHTHING/process.json");
      JSONResult result = new FireREST().getJSON(processUrl);
    } catch (Exception e) {
      System.out.println("CAUGHT EXPECTED EXCEPTION: " + e.getMessage());
      caughtException = e;
    }

    assertNotNull(caughtException);
  }

}
