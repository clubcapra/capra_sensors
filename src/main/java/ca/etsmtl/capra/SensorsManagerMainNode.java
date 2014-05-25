package ca.etsmtl.capra;

import org.apache.commons.logging.Log;
import org.ros.RosRun;
import org.ros.concurrent.CancellableLoop;
import org.ros.exception.ServiceException;
import org.ros.message.MessageListener;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.namespace.GraphName;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.service.ServiceResponseBuilder;
import org.ros.node.service.ServiceServer;
import org.ros.node.topic.Subscriber;
import capra_msgs.ToggleLight;
import capra_msgs.ToggleLightRequest;
import capra_msgs.ToggleLightResponse;
import capra_msgs.ModuleToggle;
import capra_msgs.ModuleToggleRequest;
import capra_msgs.ModuleToggleResponse;
import capra_msgs.AiStatus;

import java.util.ArrayList;


/**
 * Created by guillaumechevalier on 2014-04-18.
 */
public class SensorsManagerMainNode extends AbstractNodeMain{
    private ArrayList<String> sensors;
    private Communication communication = null;
    private GraphName graphName = null;
    private Log logger;
    final Publisher<SensorsStatus> publisher;
    private Subscriber<AiStatus> subscriber;
    private final CancellableLoop   cancellableLoopWatchdog,
                                    cancellableLoopSensorsStateUpdate,
                                    cancellableLoopSensorsStateRetriever;
    private boolean watchdog = false;
    private Config config;
    private ParameterTree parameterTree;

    public SensorsManagerMainNode() {           //AI watchdog
        cancellableLoopWatchdog = new
                CancellableLoop() {
                    @Override
                    protected void loop() throws InterruptedException {
                        Thread.sleep(config.getInteger(graphName.join("WATCHDOG_TIMER"), parameterTree));
                        if (watchdog){
                            watchdog = false;
                        }else{

                        }
                    }
                };
        cancellableLoopSensorsStateUpdate = new
                CancellableLoop() {
                    @Override
                    protected void loop() throws InterruptedException {

                        Thread.sleep(config.getInteger(graphName.join("SENSORS_STATE_UPDATE_TIMER"), parameterTree));
                    }
                };
        cancellableLoopSensorsStateRetriever = new
                CancellableLoop() {
                    @Override
                    protected void loop() throws InterruptedException {

                    }
                };

    }

    public static void main(String[] args) throws Exception {
        RosRun.main(new String[]{SensorsManagerMainNode.class.getName()});
    }


    private WarningLightManager warningLightManager;

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        super.onStart(connectedNode);
        parameterTree = connectedNode.getParameterTree();
        this.graphName = this.getDefaultNodeName().join(SensorsManagerMainNode.class.getSimpleName());
        publisher = connectedNode.newPublisher(config.getString(graphName.join("TOPIC_SENSORS_STATUS"),parameterTree),
                                                SensorsStatus._TYPE);
        subscriber = connectedNode.newSubscriber(config.getString(graphName.join("TOPIC_LIGHT_STATUS"),parameterTree),
                                                AiStatus._TYPE);
        /*logger*/
        this.logger = connectedNode.getLog();
        config = new Config(logger);

        /*Tries to connect until it works.*/
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
        return GraphName.of(config.getString(graphName.join("NODE_NAME"),parameterTree));
    }


    private void initToggles(ConnectedNode connectedNode){
        ServiceServer<ToggleLightRequest, ToggleLightResponse> LightServer =
                connectedNode.newServiceServer("~light", ToggleLight._TYPE,
                        new ServiceResponseBuilder<ToggleLightRequest, ToggleLightResponse>() {
                            @Override
                            public void build(ToggleLightRequest request, ToggleLightResponse response) throws ServiceException {
                                warningLightManager.setFlashingState(request.getOn());
                            }
                        });
        ServiceServer<ModuleToggleRequest, ModuleToggleResponse> ModuleServer =
                connectedNode.newServiceServer("~module", ModuleToggle._TYPE,
                        new ServiceResponseBuilder<ModuleToggleRequest, ModuleToggleResponse>() {
                            @Override
                            public void build(ModuleToggleRequest request, ModuleToggleResponse response) throws ServiceException {
                                try {
                                    if (request.getOn())
                                        communication.sendCommand("SET " + request.getModule() + " ON");
                                    else
                                        communication.sendCommand("SET " + request.getModule() + " OFF");
                                }catch(Exception e){

                                }
                            }


                        });

        subscriber.addMessageListener(new MessageListener<AiStatus>() {
            @Override
            public void onNewMessage(AiStatus message) {
                watchdog = message.getIsRunning();
            }
        });
    }


}
