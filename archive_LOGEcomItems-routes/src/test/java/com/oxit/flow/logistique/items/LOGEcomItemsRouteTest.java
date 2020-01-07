package com.oxit.flow.logistique.items;

import com.oxit.flow.message.MessageHeaders;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.junit.Before;
import org.junit.Test;

import javax.jms.ConnectionFactory;
import java.util.*;

public class LOGEcomItemsRouteTest extends CamelBlueprintTestSupport {

	private static final String OSGI_JNDI_SERVICE_NAME = "osgi.jndi.service.name";
	
    @Before
    public void adviceTraceRoute() throws Exception {
        RouteDefinition toAS400 = context.getRouteDefinition("EcomItems-from-TESTHOST");

        toAS400.adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
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

                interceptSendToEndpoint("jms:LOGEcomItemsOutput")
                        .skipSendToOriginalEndpoint()
                        .to("mock:localBroker");
            }
        });
    }

    @Test
    public void givenAKnownReceiverGetOneOkTrace() throws InterruptedException {
        MockEndpoint mockTraceMessage = (MockEndpoint) this.context.getEndpoint("mock:traceMessage");
        mockTraceMessage.expectedMessageCount(1);
        mockTraceMessage.expectedHeaderReceived(MessageHeaders.STATUS, "OK");

        MockEndpoint mockAS400Endpoint = (MockEndpoint) this.context.getEndpoint("mock:localBroker");
        mockAS400Endpoint.expectedMessageCount(1);

        Map<String, Object> headers = new HashMap<>();
        template.sendBodyAndHeaders("as400-TESTHOST:LOGEcomItems", "jms2api message 1", headers);

        assertMockEndpointsSatisfied();
    }

    @Override
    protected void addServicesOnStartup(List<KeyValueHolder<String, KeyValueHolder<Object, Dictionary >>> services) {
        super.addServicesOnStartup(services);
        Dictionary<String, String> brokerTraceJndi = new Hashtable<>();
        brokerTraceJndi.put(OSGI_JNDI_SERVICE_NAME, "jms/broker-traces");

        Dictionary<String, String> brokerLocalJndi = new Hashtable<>();
        brokerLocalJndi.put(OSGI_JNDI_SERVICE_NAME, "jms/broker-local");

        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");

        services.add(asKeyValueService(ConnectionFactory.class.getName(), connectionFactory, brokerTraceJndi));
        services.add(asKeyValueService(ConnectionFactory.class.getName(), connectionFactory, brokerLocalJndi));


        Map<String, String> aliasHostMap = new HashMap<>();
        aliasHostMap.put("L01", "TESTHOST");
        Dictionary<String, String> aliasHostMapProp = new Hashtable<>();
        aliasHostMapProp.put(OSGI_JNDI_SERVICE_NAME, "as400/aliasHostMap");
        services.add(asKeyValueService(Map.class.getName(), aliasHostMap, aliasHostMapProp));

        Dictionary<String, String> as400ComponentProps = new Hashtable<>();
        as400ComponentProps.put(OSGI_JNDI_SERVICE_NAME, "camel-component/as400-L01");
        as400ComponentProps.put("as400.host", "TESTHOST");

        JmsConfiguration jmsConfiguration = new JmsConfiguration();
        jmsConfiguration.setConnectionFactory(connectionFactory);

        services.add(asKeyValueService(JmsComponent.class.getName(), new JmsComponent(jmsConfiguration), as400ComponentProps));
    }

    @Override
    protected String[] loadConfigAdminConfigurationFile() {
        return new String[] { "src/test/resources/configuration/decathlon.logistique.LOGEcomItems.properties", "decathlon.logistique.LOGEcomItems" };
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint-context.xml";
    }

}