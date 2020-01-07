package com.oxit.flow.logistique.cf;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsConfiguration;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import java.util.*;

public class AS400ConnectionFactoriesActivator implements BundleActivator, ManagedService {

    private static final Logger LOG = LoggerFactory.getLogger(AS400ConnectionFactoriesActivator.class);

    private ServiceRegistration configRef;
    private Map<String, JmsComponent> aliasToConnectionFactoryMap = new HashMap<>();

    @Override
    public void start(BundleContext context) {
        LOG.info("Starting AS400 activator bundle");

        Hashtable<String, String> defaults = new Hashtable();
        defaults.put(org.osgi.framework.Constants.SERVICE_PID, "decathlon.logistique.as400");

        // register to receive configuration
        configRef = context.registerService(
                ManagedService.class,
                this,
                defaults
        );
    }

    @Override
    public void stop(BundleContext context) {
        LOG.info("Stopping AS400 activator bundle");
        configRef.unregister();
    }

    @Override
    public void updated(Dictionary<String, ?> dictionary) {
        LOG.info("Updated with configuration");
        if (dictionary == null || dictionary.isEmpty()) {
            LOG.warn("Configuration is empty");
        } else {
            Map<String, String> newAs400List = extractAliasHostMap(dictionary);
            BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
            updateConnectionFactoryList(newAs400List, context);
            exposeAliasHostMap(newAs400List, context);
        }
    }

    /**
     * Generate the Map AS400 Alias => Host from the properties
     */
    protected Map<String, String> extractAliasHostMap(Dictionary<String, ?> dictionary) {
        Map<String, String> newAs400List = new HashMap<>(dictionary.size());
        Enumeration<String> keys = dictionary.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (key.startsWith("as400.warehouse.")) {
                String as400Alias = key.substring(key.lastIndexOf('.') + 1);
                String as400Host = (String) dictionary.get(key);
                if (as400Host != null && !as400Host.isEmpty()) {
                    newAs400List.put(as400Alias, as400Host);
                }
            }
        }
        return newAs400List;
    }

    protected void updateConnectionFactoryList(Map<String, String> newAs400List, BundleContext context) {
        if (!aliasToConnectionFactoryMap.isEmpty()) {
            LOG.error("Hot reload is not implemented yet for this bundle. Restart the bundle to have the change taken into effect !");
            // TODO : gérer les ajouts / suppression / modification
            return;
        }

        newAs400List.forEach((alias, host) -> {
            try {
                JmsComponent camelComponent = createCamelComponent(host, 1414, host, "CLIENTJAVA");
                aliasToConnectionFactoryMap.put(alias, camelComponent);
                Hashtable<String, String> defaults = new Hashtable();
                defaults.put("osgi.jndi.service.name", "camel-component/as400-" + alias);
                defaults.put("as400.host", host);

                LOG.info("Register connection factory for {} : {}", alias, host);
                context.registerService(JmsComponent.class, camelComponent, defaults);
            } catch (JMSException ex) {
                LOG.error("Unable to create connection factory for {} : {}", host, ex);
            }
        });

    }

    private void exposeAliasHostMap(Map<String, String> newAs400List, BundleContext context) {
        Hashtable<String, String> defaults = new Hashtable();
        defaults.put("osgi.jndi.service.name", "as400/aliasHostMap");
        context.registerService(Map.class, newAs400List, defaults);
    }

    private JmsComponent createCamelComponent(String hostName, int port, String queueManager, String channel) throws JMSException {
        MQQueueConnectionFactory connectionFactory = new MQQueueConnectionFactory();

        connectionFactory.setHostName(hostName);
        connectionFactory.setPort(port);
        connectionFactory.setQueueManager(queueManager);
        connectionFactory.setChannel(channel);
        connectionFactory.setTransportType(WMQConstants.WMQ_CM_CLIENT);

        // TODO : activer la compression ?

        JmsConfiguration jmsConfiguration = new JmsConfiguration();
        jmsConfiguration.setConnectionFactory(connectionFactory);

        return new JmsComponent(jmsConfiguration);
    }
}