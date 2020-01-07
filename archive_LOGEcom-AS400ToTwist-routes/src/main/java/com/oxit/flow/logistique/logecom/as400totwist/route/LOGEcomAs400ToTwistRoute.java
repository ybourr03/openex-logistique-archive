package com.oxit.flow.logistique.logecom.as400totwist.route;

import com.google.common.collect.Iterables;
import com.oxit.flow.logistique.logecom.as400totwist.service.As400ToTwistConfigService;
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

public class LOGEcomAs400ToTwistRoute extends RouteBuilder {
    private static final String TRACE_ROUTE = "direct-vm:traceMessage";

    private As400ToTwistConfigService as400ToTwistConfigService;
    private List<String> hostsAlreadyAdded = new ArrayList<>();

	@BeanInject("message")
    private Message message;

    @BeanInject("blueprintBundleContext")
    private BundleContext bundleContext;

    @PropertyInject("logecom.to.twist.enable")
    private String flowsEnabled;

    @Override
    public void configure() {

        if (!"true".equalsIgnoreCase(flowsEnabled)) {
            return;
        }

        addExceptionClauses();

        as400ToTwistConfigService.getAs400ToTwistConfiguration().forEach((k,as400ToTwistConf) -> {
            Set<String> as400Hosts = findHostsForAliases(as400ToTwistConf.getListAS400());
            initJmsComponentsForHosts(as400Hosts);
            String inputQueue = as400ToTwistConf.getInputQueue();
            for (String as400Host : as400Hosts) {
                from("as400-" + as400Host + ":" + inputQueue)
                        .routeId(inputQueue + "-from-" + as400Host + "-to-" + as400ToTwistConf.getName())
                        .setHeader("DLQEndpoint", constant("DLQ." + inputQueue))
                        .bean(message, "init(${exchange})")
                        .to("jms:" + as400ToTwistConf.getQueue())
                        .wireTap(TRACE_ROUTE);
            }
        });
    }

        private void addExceptionClauses() {
            onException(Exception.class)
                    .handled(true)
                    .log(LoggingLevel.ERROR, "Unable to retrieve the message ${exception}")
                    .wireTap(TRACE_ROUTE)
                    .wireTap("jms:${header.DLQEndpoint}");
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
        try {
            Collection<ServiceReference<JmsComponent>> refs = bundleContext.getServiceReferences(JmsComponent.class, osgiFilter);
            if (refs != null) {
                for (ServiceReference<JmsComponent> jmsComponentRef : refs) {
                    String host = (String) jmsComponentRef.getProperty("as400.host");
                    if (hostList.contains(host)
                            && !hostsAlreadyAdded.contains(host)
                            && getContext().getComponent("as400-" + host, false, false) == null)
                    {
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
    
    public As400ToTwistConfigService getAs400ToTwistConfigService() {
		return as400ToTwistConfigService;
	}

	public void setAs400ToTwistConfigService(As400ToTwistConfigService as400ToTwistConfigService) {
		this.as400ToTwistConfigService = as400ToTwistConfigService;
	}


}

