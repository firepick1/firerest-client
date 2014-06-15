package org.firepick;

import java.util.*;
import java.io.*;
import java.net.*;
import java.awt.image.BufferedImage;
import java.awt.*;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.Charsets;

/**
 * FireREST Java client. Fluent API for traversing JSON responses from FireREST web services.
 */
public class FireREST {
  static Logger logger = LoggerFactory.getLogger(FireREST.class);
  private int imageWidth = 800;
  private int imageHeight = 200;

  private int msTimeout = 500;
  private HashMap<URL,LIFOCache<BufferedImage>> imageMap = new HashMap<URL,LIFOCache<BufferedImage>>();

  public FireREST() {}

  private FireREST(FireREST firerest, int msTimeout) {
    this.msTimeout = msTimeout;
  }

  public FireREST withTimeout(int msTimeout) {
    return new FireREST(this, msTimeout);
  }

  public Point getImageSize() {
    return new Point(imageWidth, imageHeight);
  }

  public BufferedImage errorImage(String... lines) {
    BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = (Graphics2D) image.getGraphics();
    g.setBackground(new Color(64,32,32));
    g.setColor(new Color(255,64,64));
    g.clearRect(0, 0, imageWidth, imageHeight);
    int maxLen = 0;
    for (String line: lines) {
      for (String innerLine: line.split("\n")) {
	maxLen = Math.max(innerLine.length(), maxLen);
      }
    }
    int padding = 20;
    float sizeForWidth = 1.8f*(imageWidth-padding-padding)/maxLen; // should use TextLayout
    System.out.println("sizeForWidth:" + sizeForWidth);
    float sizeForHeight = (imageHeight-padding-padding)/lines.length;
    System.out.println("sizeForHeight:" + sizeForHeight);
    float lineHeight = Math.min(80, Math.max(12, Math.min(sizeForWidth, sizeForHeight)));
    float fontSize = 0.8f * lineHeight;
    Font font = g.getFont().deriveFont(fontSize);
    g.setFont(font);
    float y = fontSize + padding;
    for (String line: lines) {
      g.drawString(line, padding, y);
      y += lineHeight;
    }
    return image;
  }

  public BufferedImage getImage(URL url) {
    try {
      BufferedImage image =  ImageIO.read(url);
      imageWidth = image.getWidth();
      imageHeight = image.getHeight();
      return image;
    } catch (Exception e) {
      logger.warn("getImage({}) => {}", url, e.getMessage());
      return errorImage(new Date().toString(), "(No image)", url.toString(), e.getMessage());
    }
  }

  /**
   * Load json from given file resource.
   * This is a convenient equivalent to getJSON() with a file URL.
   *
   * @return JSONResult 
   */
  public JSONResult getJSON(File file) {
    try {
      String json = new Scanner(file).useDelimiter("\\Z").next();
      return new JSONResult(json);
    } catch (Throwable e) {
      throw new FireRESTException(file.toString(), e);
    }
  }

  /**
   * HTTP GET json from given URL resource.
   *
   * @return JSONResult 
   */
  public JSONResult getJSON(URL url) {
    try {
      logger.debug("Requesting {}", url);
      StringBuilder text = new StringBuilder();
      String line;

      HttpURLConnection urlconn = (HttpURLConnection)url.openConnection();
      urlconn.setReadTimeout(msTimeout);
      urlconn.setConnectTimeout(msTimeout);
      urlconn.setRequestMethod("GET");
      urlconn.connect();
      BufferedReader br = new BufferedReader(new InputStreamReader(urlconn.getInputStream())); 
      while((line = br.readLine()) != null) {
	text.append(line);
      }
      return new JSONResult(text.toString());
    } catch (Throwable e) {
      throw new FireRESTException(url.toString(), e);
    }
  }

}
