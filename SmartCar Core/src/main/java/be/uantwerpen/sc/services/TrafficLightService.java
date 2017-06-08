package be.uantwerpen.sc.services;

import be.uantwerpen.sc.controllers.CCommandSender;
import be.uantwerpen.sc.controllers.CLightSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by Admin on 8-6-2017.
 */
@Service
public class TrafficLightService {

    @Autowired
    CLightSender cLightSender;

    public void updateState(long id, String state){
        String command = "LIGHT "+id+" "+state;
        cLightSender.sendLightCommand(command);
    }
}
