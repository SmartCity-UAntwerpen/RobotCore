package be.uantwerpen.sc.tools;

import be.uantwerpen.sc.controllers.DriverCommandSender;
import be.uantwerpen.sc.controllers.mqtt.MqttPublisher;
import be.uantwerpen.sc.services.DataService;
import be.uantwerpen.sc.services.QueueService;
import be.uantwerpen.rc.models.map.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.BlockingQueue;

/**
 * Created by Niels on 4/05/2016.
 */

public class QueueConsumer implements Runnable
{

    @Value("${sc.core.ip:localhost}") //values won't be loaded beceause QueueConsumer is created with "new" in systemloader
    private String serverIP;

    @Value("#{new Integer(${sc.core.port}) ?: 1994}")
    private int serverPort;

    private DriverCommandSender sender;
    private QueueService queueService;
    private DataService dataService;

    private boolean lockGranted = false;
    //private boolean first = true;
    private int prevQueueSize = 0;

    private BlockingQueue<String> jobQueue;

    private Logger logger = LoggerFactory.getLogger(QueueConsumer.class);

    public QueueConsumer(QueueService queueService, DriverCommandSender sender, DataService dataService, String serverIP, int serverPort)
    {
        this.queueService = queueService;
        this.sender = sender;
        this.dataService = dataService;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
                if((dataService.getCurrentLocation() == dataService.getDestination()) && (dataService.getDestination() != -1L) && (dataService.getCurrentLocation() != -1L)){
                    Terminal.printTerminal("Current location : " + dataService.getCurrentLocation() + " destination : " + dataService.getDestination() + " tempjob : " + dataService.tempjob);

                    if(!dataService.tempjob){ //end of total job
                        logger.info("Total job finished, Waiting for new job...");
                        dataService.robotDriving = false;
                        RestTemplate restTemplate = new RestTemplate(); //standaard resttemplate gebruiken
                        restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/job/finished/" + dataService.getRobotID()
                                , Void.class);

                        dataService.setDestination(-1L);
                        dataService.jobfinished = true;
                    }else { //end of temp job
                        logger.info("Arrived to starting location, executing job...");
                        dataService.setDestination(-1L);
                        dataService.robotDriving = false;
                        dataService.tempjob = false;
                    }

                    dataService.executingJob = false;
                }

            if(queueService.getContentQueue().size() != 0){
                if(!dataService.robotBusy && (dataService.getWorkingmodeEnum() != null)){

                    switch(dataService.getWorkingmodeEnum()){
                        case INDEPENDENT:
                            logger.info("Independent case");
                            logger.info("Robot not busy");
                            logger.info(queueService.getContentQueue().toString());
                            String s = queueService.getJob();
                            logger.info("executing: " + s);

                            if (s.contains("UPDATE LOCATION")) {
                                String split[] = s.split(" ");
                                dataService.setPrevNode(dataService.getCurrentLocation());
                                dataService.setCurrentLocation(Long.parseLong(split[2]));
                                dataService.setNextNode(Long.parseLong(split[3]));
                            } else if(s.equals("SEND LOCATION")) {
                                RestTemplate rest = new RestTemplate();
                                rest.getForObject("http://" + serverIP + ":" + serverPort + "/bot/" + dataService.getRobotID() + "/locationUpdate/" +dataService.getCurrentLocation(), boolean.class);
                            } else {
                                sender.sendCommand(s);
                                if(!s.contains("SPEAKER")) {
                                    dataService.robotBusy = true;
                                    while(dataService.robotBusy){ //wait till drive event is finished
                                    }
                                }
                            }

                            break;
                     default:
                         break;
                    }

                }
            }
        }
    }

    public void RequestLock(Long robotID, long nextNode){
        try{
            if(dataService.getNextNode() != -1) {
                RestTemplate rest = new RestTemplate();
                boolean response = false;
                logger.trace("Lock Requested : " + dataService.getNextNode());

                while (!response) {

                    response = rest.getForObject("http://" + serverIP + ":" + serverPort + "/point/requestlock/" + robotID + "/" + nextNode, boolean.class);

                    if (!response) {
                        logger.warn("Lock Denied: " + nextNode);
                        Thread.sleep(200);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void ReleaseLock(){
        Terminal.printTerminal("resetting point :" + dataService.getPrevNode());
        RestTemplate restTemplate = new RestTemplate();
        boolean setlock = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/point/setlock/" + dataService.getPrevNode() + "/0", Boolean.class);
    }

    public void ReleaseLock(Long pointToRelease){
        Terminal.printTerminal("resetting point :" + dataService.getPrevNode());
        RestTemplate restTemplate = new RestTemplate();
        boolean setlock = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/point/setlock/" + pointToRelease + "/0", Boolean.class);
    }
}