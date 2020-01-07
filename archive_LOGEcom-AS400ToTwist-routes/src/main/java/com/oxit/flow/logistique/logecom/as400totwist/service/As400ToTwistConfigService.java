package com.oxit.flow.logistique.logecom.as400totwist.service;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.oxit.flow.logistique.logecom.as400totwist.model.As400ToTwistConfiguration;

public class As400ToTwistConfigService {
	
	private static final String PROPERTY_AS400_LIST_KEY = "list";
	private static final String PROPERTY_QUEUE_KEY = "queue";
	private static final String PROPERTY_INPUT_QUEUE_KEY = "input.queue";
	private Map<String, As400ToTwistConfiguration> as400ToTwistConfiguration = new HashMap<>();
	
	
    public As400ToTwistConfigService(String flowId,ConfigurationAdmin configAdmin, String configId) throws IOException {
        Configuration configuration = configAdmin.getConfiguration(configId);
        Dictionary<String, Object> properties = configuration.getProperties();

        for (String propertyKey : Collections.list(properties.keys())) {
            if (propertyKey.startsWith(flowId)) {
                String[] propertyParts = propertyKey.split("\\.");
                String confName = propertyParts[1];
                if(!propertyKey.split("\\.")[1].equals("input"))
                	as400ToTwistConfiguration.computeIfAbsent(confName, key -> generateConf(flowId, key, properties));
            }
        }
    }

	private As400ToTwistConfiguration generateConf(String flowId, String key, Dictionary<String, Object> properties) {
		//setter les differents params de la de l'objet BookingConfiguration
        String prefixedConfigName = flowId + "." + key + ".";

        return new As400ToTwistConfiguration(
        		(String) properties.get(flowId + "." + PROPERTY_INPUT_QUEUE_KEY),
        		key,
                (String) properties.get(prefixedConfigName + PROPERTY_AS400_LIST_KEY),
                (String) properties.get(prefixedConfigName + PROPERTY_QUEUE_KEY )
        );
	}

    public Map<String, As400ToTwistConfiguration> getAs400ToTwistConfiguration() {
        if (as400ToTwistConfiguration == null) {
        	as400ToTwistConfiguration = new HashMap<>(0);
        }
        return as400ToTwistConfiguration;
    }
}