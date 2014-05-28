package org.firepick;

import java.net.*;

/**
 * ServiceDirectory Java client. 
 */
public class ServiceDirectory {

	public static List<ServiceResolver> discover(int msTimeout) {
		List<ServiceResolver> result = new ArrayList<ServiceResolver>();
		List<InetAddress> hosts = IPv4Scanner.scanLocal256(msTimeout);
		for (InetAddress host: hosts) {
			ServiceResolver resolver = new ServiceResolver(host);
			if (resolver.resolve()) {
				 result.add(resolver);
			}
		}

		return result;
	}

}
