package org.firepick;

import java.util.Scanner;
import java.io.*;
import java.net.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.Charsets;

/**
 * FireREST Java client. Fluent API for traversing JSON responses from FireREST web services.
 */
public class FireREST {
  static Logger logger = LoggerFactory.getLogger(FireREST.class);

  private int msTimeout = 500;

  public FireREST() {}

  private FireREST(FireREST firerest, int msTimeout) {
    this.msTimeout = msTimeout;
  }

  public FireREST withTimeout(int msTimeout) {
    return new FireREST(this, msTimeout);
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
