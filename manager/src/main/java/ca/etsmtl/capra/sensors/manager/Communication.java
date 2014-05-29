package ca.etsmtl.capra.sensors.manager;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Created by guillaumechevalier on 2014-03-17.
 */
public interface Communication {
    public String sendCommand(String command) throws UnknownHostException, IOException ;
}
