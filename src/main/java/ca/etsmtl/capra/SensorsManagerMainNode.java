package ca.etsmtl.capra;

import org.apache.commons.logging.Log;
import org.ros.RosRun;
import org.ros.exception.ServiceException;
import org.ros.internal.node.topic.PublisherIdentifier;
import org.ros.message.MessageListener;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.namespace.GraphName;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.service.ServiceResponseBuilder;
import org.ros.node.service.ServiceServer;
import org.ros.node.topic.Subscriber;
import org.ros.node.topic.SubscriberListener;
import org.ros.namespace.GraphName;
import java.net.ConnectException;
import java.util.concurrent.TimeUnit;
import capra_msgs.ToggleLight;
import capra_msgs.ToggleLightRequest;
import capra_msgs.ToggleLightResponse;
import capra_msgs.ModuleToggle;
import capra_msgs.ModuleToggleRequest;
import capra_msgs.ModuleToggleResponse;


/**
 * Created by guillaumechevalier on 2014-04-18.
 */
public class SensorsManagerMainNode extends AbstractNodeMain{
    private Communication communication = null;
    private GraphName graphName = null;
    private Log logger;
    private final String NODE_NAME = "sensors_server";
    public static void main(String[] args) throws Exception {
        RosRun.main(new String[]{SensorsManagerMainNode.class.getName()});
    }


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
//        do{
//            try {
                communication = new TCPCommunication(ip, port);
//            }catch(Exception e){e.printStackTrace();}
//        }while(communication == null);


        final Log logger = connectedNode.getLog();
        warningLightManager = new WarningLightManager(connectedNode, this, communication);

        initToggles(connectedNode);



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


    private void initToggles(ConnectedNode connectedNode){
        ServiceServer<ToggleLightRequest, ToggleLightResponse> LightServer =
                connectedNode.newServiceServer("~light", ToggleLight._TYPE,
                        new ServiceResponseBuilder<ToggleLightRequest, ToggleLightResponse>() {
                            @Override
                            public void build(ToggleLightRequest request, ToggleLightResponse response) throws ServiceException {
                                System.out.println("getOn = "+request.getOn());
                            }
                        });
        ServiceServer<ModuleToggleRequest, ModuleToggleResponse> ModuleServer =
                connectedNode.newServiceServer("~module", ModuleToggle._TYPE,
                        new ServiceResponseBuilder<ModuleToggleRequest, ModuleToggleResponse>() {
                            @Override
                            public void build(ModuleToggleRequest request, ModuleToggleResponse response) throws ServiceException {
                                try {
                                    System.out.println("getModule = " + request.getModule());
                                    System.out.println("getOn = " + request.getOn());
                                    if (request.getOn())
                                        communication.sendCommand("SET " + request.getModule() + " ON");
                                    else
                                        communication.sendCommand("SET " + request.getModule() + " OFF");
                                }catch(Exception e){

                                }
                            }


                        });
    }
}
