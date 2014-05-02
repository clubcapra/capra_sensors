package ca.etsmtl.capra;

import org.apache.commons.logging.Log;
import org.ros.namespace.GraphName;
import org.ros.node.parameter.ParameterTree;

import java.net.URL;
import java.util.Properties;


/**
 * Created by guillaumechevalier on 2014-03-16.
 */
public class Config {
    private final Log logger;
    private final static URL filePath = Config.class.getResource("warningLight.properties");
    private final Properties properties = new Properties();

    public Config(Log logger) {
        this.logger = logger;
        try {
            properties.load(filePath.openStream());
        } catch (Exception e) {
            logger.info("Failed to load default config.");
        }
    }

    public synchronized String getString(GraphName param, ParameterTree parameterTree) {
        if (parameterTree.has(param)) {
            return parameterTree.getString(param);
        } else {
            logger.debug(String.format("Reading properties[%s]", param.getBasename().toString()));
            return (String) properties.get(param.getBasename().toString());
        }
    }

    public synchronized Integer getInteger(GraphName param, ParameterTree parameterTree) {

        if (parameterTree.has(param)) {
            return parameterTree.getInteger(param);
        } else {
            logger.debug(String.format("Reading properties[%s]", param.getBasename().toString()));
            return Integer.parseInt(properties.get(param.getBasename().toString()).toString());
        }
    }

    public synchronized Boolean getBoolean(GraphName param, ParameterTree parameterTree) {
        if (parameterTree.has(param)) {
            return parameterTree.getBoolean(param);
        } else {
            logger.debug(String.format("Reading properties[%s]", param.getBasename().toString()));
            return Boolean.parseBoolean(properties.get(param.getBasename().toString()).toString());
        }
    }

}
