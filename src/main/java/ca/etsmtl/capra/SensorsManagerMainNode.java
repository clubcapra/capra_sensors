package ca.etsmtl.capra;

import org.apache.commons.logging.Log;
import org.ros.RosRun;
import org.ros.message.MessageListener;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.namespace.GraphName;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.topic.Subscriber;
import org.ros.node.topic.SubscriberListener;
import org.ros.namespace.GraphName;
import java.net.ConnectException;
import java.util.concurrent.TimeUnit;
import capra_msgs.ToggleLight;
import capra_msgs.ToggleLightRequest;
import capra_msgs.ToggleLightResponse;

/**
 * Created by guillaumechevalier on 2014-04-18.
 */
public class SensorsManagerMainNode extends AbstractNodeMain{
    private Communication communication;
    private GraphName graphName = null;
    private Log logger;
    public static void main(String[] args) throws Exception {
//        RosRun.main(new String[]{Talker.class.getName()});
        RosRun.main(new String[]{SensorsManagerMainNode.class.getName()});

    }

    private final static String NODE_NAME = "/capra/sensors_manager";
    private final static String TOPIC1 = "chatter";
    private WarningLightManager warningLightManager;



    @Override
    public void onStart(final ConnectedNode connectedNode) {
        super.onStart(connectedNode);
        ParameterTree parameterTree = connectedNode.getParameterTree();
        this.graphName = this.getDefaultNodeName().join(SensorsManagerMainNode.class.getSimpleName());

        this.logger = connectedNode.getLog();
        Config config = new Config(logger);
        final String ip = config.getString(graphName.join("ip"), parameterTree);
        final int port = config.getInteger(graphName.join("port"), parameterTree);
        communication = new TCPCommunication(ip, port);
        Subscriber<std_msgs.String> subscriber = connectedNode.newSubscriber(TOPIC1, std_msgs.String._TYPE);
        final Log logger = connectedNode.getLog();
        warningLightManager = new WarningLightManager(connectedNode, this, communication);


        subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {
                logger.info("I heard: \"" + message.getData() + "\"");
            }
        });
    }

    @Override
    public void onShutdown(Node node) {
        super.onShutdown(node);
        if (warningLightManager != null) {
            warningLightManager.close();
        }

    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(NODE_NAME);
    }


}
