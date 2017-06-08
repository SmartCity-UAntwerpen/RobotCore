package be.uantwerpen.sc.controllers.mqtt;

import be.uantwerpen.sc.services.TrafficLightService;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by Dries on 10-5-2017.
 */
public class MqttLightSubscriberCallback implements MqttCallback
{
    TrafficLightService trafficLightService;

    public MqttLightSubscriberCallback(TrafficLightService trafficLightService)
    {
        this.trafficLightService = trafficLightService;
    }

    @Override
    public void connectionLost(Throwable cause)
    {
        //This is called when the connection is lost. We could reconnect here.
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception
    {
        System.out.println("MQTT LIGHT ARRIVED");
        String lightIDString = topic.split("/")[1];
        Long lightID = Long.parseLong(lightIDString);

        String payloadString = new String(mqttMessage.getPayload());

        if(!topic.endsWith("Light")){
            return;
        }

        String temp = payloadString.split("id:")[1];
        String id = temp.split("/")[0];
        temp = temp.split("state:")[1];
        String state = temp.split("}")[0];

        if(state == "")
        {
            return;
        }

        trafficLightService.updateState((long) Integer.parseInt(id), state);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken)
    {

    }
}
