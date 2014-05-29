package org.firepick;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unchecked exception wrapper for FireREST
 */
public class FireRESTException extends RuntimeException {
  static Logger logger = LoggerFactory.getLogger(FireRESTException.class);

  public FireRESTException(Throwable e) {
    super(e);
    logger.info("{}", e.getMessage());
  }

  public FireRESTException(String msg) {
    super(msg);
    logger.info("{}", msg);
  }
 
  public FireRESTException(String msg, Throwable e) {
    super(msg, e);
    logger.info("{} {}", msg, e.getMessage());
  }
}
