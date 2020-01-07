package com.oxit.flow.logistique.cf;

import org.apache.camel.component.jms.JmsComponent;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.util.*;

import static org.junit.Assert.*;

public class AS400ConnectionFactoriesActivatorTest {

	private static final String FIRST_HOST_NAME = "FirstHostname";
	private static final String SECOND_HOST_NAME = "SecondHostname";
	
    @Test
    public void givenPropertyListGetMap() {
        AS400ConnectionFactoriesActivator activator = new AS400ConnectionFactoriesActivator();

        Dictionary<String, String> properties = new Hashtable<>();
        properties.put("invalid_key", "whatever");
        properties.put("as400.warehouse.L01", FIRST_HOST_NAME);
        properties.put("as400.warehouse.L02", SECOND_HOST_NAME);
        properties.put("as400.warehouse.L03", "");

        Map<String, String> aliasHostnameMap = activator.extractAliasHostMap(properties);
        assertEquals("AliasHost map contains only defined hostname", 2, aliasHostnameMap.size());
        assertFalse("AliasHost map does not contain empty hostname", aliasHostnameMap.containsValue(""));
        assertEquals("AliasHost map contains first key L01 and value", FIRST_HOST_NAME, aliasHostnameMap.get("L01"));
        assertEquals("AliasHost map contains second key L02 and value", SECOND_HOST_NAME, aliasHostnameMap.get("L02"));
    }

    @Test
    public void givenPropertyListUpdateConfiguration() {
        AS400ConnectionFactoriesActivator activator = new AS400ConnectionFactoriesActivator();
        Map<String, String> aliasHostMap = new HashMap<>();
        aliasHostMap.put("L01", FIRST_HOST_NAME);
        aliasHostMap.put("L02", SECOND_HOST_NAME);
        final BundleContext context = MockOsgi.newBundleContext();

        activator.updateConnectionFactoryList(aliasHostMap, context);

        try {
            String osgiFilter = "(osgi.jndi.service.name=camel-component/*)";
            Collection<ServiceReference<JmsComponent>> refs = context.getServiceReferences(JmsComponent.class, osgiFilter);
            assertEquals("All definitions should be registered as JMS Component",2, refs.size());
        } catch (InvalidSyntaxException e) {
            fail("Service reference lookup should work");
        }
    }
}
