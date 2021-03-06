package org.firepick;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;

/**
 * FireREST Java client. Fluent API for traversing JSON responses from FireREST web services.
 */
public class FireREST {
  static Logger logger = LoggerFactory.getLogger(FireREST.class);
  private int imageWidth = 800;
  private int imageHeight = 200;
  private BufferedImage imageBuffer;
  private int msTimeout = 500;
  private HashMap<URL, LIFOCache<BufferedImage>> imageMap = new HashMap<URL, LIFOCache<BufferedImage>>();

  public FireREST() {
  }

  private FireREST(FireREST firerest, int msTimeout) {
    this.msTimeout = msTimeout;
  }

  public FireREST withTimeout(int msTimeout) {
    return new FireREST(this, msTimeout);
  }

  /**
   * Return image dimensions of last retrieved image
   *
   * @return Point(width, height)
   */
  public Point getImageSize() {
    return new Point(imageWidth, imageHeight);
  }

  /**
   * Return an red image with the given text auto-sized to fit the current imageWidthximageHeight
   *
   * @param lines one or more lines of text
   * @return image
   */
  public BufferedImage errorImage(String... lines) {
    if (imageBuffer == null || imageBuffer.getWidth() != imageWidth || imageBuffer.getHeight() != imageHeight) {
      imageBuffer = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
    }
    Graphics2D g = (Graphics2D) imageBuffer.getGraphics();
    g.setBackground(new Color(64, 32, 32));
    g.setColor(new Color(255, 64, 64));
    g.clearRect(0, 0, imageWidth, imageHeight);
    int maxLen = 0;
    for (String line : lines) {
      if (line != null) {
        for (String innerLine : line.split("\n")) {
          maxLen = Math.max(innerLine.length(), maxLen);
        }
      }
    }
    int padding = 20;
    float sizeForWidth = 1.8f * (imageWidth - padding - padding) / maxLen; // should use TextLayout
    float sizeForHeight = (imageHeight - padding - padding) / lines.length;
    float lineHeight = Math.min(80, Math.max(12, Math.min(sizeForWidth, sizeForHeight)));
    float fontSize = 0.8f * lineHeight;
    Font font = g.getFont().deriveFont(fontSize);
    g.setFont(font);
    float y = fontSize + padding;
    for (String line : lines) {
      if (line != null) {
        g.drawString(line, padding, y);
        y += lineHeight;
      }
    }
    return imageBuffer;
  }

  /**
   * Return image from given URL.
   *
   * @return image from url or image with error text
   */
  public BufferedImage getImage(URL url) {
    String now = new Date().toString();
    if (url == null) {
      return errorImage(now, "(No image url)");
    }
    try {
      HttpURLConnection urlconn = (HttpURLConnection) url.openConnection();
      urlconn.setReadTimeout(msTimeout);
      urlconn.setConnectTimeout(msTimeout);
      urlconn.setRequestMethod("GET");
      urlconn.connect();
      BufferedImage image = ImageIO.read(urlconn.getInputStream());
      if (image == null) {
        return errorImage(now, "(Null image read)");
      }
      imageWidth = image.getWidth();
      imageHeight = image.getHeight();
      return image;
    } catch (SocketTimeoutException e) {
      logger.warn("getImage({}) => {} {}", url, e.getClass().getCanonicalName(), e.getMessage());
      return errorImage(now, msTimeout+"ms TIMEOUT");
    }  catch (Exception e) {
      logger.warn("getImage({}) => {} {}", url, e.getClass().getCanonicalName(), e.getMessage());
      return errorImage(now, "(No image)", url.toString(), e.getMessage());
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
    }
    catch (Throwable e) {
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

      HttpURLConnection urlconn = (HttpURLConnection) url.openConnection();
      urlconn.setReadTimeout(msTimeout);
      urlconn.setConnectTimeout(msTimeout);
      urlconn.setRequestMethod("GET");
      urlconn.connect();
      BufferedReader br = new BufferedReader(new InputStreamReader(urlconn.getInputStream()));
      while ((line = br.readLine()) != null) {
        text.append(line);
      }
      return new JSONResult(text.toString());
    }
    catch (Throwable e) {
      throw new FireRESTException(url.toString(), e);
    }
  }

}
