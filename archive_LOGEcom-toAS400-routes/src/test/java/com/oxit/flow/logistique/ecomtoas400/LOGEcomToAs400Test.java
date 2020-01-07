package com.oxit.flow.logistique.ecomtoas400;

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

public class LOGEcomToAs400Test extends CamelBlueprintTestSupport {

    private static final String MOCK_TRACE_MESSAGE = "mock:traceMessage";
    private static final String MOCK_AS400_ENDPOINT = "mock:as400Endpoint";
    private static final String OSGI_JNDI_SERVICE_NAME = "osgi.jndi.service.name";

    @Before
    public void adviceTraceRoute() throws Exception {
        RouteDefinition toAS400 = context.getRouteDefinition("LOGEcomReturns-to-AS400");

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
                        .to(MOCK_TRACE_MESSAGE);

                interceptSendToEndpoint("as400-L01:LOGEcomReturnsOutpupt")
                        .skipSendToOriginalEndpoint()
                        .to(MOCK_AS400_ENDPOINT);
            }
        });
    }

    @Test
    public void givenAKnownReceiverGetOneOkTrace() throws InterruptedException {
        MockEndpoint mockTraceMessage = (MockEndpoint) this.context.getEndpoint(MOCK_TRACE_MESSAGE);
        mockTraceMessage.expectedMessageCount(1);
        mockTraceMessage.expectedHeaderReceived(MessageHeaders.STATUS, "OK");

        MockEndpoint mockAS400Endpoint = (MockEndpoint) this.context.getEndpoint(MOCK_AS400_ENDPOINT);
        mockAS400Endpoint.expectedMessageCount(1);

        Map<String, Object> headers = new HashMap<>();
        headers.put(LOGEcomToAs400Route.WAREHOUSE_HEADER, "L01");
        template.sendBodyAndHeaders("jms:queue:LOGEcomReturns", "jms2api message 1", headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void givenAnUnknownReceiverGetOneKoTrace() throws InterruptedException {
        MockEndpoint mockTraceMessage = (MockEndpoint) this.context.getEndpoint(MOCK_TRACE_MESSAGE);
        mockTraceMessage.expectedMessageCount(1);
        mockTraceMessage.expectedHeaderReceived(MessageHeaders.STATUS, "KO");

        MockEndpoint mockAS400Endpoint = (MockEndpoint) this.context.getEndpoint(MOCK_AS400_ENDPOINT);
        mockAS400Endpoint.expectedMessageCount(0);

        Map<String, Object> headers = new HashMap<>();
        headers.put(LOGEcomToAs400Route.WAREHOUSE_HEADER, "UnknownReceiver");
        template.sendBodyAndHeaders("jms:queue:LOGEcomReturns", "jms2api message 1", headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void disabledFlowInPropertiesShouldNotStart() {
        assertNull("Click N Collect flow is disabled", context.getRouteDefinition("LOGEcomClickNCollect-to-AS400"));
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


        Dictionary<String, String> as400ComponentProps = new Hashtable<>();
        as400ComponentProps.put(OSGI_JNDI_SERVICE_NAME, "camel-component/as400-L01");

        JmsConfiguration jmsConfiguration = new JmsConfiguration();
        jmsConfiguration.setConnectionFactory(connectionFactory);

        services.add(asKeyValueService(JmsComponent.class.getName(), new JmsComponent(jmsConfiguration), as400ComponentProps));
    }

    @Override
    protected String[] loadConfigAdminConfigurationFile() {
        return new String[] { "src/test/resources/configuration/decathlon.logistique.EcomToAS400.properties", "decathlon.logistique.EcomToAS400" };
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint-context.xml";
    }

}
