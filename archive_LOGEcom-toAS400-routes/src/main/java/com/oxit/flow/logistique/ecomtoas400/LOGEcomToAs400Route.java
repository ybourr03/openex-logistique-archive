package com.oxit.flow.logistique.ecomtoas400;

import com.google.common.collect.Iterables;
import com.oxit.flow.logistique.ecomtoas400.exception.InvalidReceiverIdException;
import com.oxit.flow.message.Message;
import com.oxit.flow.message.MessageHeaders;
import org.apache.camel.BeanInject;
import org.apache.camel.Component;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class LOGEcomToAs400Route extends RouteBuilder {
    private static final String TRACE_ROUTE = "direct-vm:traceMessage";
    protected static final String WAREHOUSE_HEADER = "warehouseID";

    @BeanInject("message")
    private Message message;

    @PropertyInject("as400.alias.list")
    private String as400ListString;

    @BeanInject("blueprintBundleContext")
    private BundleContext bundleContext;

    private String enabled;
    private String flowName;
    private String inputQueue;
    private String outputQueue;

    public LOGEcomToAs400Route(String enabled, String flowName, String inputQueue, String outputQueue) {
        super();
        this.enabled = enabled;
        this.flowName = flowName;
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
    }

    @Override
    public void configure() {

        if (!"true".equalsIgnoreCase(enabled)) {
            return;
        }

        List<String> as400List = Arrays.asList(as400ListString.split(";"));
        for (String as400Alias : as400List) {
            initJmsComponent("as400-" + as400Alias);
        }

        addExceptionClauses();

        from("jms:queue:" + inputQueue)
                .routeId(flowName + "-to-AS400")
                .bean(message, "init(${exchange})")
                .process((exchange -> {
                    String receiver = exchange.getIn().getHeader(WAREHOUSE_HEADER, String.class);
                    if (!as400List.contains(receiver)) {
                        throw new InvalidReceiverIdException(receiver + " is not in the available AS400 list");
                    }

                    // set the receiver for GoldenEyes
                    exchange.getIn().setHeader(MessageHeaders.RECEIVER_ID, receiver);
                }))
                .toD("as400-${header.warehouseID}:" + outputQueue)
                .wireTap(TRACE_ROUTE);
    }

    private void addExceptionClauses() {
        onException(InvalidReceiverIdException.class)
                .handled(true)
                .maximumRedeliveries(0)
                .log(LoggingLevel.ERROR, "Dropping message because ${exception.message}")
                .wireTap(TRACE_ROUTE);

        onException(Exception.class)
                .handled(true)
                .maximumRedeliveries(2)
                .log(LoggingLevel.ERROR, "Unable to send the message ${exception}")
                .wireTap(TRACE_ROUTE)
                .wireTap("jms:queue:DLQ." + inputQueue);

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
}
