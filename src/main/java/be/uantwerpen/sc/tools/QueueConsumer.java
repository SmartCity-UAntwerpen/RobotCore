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
                                rest.getForObject("http://" + serverIP + ":" + serverPort +
                                        "/bot/" + dataService.getRobotID() + "/locationUpdate/" +
                                        dataService.getCurrentLocation(), boolean.class);
                            } else if (s.contains("REQUEST LOCKS")) {
                                String split[] = s.split(" ");
                                Long driveTo = Long.parseLong(split[2]);
                                Long linkId = Long.parseLong(split[3]);
                                requestPointLock(dataService.getRobotID(), driveTo);
                                requestLinkLock(dataService.getRobotID(), linkId);
                            } else if(s.contains("RELEASE LOCKS")) {
                                String split [] = s.split(" ");
                                Long point = Long.parseLong(split[2]);
                                Long linkId =  Long.parseLong(split[3]);
                                releasePointLock(dataService.getRobotID(), point);
                                requestLinkLock(dataService.getRobotID(), linkId);
                            } else {
                                //commands that have to be executed on the robot driver
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

    public void requestPointLock(Long robotID, Long driveTo) {
        try{
            if(driveTo != -1) {
                RestTemplate rest = new RestTemplate();
                boolean response = false;
                logger.trace("point lock Requested :" + driveTo);

                while (!response) {
                    response = rest.getForObject("http://" + serverIP + ":" + serverPort + "/point/requestlock/" + robotID + "/" + driveTo, boolean.class);
                    if (!response) {
                        logger.warn("Point lock denied: " + driveTo);
                        Thread.sleep(200);
                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void requestLinkLock(Long robotId, Long linkId) {
        if(linkId != -1) {
            RestTemplate rest = new RestTemplate();
            boolean response = false;
            logger.trace("Link lock requested: " + linkId);
            while(!response) {
                response = rest.getForObject("http://" + serverIP + ":" + serverPort + "/link/requestlock/" + robotId + "/" + linkId, boolean.class);
                if(!response) {
                    logger.warn("Link lock denied: " + linkId);
                    try {
                        Thread.sleep(200);
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            logger.error("linkID = " +linkId + "when requesting linklock");
        }

    }

    public void releasePointLock(Long robotId, Long pointId) {
        logger.trace("releasing point lock: " + pointId);
        RestTemplate rest = new RestTemplate();
        boolean response = false;
        while(!response) {
            response = rest.getForObject("http://" + serverIP + ":" + serverPort + "/point/unlock/" + robotId + "/" + pointId, boolean.class);
            if(!response) {
                logger.warn("Point release denied: " + pointId);
                try {
                    Thread.sleep(200);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void releaseLinkLock(Long robotId, Long linkId) {
        logger.trace("releasing link lock: " + linkId);
        RestTemplate rest = new RestTemplate();
        boolean response = false;
        while(!response) {
            response = rest.getForObject("http://" + serverIP + ":" + serverPort + "/link/unlock/" + robotId + "/" + linkId, boolean.class);
            if(!response) {
                logger.warn("Link release denied: " + linkId);
                try {
                    Thread.sleep(200);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}