package org.firepick;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

// The following imports are required to run test content
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.image.BufferedImage;  
import java.awt.*;
import javax.imageio.ImageIO;

public class TestFireREST extends TestCase {
  /**
   * Create the test case
   *
   * @param testName name of the test case
   */
  public TestFireREST( String testName ) {
    super( testName );
  }

  /**
   * @return the suite of tests being tested
   */
  public static Test suite() throws Exception {
    Exception caughtException = null;
    JSONResult json = new JSONResult(null);
    try {
      URL url = new URL("http://localhost:8080/firerest/config.json");
      json = new FireREST().getJSON(url);
    } catch(Exception e) {
      caughtException = e;
      e.printStackTrace();
    }
    if (caughtException != null || json.isNull()) {
      StringBuilder msg = new StringBuilder();
      msg.append("\n");
      msg.append("----------------ERROR---------------\n");
      msg.append("| Test requires FireREST server.   |\n");
      msg.append("| Launch the localhost server:     |\n");
      msg.append("|   node server/firerest.js        |\n");
      msg.append("------------------------------------\n");
      System.out.println(msg);
    }
    return new TestSuite( TestFireREST.class );
  }

  public void testURL() throws Exception {
    HashMap<URL,String> map = new HashMap<URL,String>();
    URL url1 = new URL("http://www.firepick.org/");
    URL url2 = new URL("http://www.firepick.org/");
    assert(url1 != url2);
    assertEquals(url1, url2);
    map.put(url1, "hello");
    assertEquals("hello", map.get(url2));
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

  public void testConfigJson() throws MalformedURLException {
    File file  = new File("src/test/resources/config.json");
    JSONResult result = new FireREST().getJSON(file);
    assertNotNull(result);
    JSONResult one1 = result.get("cv").get("camera_map").get(1);
    JSONResult one = result.get("cv").get("camera_map").get("1");
    assertNotNull(one);
    assertEquals(one, one1);
    JSONResult profileMap = one.get("profile_map");
    assertNotNull(profileMap);
  }

  public void testProcessJson() throws MalformedURLException {
    URL processUrl = new URL("http://localhost:8080/firerest/cv/1/gray/cve/calc-offset/process.fire");
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

  public void testNoImage() throws Exception {
    FireREST firerest = new FireREST();
    BufferedImage image = firerest.errorImage("(No Image)", "FireREST");
    ImageIO.write(image, "jpg", new File("target/noimage.jpg"));
  }

  public void test_getImage() throws Exception {
    FireREST firerest = new FireREST();
    BufferedImage imageGood = firerest.getImage(new URL("http://firepick1.github.io/firerest/cv/1/monitor.jpg"));
    ImageIO.write(imageGood, "jpg", new File("target/image-ok.jpg"));
    BufferedImage imageBad = firerest.getImage(new URL("http://firepick:8080/firerest/cv/1/badimage.jpg"));
    ImageIO.write(imageBad, "jpg", new File("target/image-bad.jpg"));
    BufferedImage imageNull = firerest.getImage(null);
    ImageIO.write(imageBad, "jpg", new File("target/image-null.jpg"));
  }

  public void testBadUrl() throws MalformedURLException {
    Exception caughtException = null;
    try {
      URL processUrl = new URL("http://localhost:8080/firerest/cv/1/gray/cve/NOSUCHTHING/process.fire");
      JSONResult result = new FireREST().getJSON(processUrl);
    } catch (Exception e) {
      System.out.println("CAUGHT EXPECTED EXCEPTION: " + e.getMessage());
      caughtException = e;
    }

    assertNotNull(caughtException);
  }

  public void testJSONResult() {
    assertEquals("{}", new JSONResult("{}"), "{}");
    assertEquals(new JSONResult("{}"), new JSONResult("{}"));
    assertEquals("{\"1\":2}", new JSONResult("{\"1\":2}").toString());
    assertEquals("2", new JSONResult("{\"1\":2}").get("1").toString());
    assertEquals("2", new JSONResult("{\"1\":2}").get("1").getString());
    assertEquals("2", new JSONResult("{\"1\":2}").get(1).getString());
    assertEquals("null", new JSONResult(null).toString());
    assertEquals(null, new JSONResult(null).getString());
  }

}
