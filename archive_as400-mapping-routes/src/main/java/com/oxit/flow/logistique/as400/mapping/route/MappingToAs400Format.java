package com.oxit.flow.logistique.as400.mapping.route;

import org.apache.camel.builder.RouteBuilder;

import javax.xml.bind.JAXBContext;

import org.apache.camel.BeanInject;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.converter.jaxb.JaxbDataFormat;

import com.oxit.flow.message.Message;

import lombok.Data;

/**
 * Route Jms to log.
 *
 * @author aidrissi
 * @since 13/01/18.
 */
@Data
public class MappingToAs400Format extends RouteBuilder {
	
	@BeanInject("message")
    private Message message;
	
	private String inputQueue;
	
	private String outputQueue;
	
	private String formatClass;
	
	private String fileName;
	
	private static final String DLQ_ROUTE = "direct:sendToDLQ";
	    
	private static final String TRACE_ROUTE = "direct-vm:traceMessage";
	
  @Override
  public void configure() throws Exception {

    JaxbDataFormat xmlDataFormat = new JaxbDataFormat();
    JAXBContext con = JAXBContext.newInstance(Class.forName(formatClass));
    xmlDataFormat.setContext(con);
    
    onException(Exception.class)
	    .handled(true)
	    .maximumRedeliveries(0)
	    .log(LoggingLevel.ERROR, "${header.processTypeName} ${header.documentID} unexpected error ${exception}")
	    .wireTap(TRACE_ROUTE)
	    .wireTap(DLQ_ROUTE);

    from("jms:queue:"+inputQueue)
	    .routeId("Mapping.ToAS400."+outputQueue )
		.setHeader("DLQEndpoint", simple("DLQ." + inputQueue))
		.bean(message, "init(${exchange})") // to update service name
		.to("xslt://removeNS.xslt")
		.unmarshal(xmlDataFormat)
		.setHeader("FileName",constant(fileName))
		.process(new Processor() {
			public void process(Exchange exchange) throws Exception {
				Object myXml= Class.forName(formatClass).cast(exchange.getIn().getBody());
				exchange.getIn().setBody(myXml.toString(), String.class);
			}
		})
		.to("jms:queue:"+outputQueue)
		.wireTap(TRACE_ROUTE);
  }
}
