package com.oxit.flow.logistique.as400.mapping.route;

import org.apache.camel.builder.RouteBuilder;

/**
 * Route Jms to log.
 *
 * @author aidrissi
 * @since 13/01/18.
 */
public class DlqRoute extends RouteBuilder {
	
	private static final String DLQ_ROUTE = "direct:sendToDLQ";
	
  @Override
  public void configure() throws Exception {
	  from(DLQ_ROUTE)
	  	.routeId("DLQ")
	  	.toD("jms:${header.DLQEndpoint}");
  }
}
