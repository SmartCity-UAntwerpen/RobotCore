package be.uantwerpen.sc.controllers;

import be.uantwerpen.sc.controllers.mqtt.MqttPublisher;
import be.uantwerpen.sc.services.DataService;
import be.uantwerpen.sc.tools.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by Arthur on 4/05/2016.
 */
//Hier worden de antwoorden van de robotdriver opgevangen
@Service
public class DriverStatusEventHandler implements Runnable
{
    @Autowired
    DataService dataService;

    @Autowired
    MqttPublisher locationPublisher;

    Socket socket;
    DataInputStream dIn;

    Logger logger = LoggerFactory.getLogger(DriverStatusEventHandler.class);

    //@Value("${car.ccore.ip:localhost}")
    @Value("${car.driver.ip:146.175.140.187}")
    private String driverIp;

    @Value("#{new Integer(${car.driver.eventport}) ?: 1314}")
    private int driverEventPort;

    public DriverStatusEventHandler()
    {

    }

    @PostConstruct
    private void postConstruct()
    {
        //IP and port-values are initialised at the end of the constructor
        try
        {

            socket = new Socket(driverIp, driverEventPort);
            dIn = new DataInputStream(socket.getInputStream());

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void closeConnection(){
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run()
    {
        while(!Thread.currentThread().isInterrupted()){
            try {
                String s = readData(); //Waiting for messages
                if (s.startsWith("DRIVE EVENT: FINISHED")){
                    synchronized (this){
                        dataService.setRobotBusy(false);
                    }
                }if (s.startsWith("TRAFFICLIGHT DETECTION EVENT")){
                    String statusString = s.split(":", 2)[1];
                    String status = "";
                    if(statusString.contains("GREEN")){
                        status = "GREEN";
                    }
                    if(statusString.contains("RED")){
                        status = "RED";
                    }
                    if(statusString.contains("NONE")){
                        status = "NONE";
                    }
                    synchronized (this) {
                        dataService.trafficLightStatus = status;
                    }
                }
                if (s.startsWith("TRAVEL DISTANCE EVENT")){
                    //TODO use this link progress to determine a better progress
                    //String travelledDistance = s.split(":", 2)[1].trim();
                    //locationPublisher.publishLocation(Integer.parseInt(travelledDistance));

                }if (s.startsWith("TAG DETECTION EVENT")){
                    String tag = s.split(":", 2)[1].trim();
                    synchronized (this){
                        dataService.setTag(tag);
                        dataService.setRobotBusy(false);

                        if(!tag.trim().equals("NONE") && !tag.trim().equals("NO_TAG"))
                        {
                            if(dataService.getMap() != null){
                                dataService.setCurrentLocation(dataService.map.getLocationByRFID(tag));
                            }
                            dataService.setLocationUpdated(true);
                        }
                    }
                }if (s.startsWith("TRAFFIC_LIGHT")){
                    String trafficlightStatus = s.split(" ", 2)[1];
                    synchronized (this){
                        dataService.trafficLightStatus = trafficlightStatus;
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        try{
            Terminal.printTerminal("Closed connection to bot");
            socket.close();

        }catch (Exception e){
            e.printStackTrace();
        }

        Terminal.printTerminal("DriverStatusEventHandler end");

    }


    private String readData(){
        String recvData = "";
        try {
            if(dIn==null){
                try
                {
                    socket = new Socket(driverIp, driverEventPort);
                    dIn = new DataInputStream(socket.getInputStream());
                    if(socket.getInputStream()==null) {
                        logger.error("No inputstream for event socket");
                        Terminal.printTerminal("There is no inputstream for the event socket");
                    }
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }

            char b = (char)dIn.readByte(); //Read first char
            while(b != '\n'){ //Read the other chars and add them to string
                recvData = recvData + b;
                b = (char)dIn.readByte();

            }
            return recvData;
        }catch(Exception e){
            System.err.println("Lost connection to robot");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException er) {
                er.printStackTrace();
            }
        }
        return recvData;
    }
}