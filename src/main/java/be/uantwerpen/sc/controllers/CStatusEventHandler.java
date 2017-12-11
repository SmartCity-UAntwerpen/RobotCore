package be.uantwerpen.sc.controllers;

import be.uantwerpen.sc.controllers.mqtt.MqttLocationPublisher;
import be.uantwerpen.sc.services.DataService;
import be.uantwerpen.sc.tools.Terminal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.net.Socket;

/**
 * Created by Arthur on 4/05/2016.
 */
@Service
public class CStatusEventHandler implements Runnable
{
    @Autowired
    DataService dataService;

    @Autowired
    MqttLocationPublisher locationPublisher;

    Socket socket;
    DataInputStream dIn;

    //@Value("${car.ccore.ip:localhost}")
    @Value("${car.ccore.ip:146.175.140.187}")
    private String coreIP;

    @Value("#{new Integer(${car.ccore.eventport}) ?: 1314}")
    private int coreEventPort;

    public CStatusEventHandler()
    {

    }

    @PostConstruct
    private void postConstruct()
    {
        //IP / port-values are initialised at the end of the constructor
        try
        {

            socket = new Socket(coreIP, coreEventPort);
            dIn = new DataInputStream(socket.getInputStream());

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void run()
    {

        Terminal.printTerminal("CstatusEventHandler start");
        while(!Thread.currentThread().isInterrupted()){
            Terminal.printTerminal("CstatusEventHandler running");
            try {
                String s = readData();
                Terminal.printTerminal("String = " + s);
                //TODO Continue this method
                if (s.startsWith("DRIVE EVENT: FINISHED")){
                    synchronized (this){
                        dataService.robotBusy = false;
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

                        String millisString = s.split(":", 2)[1].trim();
                        int millis = Integer.parseInt(millisString);

                        if(!dataService.isLocationVerified()){
                            if(millis < 50){
                                dataService.setLocationVerified(true);
                            }else{
                                dataService.setMillis(0);
                            }
                        }else{
                            synchronized (this) {
                                //Terminal.printTerminal("Distance: " + millis);
                                dataService.setMillis(millis);
                                locationPublisher.publishLocation(millis, 45L); ////whuuuuuuut
                            }
                        }


                }if (s.startsWith("TAG DETECTION EVENT")){
                    String tag = s.split(":", 2)[1].trim();
                    synchronized (this){
                        dataService.setTag(tag);
                        dataService.robotBusy = false;

                        //dataService.setCurrentLocationAccordingTag();
                        if(!tag.trim().equals("NONE"))
                        {
                            dataService.locationUpdated = true;
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
            Terminal.printTerminal("connection closed with bot");
            socket.close();

        }catch (Exception e){
            e.printStackTrace();
        }

        Terminal.printTerminal("CstatusEventHandler end");

    }


    private String readData(){
        String recvData = "";
        try {
            Terminal.printTerminal("ReadData");
            if(dIn==null){
                Terminal.printTerminal("dIn is null");
                try
                {
                    socket = new Socket(coreIP, coreEventPort);
                    dIn = new DataInputStream(socket.getInputStream());
                    if(socket.getInputStream()==null) {
                        Terminal.printTerminal("get is null");
                    }
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }

            Terminal.printTerminal("CstatusEventHandler: waiting for data");
            Terminal.printTerminal("CstatusEventHandler: dIn open?" + socket.isConnected());
            char b = (char)dIn.readByte();
            Terminal.printTerminal("CstatusEventHandler: firstChar received = " + b);
            //recvData = recvData + b;
            while(b != '\n'){
                recvData = recvData + b;
                b = (char)dIn.readByte();

            }
            //String s = new String(bytes);

            Terminal.printTerminal("CstatusEventHandler: received data = " + recvData);

            return recvData;
        }catch(Exception e){
            e.printStackTrace();
        }
        Terminal.printTerminal(" end ReadData");
        return recvData;
    }
}
