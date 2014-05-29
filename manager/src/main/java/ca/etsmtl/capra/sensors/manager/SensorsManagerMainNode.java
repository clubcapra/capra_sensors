package ca.etsmtl.capra.sensors.manager;

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
import capra_msgs.SensorsTelemetry;
import java.io.IOException;
import java.util.HashMap;

import org.ros.node.topic.Publisher;


/**
 * Created by guillaumechevalier on 2014-04-18.
 */
public class SensorsManagerMainNode extends AbstractNodeMain{
    private String[] sensorsList = new String[]{"Fan","IMU","Camera","GPS","Switch","Lights",
                                        "RangeFinder","Tension","Current","Temperature",
                                        "EstopManual","EstopRemote","Mode"};
    private final String NODE_NAME = "/capra/sensors/manager";
    private HashMap<String,String> sensorsListStatus = new HashMap<String,String>();
    private Communication communication = null;
    private GraphName graphName = null;
    private Log logger;
    private Publisher<SensorsTelemetry> publisher = null;
    private Subscriber<AiStatus> subscriber;
    private SensorsTelemetry sensorsTelemetry;
    private final CancellableLoop   cancellableLoopWatchdog,
                                    cancellableLoopSensorsStateUpdate,
                                    cancellableLoopSensorsStateRetriever;
    private boolean watchdog = false;
    private Config config;
    private ParameterTree parameterTree;
    private int posSensorArray = 0;

    public SensorsManagerMainNode() {                       /*AI watchdog*/
                cancellableLoopWatchdog = new
                CancellableLoop() {
                    @Override
                    protected void loop() throws InterruptedException {
                        Thread.sleep(config.getInteger(graphName.join("WATCHDOG_TIMER"), parameterTree));
                        if (watchdog){
                            watchdog = false;
                            if(!(sensorsListStatus.get(sensorsList[5]) == "ON")){                      //Not flashing?
                                warningLightManager.setFlashingState(true);     //Flashing
                            }
                        }else{
                            if(sensorsListStatus.get(sensorsList[5]) == "ON"){
                                warningLightManager.setFlashingState(false);
                            }
                        }
                    }
                };
        cancellableLoopSensorsStateUpdate = new             /*Update sensorsTelemetry*/
                CancellableLoop() {
                    @Override
                    protected void loop() throws InterruptedException {
                            //sensorsTelemetry = populateMessage(sensorsTelemetry);
                            try {
                                sensorsTelemetry = populateMessage(sensorsTelemetry);
                            }catch (Exception e){}
                            Thread.sleep(config.getInteger(graphName.join("SENSORS_STATE_UPDATE_TIMER"), parameterTree));
                        }
                };
        cancellableLoopSensorsStateRetriever = new          /*Retrieves data from the Sensors*/
                CancellableLoop() {
                    @Override
                    protected void loop() throws InterruptedException {
                        for(int i = 0;i<sensorsList.length;i++) {
                            try {
                                String[] str = communication.sendCommand("GET "+sensorsList[i]).split(" ");
                                sensorsListStatus.put(str[0],str[1]);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Thread.sleep(config.getInteger(graphName.join("SENSORS_STATE_RETRIEVAL_TIMER"), parameterTree));
                        }
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
        /*logger*/
        this.logger = connectedNode.getLog();
        config = new Config(logger);
        this.graphName = this.getDefaultNodeName().join(SensorsManagerMainNode.class.getSimpleName());
        publisher = connectedNode.newPublisher(config.getString(graphName.join("TOPIC_SENSORS_STATUS"),parameterTree),
                SensorsTelemetry._TYPE);
        subscriber = connectedNode.newSubscriber(config.getString(graphName.join("TOPIC_LIGHT_STATUS"),parameterTree),
                                                AiStatus._TYPE);



        /*Tries to connect until it works.*/
        final String ip = config.getString(graphName.join("ip"), parameterTree);
        final int port = config.getInteger(graphName.join("port"), parameterTree);
//        do{
//            try {
                communication = new TCPCommunication(ip, port);
//            }catch(Exception e){e.printStackTrace();}
//        }while(communication == null);

        logger = connectedNode.getLog();

        new SensorsManagerMainNode();
        connectedNode.executeCancellableLoop(cancellableLoopWatchdog);
        connectedNode.executeCancellableLoop(cancellableLoopSensorsStateRetriever);
        connectedNode.executeCancellableLoop(cancellableLoopSensorsStateUpdate);
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
                                   warningLightManager.toggleLight(request.getOn());
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

    private SensorsTelemetry populateMessage(SensorsTelemetry st){
        st = publisher.newMessage();
        st.setFan(sensorsListStatus.get(sensorsList[0]) == "ON");
        st.setIMU(sensorsListStatus.get(sensorsList[1]) == "ON");
        st.setCamera(sensorsListStatus.get(sensorsList[2]) == "ON");
        st.setGPS(sensorsListStatus.get(sensorsList[3]) == "ON");
        st.setSwitch(sensorsListStatus.get(sensorsList[4]) == "ON");
        st.setSwitch(sensorsListStatus.get(sensorsList[5]) == "ON");
        st.setRangeFinder(sensorsListStatus.get(sensorsList[6]) == "ON");
        st.setTemperature(Float.parseFloat(sensorsListStatus.get(sensorsList[9])));
        return st;
    }

//    new String[]{"Fan","IMU","Camera","GPS","Switch","Lights",
//            "RangeFinder","Tension","Current","Temperature",
//            "EstopManual","EstopRemote","Mode"};


}
