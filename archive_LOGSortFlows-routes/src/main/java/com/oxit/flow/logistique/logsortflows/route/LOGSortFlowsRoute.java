package com.oxit.flow.logistique.logsortflows.route;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.camel.BeanInject;
import org.apache.camel.Component;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.google.common.collect.Iterables;
import com.oxit.flow.message.Message;
import com.oxit.flow.message.MessageHeaders;

/**
 * @author Yassine
 *
 */
public class LOGSortFlowsRoute extends RouteBuilder {

    private static final String TRACE_ROUTE = "direct-vm:traceMessage";
    private static final String DLQ_ROUTE = "direct:sendToDLQ";

    @BeanInject("message")
    private Message message;
    
    private List<String> as400Receivers = new ArrayList<>();
    
    @BeanInject("blueprintBundleContext")
    private BundleContext bundleContext;
    
    private String as400MQReceivers;
    
    private String flowName;

    private String jmsQueueName;
    
    private String jmsOutputQueueName;

    private String jmsConcurrentConsumers;

    private String jmsMaxConcurrentConsumers;

    @Override
    public void configure() {
    	
    	if(!as400MQReceivers.isEmpty()) {
    	  as400Receivers =  Arrays.asList(as400MQReceivers.split(";"));
    	  for (String as400Alias : as400Receivers) {
              initJmsComponent("as400-" + as400Alias);
          }
    	}

    	onException(Exception.class)
            .handled(true)
            .maximumRedeliveries(2)
            .log(LoggingLevel.ERROR, "Unable to send the message ${exception}")
            .wireTap(TRACE_ROUTE)
            .wireTap(DLQ_ROUTE);

        from("jms:queue:" + jmsQueueName)
            .routeId(flowName)
            .bean(message, "init(${exchange})")
            .setHeader("DLQEndpoint", simple("DLQ." + jmsQueueName))

            .loop(as400Receivers.size())
                .process(exchange -> {
                    String warehouseId = as400Receivers.get(simple("${header.CamelLoopIndex}").evaluate(exchange, Integer.class));
                    exchange.getIn().setHeader(MessageHeaders.RECEIVER_ID, warehouseId);
                    exchange.getIn().setHeader("warehouseID", warehouseId);

                    exchange.getIn().setHeader("receiverQueue", jmsQueueName + "." + warehouseId);
                    exchange.getIn().setHeader("DLQEndpoint", "DLQ." + jmsQueueName + "." + warehouseId);
                })
                .toD("jms:queue:${header.receiverQueue}");
        
        for (String receiver : as400Receivers) {
        	from("jms:queue:" + jmsQueueName + "." + receiver)
        	    .routeId(flowName+"-to-AS400-" + receiver)
        	    .toD("as400-${header.receiverID}:" + jmsOutputQueueName)
                .wireTap(TRACE_ROUTE);
        }
                
        from(DLQ_ROUTE)
            .routeId("DLQ")
            .toD("jms:${header.DLQEndpoint}");
    }
    
    /**
     * Add the camel-component (available in the OSGI registry) in the local available components
     * @param jmsComponentName the Camel component name in the OSGI registry (ie `as400-Lxx`).
     * @throws IllegalArgumentException if the service is not found in the registry
     */
    private void initJmsComponent(String jmsComponentName) {
        Component jmsComponent = getContext().getComponent(jmsComponentName);

        if (jmsComponent == null) {
            String osgiFilter = "(osgi.jndi.service.name=camel-component/" + jmsComponentName + ")";

            try {
                Collection<ServiceReference<JmsComponent>> refs = bundleContext.getServiceReferences(JmsComponent.class, osgiFilter);
                ServiceReference<JmsComponent> serviceReference = Iterables.getFirst(refs, null);
                if (serviceReference == null) {
                    throw new IllegalArgumentException("Service not found in registry with filter " + osgiFilter);
                }
                jmsComponent = bundleContext.getService(serviceReference);
                getContext().addComponent(jmsComponentName, jmsComponent);
            } catch (InvalidSyntaxException ex) {
                log.error("Error in syntax when searching for {}", osgiFilter, ex);
            }
        }
    }

	public String getFlowName() {
		return flowName;
	}

	public void setFlowName(String flowName) {
		this.flowName = flowName;
	}

	public String getJmsConcurrentConsumers() {
		return jmsConcurrentConsumers;
	}

	public void setJmsConcurrentConsumers(String jmsConcurrentConsumers) {
		this.jmsConcurrentConsumers = jmsConcurrentConsumers;
	}

	public String getJmsMaxConcurrentConsumers() {
		return jmsMaxConcurrentConsumers;
	}

	public void setJmsMaxConcurrentConsumers(String jmsMaxConcurrentConsumers) {
		this.jmsMaxConcurrentConsumers = jmsMaxConcurrentConsumers;
	}

	public String getJmsQueueName() {
		return jmsQueueName;
	}

	public void setJmsQueueName(String jmsQueueName) {
		this.jmsQueueName = jmsQueueName;
	}

	public String getJmsOutputQueueName() {
		return jmsOutputQueueName;
	}

	public void setJmsOutputQueueName(String jmsOutputQueueName) {
		this.jmsOutputQueueName = jmsOutputQueueName;
	}

	public String getAs400MQReceivers() {
		return as400MQReceivers;
	}

	public void setAs400MQReceivers(String as400mqReceivers) {
		as400MQReceivers = as400mqReceivers;
	}
	
}
