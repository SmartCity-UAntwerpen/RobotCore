package be.uantwerpen.sc.controllers;

import be.uantwerpen.sc.controllers.mqtt.MqttPublisher;
import be.uantwerpen.sc.services.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by Arthur on 11/05/2016.
 */

//Als de bot aan het rijden is moet er om de zoveel tijd een bericht gestuurd worden locatie en progress
@Service
public class DriverLocationPoller implements Runnable
{
    @Autowired
    DriverCommandSender driverCommandSender;

    @Autowired
    MqttPublisher mqttPublisher;

    @Autowired
    DataService dataService;

    public void run()
    {

        while(!Thread.currentThread().isInterrupted())
        {
            try
            {
                Thread.currentThread().sleep(2000);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            if(dataService.map != null && dataService.robotDriving){
                //dataService.readTag();
                //Vraagt op hoeveel mm er al gereden is
                driverCommandSender.sendCommand("DRIVE DISTANCE");
            }
        }
    }
}