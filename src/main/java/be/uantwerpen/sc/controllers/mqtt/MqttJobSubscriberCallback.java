package be.uantwerpen.sc.controllers.mqtt;

import be.uantwerpen.sc.services.JobService;
import be.uantwerpen.sc.tools.Terminal;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Thomas on 01/06/2016.
 */

/*
Class waar de berichten van de mqtt worden opgevangen
 */
public class MqttJobSubscriberCallback implements MqttCallback
{
    private Logger logger = LoggerFactory.getLogger(MqttJobSubscriberCallback.class);

    private JobService jobService;
    private MqttJobSubscriber subscriber;

    public MqttJobSubscriberCallback(MqttJobSubscriber subscriber, JobService jobService)
    {
        this.subscriber = subscriber;
        this.jobService = jobService;
    }

    @Override
    public void connectionLost(Throwable cause)
    {
        //This is called when the connection is lost. We could reconnect here.
        logger.error("connection lost to mqtt broker");
    }

    //bericht wordt ontvangen
    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception
    {
        //TODO Process message
        logger.info("mqtt message ontvangen");
        logger.info("Topic: " + topic + ", Message: " + mqttMessage);
        String payloadString = new String(mqttMessage.getPayload());
        logger.info("payload = " + payloadString);

        try
        {
            jobService.parseJob(payloadString); //ontvangen bericht toevoegen aan de jobs
        }
        catch(Exception e)
        {
            System.err.println("Could not parse job from message: " + payloadString);
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken)
    {

    }
}
