package ca.etsmtl.capra;

import jssc.SerialPortException;
import org.apache.commons.logging.Log;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.parameter.ParameterTree;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Created by guillaumechevalier on 2014-03-16.
 */
public class WarningLightManager {
    private final Log logger;
    private final ConnectedNode connectedNode;
    private final GraphName graphName;
    private boolean isRunning = false;
    protected int DELAY = 1000;
    protected String OnCommand = "SET Lights ON",OffCommand = "SET Lights OFF";
    private final Communication communication;
    private boolean isLightOn = true;

    /**
     *
     * @param connectedNode
     * @param sensorsManagerMainNode
     * @throws SerialPortException
     */
    public WarningLightManager(final ConnectedNode connectedNode, SensorsManagerMainNode sensorsManagerMainNode, final Communication communication){
        ParameterTree parameterTree = connectedNode.getParameterTree();
        this.communication = communication;
        this.connectedNode = connectedNode;
        this.logger = connectedNode.getLog();
        this.graphName = sensorsManagerMainNode.getDefaultNodeName().join(SensorsManagerMainNode.class.getSimpleName());
        Config config = new Config(logger);
        DELAY  = config.getInteger(graphName.join("FlashDelay"), parameterTree);


        Runnable rCheckEs = new Runnable() {
            @Override
            public void run() {
                try {
                    isLightOn = true;
                    logger.info("isRunning = " + isRunning);
                    while (true) {
                        while(isRunning){
                            if (isLightOn) {
                                isLightOn = false;
                                communication.sendCommand(OnCommand);
                            } else {
                                isLightOn = true;
                                communication.sendCommand(OffCommand);
                            }
                            Thread.sleep(DELAY);
                        }
                    }
                } catch (InterruptedException ex) {
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread t = new Thread(rCheckEs, "thread1");
        t.start();
    }

    public boolean getState(){
        return isLightOn;
    }
    public void setFlashingState(boolean flashingState){
        isRunning = flashingState;
    }


    public void close(){
        isRunning = false;
        try {
            communication.sendCommand(OnCommand);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
