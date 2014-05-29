package org.firepick;

import java.net.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FireREST service locator discovers FireREST services on local network
 */
public class ServiceResolver {
  static Logger logger = LoggerFactory.getLogger(ServiceResolver.class);
  private InetAddress address;
  private URL url;
  private int attempts;
  private JSONResult config;
  private int msTimeout = 500;
  
  private ServiceResolver(ServiceResolver resolver, int msTimeout) {
    this.url = resolver.url;
    this.address = resolver.address;
    this.msTimeout = msTimeout;
  }

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

  public ServiceResolver withTimeout(int msTimeout) {
    return new ServiceResolver(this, msTimeout);
  }

  /**
   * Discover FireREST services located in a range of IPv4 InetAddresses.
   * E.g., The range of 256 addresses that starts with 10.0.1.128 ends with 10.0.2.127.
   *
   * @param start first InetAddress to scan (null for localhost)
   * @param count number of subsequent InetAddresses in the range
   * @param msTimeout maximum time to wait for host response
   * @return ServiceResolvers that resolve to a FireREST service.
   */
  public static Collection<ServiceResolver> discover(InetAddress start, int count, int msTimeout) {
    Collection<ServiceResolver> result = new ArrayList<ServiceResolver>();
    Collection<InetAddress> hosts = IPv4Scanner.scanRange(start, count, msTimeout);
    for (InetAddress host: hosts) {
      ServiceResolver resolver = new ServiceResolver(host);
      logger.info("resolving {} {}", host.getHostAddress(), host.getCanonicalHostName());
      JSONResult config = resolver.getConfig();
      if (config != null) {
         result.add(resolver);
      }
    }

    return result;
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
      logger.info("Resolving {}", url);
      config = new FireREST().withTimeout(msTimeout).getJSON(url);
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
    if (config == null) {
      logger.info("{} => no response", url);
      return null;
    }

    logger.info("{} => {}", url, config.get("FireREST").getString());
    return config;
  }

}
