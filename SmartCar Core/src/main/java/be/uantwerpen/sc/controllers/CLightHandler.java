package be.uantwerpen.sc.controllers;

import be.uantwerpen.sc.controllers.mqtt.MqttLightPublisher;
import be.uantwerpen.sc.controllers.mqtt.MqttLocationPublisher;
import be.uantwerpen.sc.models.TrafficLightEntity;
import be.uantwerpen.sc.services.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.DataInputStream;
import java.net.Socket;

/**
 * Created by Arthur on 4/05/2016.
 */
@Service
public class CLightHandler implements Runnable
{
    @Autowired
    DataService dataService;

    @Autowired
    MqttLightPublisher mqttLightPublisher;

    Socket socket;
    DataInputStream dIn;

    @Value("${trafficlightcore.ip:localhost}")
    private String trafficLightIP;

    @Value("#{new Integer(${trafficlightcore.eventport}) ?: 1314}")
    private int trafficLightEventPort;

    public CLightHandler()
    {

    }

    @PostConstruct
    private void postConstruct()
    {
        //IP / port-values are initialised at the end of the constructor
        try
        {
            socket = new Socket(trafficLightIP, trafficLightEventPort);
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
        while(!Thread.currentThread().isInterrupted()){
            try {
                byte[] bytes = readData();
                String s = new String(bytes);

                if(s == "TEST"){
                    System.out.println("DE TEST IS AANGEKOMEN");
                    TrafficLightEntity tle = new TrafficLightEntity();
                    tle.setTlid((long) 1);
                    tle.setState("GREEN");
                    mqttLightPublisher.publishLight(tle, 1);
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        try{
            socket.close();

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private byte[] readData(){
        byte[] bytes = new byte[1024];
        try {
            byte b = dIn.readByte();
            char c = ((char) b);
            int i = 0;
            while (c != '\n') {
                //Terminal.printTerminal("" + c);
                bytes[i] = b;
                i++;
                b = dIn.readByte();
                c = ((char) b);
            }
            bytes[i-1] = '\0';
            return bytes;
        }catch(Exception e){
            e.printStackTrace();
        }
        return bytes;
    }
}
