package com.oxit.flow.logistique.logsortflows;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.oxit.flow.message.MessageHeaders;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.Exchange;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.junit.Before;
import org.junit.Test;

import javax.jms.ConnectionFactory;
import java.util.*;

public class LOGSortFlowsTest extends CamelBlueprintTestSupport {

    private static final String OSGI_JNDI_SERVICE_NAME = "osgi.jndi.service.name";

    @PropertyInject("logSortGroupOfStores.as400.receivers")
    private String as400MQReceiversGroupOfStores;

    @Before
    public void adviceRoutes() throws Exception {
        for (String receiver : as400MQReceiversGroupOfStores.split(";")) {
            adviceRoute("LOGSortGroupOfStores", receiver);
        }
    }

    public void adviceRoute(String flowName, String receiver) throws Exception {
        context.getRouteDefinition(flowName + "-to-AS400-" + receiver).adviceWith(context, new RouteBuilder() {
            @Override
            public void configure() {
                interceptSendToEndpoint("as400-*")
                        .skipSendToOriginalEndpoint()
                        .to("mock:as400Endpoint");

                interceptSendToEndpoint("direct-vm:traceMessage")
                        .skipSendToOriginalEndpoint()
                        .process(exchange -> {
                            if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
                                exchange.getIn().setHeader(MessageHeaders.STATUS, "KO");
                            } else {
                                exchange.getIn().setHeader(MessageHeaders.STATUS, "OK");
                            }
                        })
                        .to("mock:traceMessage");
            }
        });
    }

    @Test
    public void testGroupOfStores() throws InterruptedException {
        // Only test one of the 3 flows, it's the same code and configuration for the 3

        int numberOfWarehouse = as400MQReceiversGroupOfStores.split(";").length;

        MockEndpoint mockTraceMessage = (MockEndpoint) this.context.getEndpoint("mock:traceMessage");
        mockTraceMessage.expectedMessageCount(numberOfWarehouse);
        mockTraceMessage.expectedHeaderReceived(MessageHeaders.STATUS, "OK");

        MockEndpoint mockAS400Endpoint = (MockEndpoint) this.context.getEndpoint("mock:as400Endpoint");
        mockAS400Endpoint.expectedMessageCount(numberOfWarehouse);

        Map<String, Object> headers = new HashMap<>();
        headers.put("processTypeName", "LOGSortGroupOfStores");
        template.sendBodyAndHeaders("jms:queue:LOGSortGroupOfStores", "jms2api message GroupOfStores", headers);

        assertMockEndpointsSatisfied();
    }


    @Override
    protected void addServicesOnStartup(List<KeyValueHolder<String, KeyValueHolder<Object, Dictionary>>> services) {
        super.addServicesOnStartup(services);
        Dictionary<String, String> brokerTraceJndi = new Hashtable<>();
        brokerTraceJndi.put(OSGI_JNDI_SERVICE_NAME, "jms/broker-traces");

        Dictionary<String, String> brokerLocalJndi = new Hashtable<>();
        brokerLocalJndi.put(OSGI_JNDI_SERVICE_NAME, "jms/broker-local");

        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");

        services.add(asKeyValueService(ConnectionFactory.class.getName(), connectionFactory, brokerTraceJndi));
        services.add(asKeyValueService(ConnectionFactory.class.getName(), connectionFactory, brokerLocalJndi));

        addAs400CFServices(services);
    }

    private void addAs400CFServices(List<KeyValueHolder<String, KeyValueHolder<Object, Dictionary>>> services) {
        MQQueueConnectionFactory mqConnectionFactory = new MQQueueConnectionFactory();
        JmsConfiguration jmsConfiguration = new JmsConfiguration();
        jmsConfiguration.setConnectionFactory(mqConnectionFactory);

        JmsComponent jmsCompnent = new JmsComponent(jmsConfiguration);

        String as400List = "L09;L11";
        for (String receiver : as400List.split(";")) {
            Dictionary<String, String> brokerJndi = new Hashtable<>();
            brokerJndi.put(OSGI_JNDI_SERVICE_NAME, "camel-component/as400-" + receiver);
            services.add(asKeyValueService(JmsComponent.class.getName(), jmsCompnent, brokerJndi));
        }
    }

    @Override
    protected String[] loadConfigAdminConfigurationFile() {
        return new String[]{"src/test/resources/configuration/decathlon.logistique.LOGSortFlows.properties", "decathlon.logistique.LOGSortFlows"};
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint-context.xml";
    }

}