package be.uantwerpen.sc.controllers;

import be.uantwerpen.sc.controllers.mqtt.MqttLocationPublisher;
import be.uantwerpen.sc.services.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KeepAliveController implements Runnable{
    @Autowired
    private DataService dataService;

    @Autowired
    MqttLocationPublisher locationPublisher;

    //Send each 2 minutes a mqtt message, so RobotBackend knows bot is still live.
    public void run(){

        while(!Thread.currentThread().isInterrupted()){
            try
            {
                Thread.currentThread().sleep(120000); //wait 2 minutes
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            locationPublisher.sendAlive();

        }
    }
}
