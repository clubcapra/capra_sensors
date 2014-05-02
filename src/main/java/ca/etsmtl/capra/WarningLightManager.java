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
    private boolean isRunning;
    protected int DELAY = 1000;
    protected String OnCommand = "SET Lights ON",OffCommand = "SET Lights OFF";
    private Communication communication;
    private boolean isLightOn,isEStopOn = false; //isEstopOn is temporary (needs to be attached to a service)

    /**
     *
     * @param connectedNode
     * @param sensorsManagerMainNode
     * @throws SerialPortException
     */
    public WarningLightManager(final ConnectedNode connectedNode, SensorsManagerMainNode sensorsManagerMainNode){
        ParameterTree parameterTree = connectedNode.getParameterTree();
        this.connectedNode = connectedNode;
        this.logger = connectedNode.getLog();
        this.graphName = sensorsManagerMainNode.getDefaultNodeName().join(SensorsManagerMainNode.class.getSimpleName());
        Config config = new Config(logger);
        DELAY  = config.getInteger(graphName.join("FlashDelay"), parameterTree);
        final String ip = config.getString(graphName.join("ip"), parameterTree);
        final int port = config.getInteger(graphName.join("port"), parameterTree);


        Runnable rCheckEs = new Runnable() {
            @Override
            public void run() {
                try {
                    communication = new TCPCommunication(ip, port);
                    isLightOn = true;
                    isRunning = true; //TEMPO!!!
                    logger.info("isRunning = " + isRunning);
                    while (isRunning) {
                        isEStopOn = false; //TEMPO!
                        if (isEStopOn) {
                            communication.sendCommand(OffCommand);
                        } else {
                            if (isLightOn) {
                                isLightOn = false;
                                communication.sendCommand(OnCommand);
                            } else {
                                isLightOn = true;
                                communication.sendCommand(OffCommand);
                            }
                        }
                        Thread.sleep(DELAY);

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
        isEStopOn = flashingState;
    }

    /**
     * Temp fix for closing Thread (listener to implement eventually)
     */
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
