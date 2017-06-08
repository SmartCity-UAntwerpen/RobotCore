package be.uantwerpen.sc.services;

import be.uantwerpen.sc.controllers.CCommandSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by Admin on 8-6-2017.
 */
@Service
public class TrafficLightService {

    @Autowired
    CCommandSender cCommandSender;

    public void updateState(long id, String state){
        String command = "LIGHT "+id+" "+state;
        cCommandSender.sendCommand(command);
    }
}
