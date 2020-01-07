package com.oxit.flow.logistique.as400.mapping.route;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.jms.ConnectionFactory;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.apache.activemq.ActiveMQConnectionFactory;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.oxit.flow.message.MessageHeaders;

/**
 * @author aidri
 * @since 28/10/2019
 */
public class MappingToAs400FormatTest extends CamelBlueprintTestSupport {
	
	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(MappingToAs400FormatTest.class);

	@Test
	public void testMappingLOGSortGroupOfStores() throws Exception {
		RouteDefinition mappingRoute = context.getRouteDefinition("Mapping.ToAS400.LOGSortGroupOfStores");
		mockOutputQueues(mappingRoute, exchange -> {
		});

		Map<String, Object> headers = new HashMap<>();
		headers.put(MessageHeaders.PROCESS_TYPE_NAME, "LOGSortGroupOfStores");

		MockEndpoint mockOutputQueue= (MockEndpoint) this.context.getEndpoint("mock:OutputLOGSortGroupOfStores");
		mockOutputQueue.expectedMessageCount(1);
		MappingToAs400FormatTest.compareContentWithIgnoringIOL(mockOutputQueue);
		template.sendBodyAndHeaders("jms://queue:LOGSortGroupOfStores.mapping.input",
				MappingToAs400FormatTest.getFileContent("src/test/resources/LOGSortGroupOfStores.xml"), headers);
		mockOutputQueue.expectedHeaderReceived("FileName", "STQGCPP");
		
		Callable<Boolean> condition = () -> mockOutputQueue.getReceivedCounter() != 0;
		Awaitility.await().atMost(Duration.TEN_SECONDS).until(condition);

		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void testMappingLOGSortWarehouseMachine() throws Exception {
		RouteDefinition mappingRoute = context.getRouteDefinition("Mapping.ToAS400.LOGSortWarehouseMachine");
		mockOutputQueues(mappingRoute, exchange -> {
		});

		Map<String, Object> headers = new HashMap<>();
		headers.put(MessageHeaders.PROCESS_TYPE_NAME, "LOGSortWarehouseMachine");
		MockEndpoint mockOutputQueue= (MockEndpoint) this.context.getEndpoint("mock:OutputLOGSortWarehouseMachine");
		mockOutputQueue.expectedMessageCount(1);
		MappingToAs400FormatTest.compareContentWithIgnoringIOL(mockOutputQueue);
		template.sendBodyAndHeaders("jms://queue:LOGSortWarehouseMachine.mapping.input",
				MappingToAs400FormatTest.getFileContent("src/test/resources/LOGSortWarehouseMachine.xml"), headers);
		mockOutputQueue.expectedHeaderReceived("FileName", "STQHCPP");
		
		Callable<Boolean> condition = () -> mockOutputQueue.getReceivedCounter() != 0;
		Awaitility.await().atMost(Duration.TEN_SECONDS).until(condition);

		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void testMappingLOGSortMaterialMachine() throws Exception {
		RouteDefinition mappingRoute = context.getRouteDefinition("Mapping.ToAS400.LOGSortMaterialMachine");
		mockOutputQueues(mappingRoute, exchange -> {
		});

		Map<String, Object> headers = new HashMap<>();
		headers.put(MessageHeaders.PROCESS_TYPE_NAME, "LOGSortMaterialMachine");

		MockEndpoint mockOutputQueue= (MockEndpoint) this.context.getEndpoint("mock:OutputLOGSortMaterialMachine");
		mockOutputQueue.expectedMessageCount(1);
		MappingToAs400FormatTest.compareContentWithIgnoringIOL(mockOutputQueue);
		template.sendBodyAndHeaders("jms://queue:LOGSortMaterialMachine.mapping.input",
				MappingToAs400FormatTest.getFileContent("src/test/resources/LOGSortMaterialMachine.xml"), headers);
		mockOutputQueue.expectedHeaderReceived("FileName", "STQICPP");
		
		Callable<Boolean> condition = () -> mockOutputQueue.getReceivedCounter() != 0;
		Awaitility.await().atMost(Duration.TEN_SECONDS).until(condition);

		assertMockEndpointsSatisfied();
	}
	
	
	

	@Override
	protected String getBlueprintDescriptor() {
		return "/OSGI-INF/blueprint/blueprint-context.xml";
	}

	private void mockOutputQueues(final RouteDefinition route, Processor processor) throws Exception {
		route.adviceWith(context, new RouteBuilder() {
			@Override
			public void configure() {
				interceptSendToEndpoint("jms:queue:LOGSortGroupOfStores")
					.skipSendToOriginalEndpoint().process(processor)
					.to("mock:OutputLOGSortGroupOfStores");
				interceptSendToEndpoint("jms:queue:LOGSortWarehouseMachine")
					.skipSendToOriginalEndpoint().process(processor)
					.to("mock:OutputLOGSortWarehouseMachine");
				interceptSendToEndpoint("jms:queue:LOGSortMaterialMachine")
					.skipSendToOriginalEndpoint().process(processor)
					.to("mock:OutputLOGSortMaterialMachine");
			}
		});
	}

	@Override
	protected CamelContext createCamelContext() throws Exception {
		CamelContext camelContext = super.createCamelContext();
		camelContext.addRoutes(new RouteBuilder() {
			@Override
			public void configure() {

				from("direct-vm:traceMessage").process(exchange -> LOG.info("Received trace"))
						.to("mock:traceMessage");
			}
		});
		return camelContext;
	}

	/**
	 * Provide necessary services (activemq).
	 * 
	 * @param services
	 *            init services
	 */
	@Override
	protected void addServicesOnStartup(List<KeyValueHolder<String, KeyValueHolder<Object, Dictionary>>> services) {
		super.addServicesOnStartup(services);

		Dictionary<String, String> brokerTraceJndi = new Hashtable<>();
		brokerTraceJndi.put("osgi.jndi.service.name", "jms/broker-traces");

		Dictionary<String, String> brokerLocalJndi = new Hashtable<>();
		brokerLocalJndi.put("osgi.jndi.service.name", "jms/broker-local");

		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
				"vm://localhost?broker.persistent=false");
		connectionFactory.setUserName("karaf");
		connectionFactory.setPassword("karaf");

		services.add(asKeyValueService(ConnectionFactory.class.getName(), connectionFactory, brokerTraceJndi));
		services.add(asKeyValueService(ConnectionFactory.class.getName(), connectionFactory, brokerLocalJndi));
	}

	private static String getFileContent(String file) {
		try {
			return new String(Files.readAllBytes(Paths.get(file)));
		} catch (IOException ex) {
			return "";
		}
	}
	
	private static void compareContentWithIgnoringIOL(MockEndpoint mockEndpoint) {
		mockEndpoint.expects(() -> {
            Exchange exchange = mockEndpoint.getExchanges().get(0);

            String actualBody = exchange.getIn().getBody(String.class);
            actualBody = actualBody.replaceAll("\\r\\n", "\n");
            actualBody = actualBody.replaceAll("\\r", "\n");
            String expectedBody = MappingToAs400FormatTest.getFileContent("src/test/resources/LOGSortWarehouseMachine.txt");
            expectedBody = actualBody.replaceAll("\\r\\n", "\n");
            expectedBody = actualBody.replaceAll("\\r", "\n");

            assertEquals(expectedBody,actualBody);
        });
	}
}