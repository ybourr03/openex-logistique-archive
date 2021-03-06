package com.oxit.flow.logistique.items;

import com.google.common.collect.Iterables;
import com.oxit.flow.message.Message;
import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.util.*;
import java.util.stream.Collectors;

public class LOGEcomItemsRoute extends RouteBuilder {

    private static final String TRACE_ROUTE = "direct-vm:traceMessage";

    @BeanInject("message")
    private Message message;

    @PropertyInject("jms.input.queue")
    private String inputQueue;

    @PropertyInject("jms.output.queue")
    private String outputQueue;

    @PropertyInject("as400.alias.list")
    private String as400ListString;

    @BeanInject("blueprintBundleContext")
    private BundleContext bundleContext;

    @Override
    public void configure() {

        addExceptionClauses();

        Set<String> as400Hosts = findHostsForAliases(Arrays.asList(as400ListString.split(";")));
        initJmsComponentsForHosts(as400Hosts);
        for (String as400Host : as400Hosts) {
            from("as400-" + as400Host + ":" + inputQueue)
                .routeId("EcomItems-from-" + as400Host)
                .bean(message, "init(${exchange})")
                .to("jms:" + outputQueue)
                .wireTap(TRACE_ROUTE);
        }
    }

    private void addExceptionClauses() {
        onException(Exception.class)
            .handled(true)
            .log(LoggingLevel.ERROR, "Unable to retrieve the message ${exception}")
            .wireTap(TRACE_ROUTE)
            .wireTap("jms:queue:DLQ." + outputQueue);

    }

    /**
     * A same host can be used for multiple aliases. We only need one connection per host.
     */
    private Set<String> findHostsForAliases(List<String> aliasNames) {
        String osgiFilter = "(osgi.jndi.service.name=as400/aliasHostMap)";

        try {
            Collection<ServiceReference<Map>> refs = bundleContext.getServiceReferences(Map.class, osgiFilter);
            ServiceReference<Map> serviceReference = Iterables.getFirst(refs, null);
            if (serviceReference == null) {
                throw new IllegalArgumentException("Service not found in registry with filter " + osgiFilter);
            }
            Map<String, String> aliasHostMap = bundleContext.getService(serviceReference);

            return aliasHostMap.entrySet()
                    .stream()
                    .filter(entry -> aliasNames.contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toSet());
        } catch (InvalidSyntaxException ex) {
            log.error("Error in syntax when searching for {}", osgiFilter, ex);
            return new HashSet<>();
        }
    }

    /**
     * Add in camel context the JMS components for the host list.
     * The JMS components will be registered as « as400-HOSTNAME ».
     */
    private void initJmsComponentsForHosts(Set<String> hostList) {
        String osgiFilter = "(osgi.jndi.service.name=camel-component/as400-*)";
        List<String> hostsAlreadyAdded = new ArrayList<>();
        try {
            Collection<ServiceReference<JmsComponent>> refs = bundleContext.getServiceReferences(JmsComponent.class, osgiFilter);
            if (refs != null) {
                for (ServiceReference<JmsComponent> jmsComponentRef : refs) {
                    String host = (String) jmsComponentRef.getProperty("as400.host");
                    if (hostList.contains(host) && !hostsAlreadyAdded.contains(host)) {
                        hostsAlreadyAdded.add(host);
                        JmsComponent jmsComponent = bundleContext.getService(jmsComponentRef);
                        getContext().addComponent("as400-" + host, jmsComponent);
                    }
                }
            }
        } catch (InvalidSyntaxException ex) {
            log.error("Error in syntax when searching for {}", osgiFilter, ex);
        }

    }

}
