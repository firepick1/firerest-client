package org.firepick;

import java.net.*;
import java.util.*;


/**
 * FireREST service locator discovers FireREST services on local network
 */
public class ServiceResolver {
  private InetAddress address;
  private URL url;
  private int attempts;
  private JSONResult config;
  
  public ServiceResolver(URL url) {
    if (url == null) {
      throw new NullPointerException("url cannot be null");
    }
    this.url = url;
  }

  public ServiceResolver(InetAddress address) {
    if (address == null) {
      throw new NullPointerException("address cannot be null");
    }
    this.address = address;
  }

  public URL getURL() {
    return url;
  }

  /**
   * (Re-)discover the service configuration
   *
   * @return true if config.json was successfully obtained
   */
  public void resolve() {
    attempts++;
    config = null;
    Exception caughtException = null;
    String host = address == null ? "null" : address.getCanonicalHostName();
    if (url == null) {
      URL attemptUrl = null;
      int [] ports = {8080, 80};
      for (int i=0; i < ports.length; i++) {
	try {
	  attemptUrl = new URL("http", host, ports[i], "/firerest/config.json");
	  break;
	} catch (Exception e) {
	  attemptUrl = null;
	  caughtException = e;
	}
      }
      if (attemptUrl == null) {
	throw new FireRESTException("Could not resolve service at " + host, caughtException);
      }
      url = attemptUrl;
    }

    if (config == null) {
      config = FireREST.getJSON(url);
    }
  }

  /** 
   * Return number of attempts to resolve() service.
   */
  public int getAttempts() {
    return attempts;
  }

  public InetAddress getAddress() {
    return address;
  }

  /**
   * Return the cached service configuration, resolving the service if required.
   * 
   * @return service configuration or null if service could not be resolved.
   */
  public JSONResult getConfig() {
    if (attempts == 0) {
      try {
	resolve();
      } catch(Exception e) {
        // discard exception
      }
    }
    return config;
  }

}
