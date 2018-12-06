package be.uantwerpen.sc.controllers.mqtt;

import be.uantwerpen.sc.controllers.DriverStatusEventHandler;
import be.uantwerpen.sc.services.DataService;
import be.uantwerpen.sc.tools.Terminal;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Created by Arthur on 9/05/2016.
 */

// Class waarmee de mqtt berichten worden gestuurd
@Service
public class MqttPublisher
{
    @Autowired
    private DataService dataService;

    @Value("${mqtt.ip:localhost}")
    private String mqttIP;

    @Value("#{new Integer(${mqtt.port}) ?: 1883}")
    private int mqttPort;

    @Value("${mqtt.username:default}")
    private String mqttUsername;

    @Value("${mqtt.password:default}")
    private String mqttPassword;
    private Logger logger = LoggerFactory.getLogger(MqttPublisher.class);


    public void publishLocation(Integer drivenDistance)
    {
        String content      = "Location:{id:"+dataService.getRobotID()
                +"/ vertex:"+dataService.getCurrentLocation()
                +"/ progress:"+ drivenDistance+"}";
        int qos             = 2;
        String topic        = "BOT/" + dataService.getRobotID()+"/loc";
        String broker       = "tcp://" + mqttIP + ":" + mqttPort;
        String clientId     = "-1";

        if(dataService.getRobotID() != null)
        {
            clientId = dataService.getRobotID().toString();
        }

        MemoryPersistence persistence = new MemoryPersistence();

        if(dataService.getRobotID() != null)
        {
            try
            {
                MqttClient client = new MqttClient(broker, clientId + "_publisher", persistence);
                MqttConnectOptions connOpts = new MqttConnectOptions();
                connOpts.setCleanSession(true);
                connOpts.setUserName(mqttUsername);
                connOpts.setPassword(mqttPassword.toCharArray());
                client.connect(connOpts);
                MqttMessage message = new MqttMessage(content.getBytes());
                message.setQos(qos);
                client.publish(topic, message);
                System.out.println("Message published: "+ content + " destination: " + dataService.getDestination() + " nextprevnode : " + dataService.getNextNode() + " " + dataService.getPrevNode());

                client.disconnect();
            }
            catch(MqttException me)
            {
                logger.error("Could not publish topic: " + topic + " to mqtt service!");
                logger.error("reason " + me.getReasonCode());
                logger.error("msg " + me.getMessage());
            }
        }
    }

    public void close()
    {
        try
        {

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        System.out.println("Disconnected");
    }

    public void sendAlive(){
        try {
            String topic = "BOT/alive";
            String broker = "tcp://" + mqttIP + ":" + mqttPort;
            String clientId = dataService.getRobotID().toString();

            MemoryPersistence persistence = new MemoryPersistence();

            MqttClient client = new MqttClient(broker, clientId + "_publisher", persistence);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setUserName(mqttUsername);
            connOpts.setPassword(mqttPassword.toCharArray());

            client.connect(connOpts);
            String content = "botid:" + clientId;
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(2);

            client.publish(topic, message);
            System.out.println("Message published: "+ content);

            client.disconnect();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
