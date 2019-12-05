package be.uantwerpen.sc.tools;

import be.uantwerpen.sc.controllers.DriverCommandSender;
import be.uantwerpen.sc.controllers.mqtt.MqttPublisher;
import be.uantwerpen.sc.services.DataService;
import be.uantwerpen.sc.services.QueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Created by Niels on 4/05/2016.
 */

public class QueueConsumer implements Runnable
{

    @Value("${sc.backend.ip:localhost}") //values won't be loaded beceause QueueConsumer is created with "new" in systemloader
    private String serverIP;

    @Value("#{new Integer(${sc.backend.port}) ?: 1994}")
    private int serverPort;

    private MqttPublisher locationPublisher;

    private DriverCommandSender sender;
    private QueueService queueService;
    private DataService dataService;
    private Logger logger = LoggerFactory.getLogger(QueueConsumer.class);

    public QueueConsumer(QueueService queueService, DriverCommandSender sender, DataService dataService,
                         String serverIP, int serverPort, MqttPublisher locationPublisher)
    {
        this.queueService = queueService;
        this.sender = sender;
        this.dataService = dataService;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.locationPublisher = locationPublisher;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {

            if(queueService.getContentQueue().size() != 0){
                if(!dataService.getRobotBusy() && (dataService.getWorkingmodeEnum() != null)){
                    logger.info("tempjob: "+dataService.getTempJob());

                    switch(dataService.getWorkingmodeEnum()){
                        case INDEPENDENT:
                            logger.info("Independent case");
                            logger.info("Robot not busy");
                            logger.trace(queueService.getContentQueue().toString());
                            String s = queueService.getCommand();
                            logger.info("executing: " + s);

                            if (s.contains("UPDATE LOCATION")) {
                                String split[] = s.split(" ");
                                dataService.setPrevNode(dataService.getCurrentLocation());
                                dataService.setCurrentLocation(Long.parseLong(split[2]));
                                dataService.setNextNode(Long.parseLong(split[3]));
                                float progress = (Float.parseFloat(split[4]));
                                locationPublisher.publishLocation(new Integer(Math.round(progress)));
                            } else if(s.equals("SEND LOCATION")) {
                                sendLocation();
                            } else if (s.contains("REQUEST LOCKS") || (s.contains("RELOCK TILE")) ) {
                                String split[] = s.split(" ");
                                Long driveTo = Long.parseLong(split[2]);
                                requestPointLock(dataService.getRobotID(), driveTo);
                                if(!s.contains("RELOCK TILE")) {
                                    Long linkId = Long.parseLong(split[3]);
                                    requestLinkLock(dataService.getRobotID(), linkId);
                                }
                            } else if(s.contains("RELEASE LOCKS")) {
                                String split [] = s.split(" ");
                                Long point = Long.parseLong(split[2]);
                                Long linkId =  Long.parseLong(split[3]);
                                boolean success;
                                try {
                                    do {
                                        success = releasePointLock(dataService.getRobotID(), point);
                                        Thread.sleep(1000);
                                    } while(success == false);
                                    do {
                                        releaseLinkLock(dataService.getRobotID(), linkId);
                                        Thread.sleep(1000);
                                    } while(success == false);
                                }catch(InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else if (s.contains("TRAFFICLIGHT DETECTION")) {
                                String split [] = s.split(" ");
                                Long point = Long.parseLong(split[2]);
                                handleTrafficLight(point);
                            } else if (s.contains("FINISH JOB")) {
                                //notify backend
                                sendJobFinish();
                                //notify other threads that the robot arrived at its location
                                dataService.setRobotDriving(false);
                            } else {
                                //commands that have to be executed on the robot driver
                                sender.sendCommand(s);
                                if(!s.contains("SPEAKER")) {
                                    dataService.setRobotBusy(true);
                                    while(dataService.getRobotBusy()){ //wait till drive event is finished
                                    }
                                }
                            }
                            break;
                        case PARTIALSERVER:
                            //TODO
                            break;
                        case PARTIALSERVERNG:
                            //TODO
                            break;
                        case FULLSERVER:
                            //TODO
                            break;
                     default:
                         break;
                    }

                }
            }
        }
    }

    private void sendLocation() {
        RestTemplate rest = new RestTemplate();
        while (true) {
            try {
                logger.info("Sending location to backend");
                rest.getForObject("http://" + serverIP + ":" + serverPort +
                        "/bot/" + dataService.getRobotID() + "/locationUpdate/" +
                        dataService.getCurrentLocation(), boolean.class);
                break;
            } catch (RestClientException e) {
                logger.error("Can't connect to the backend for location update, retrying...");
                try {
                    Thread.sleep(500);
                } catch(InterruptedException er) {

                }
            }
        }
    }
    private void sendJobFinish() {
        logger.info("Finishing the total job");
        RestTemplate restTemplate = new RestTemplate(); //standaard resttemplate gebruiken
        while(true) {
            try {
                logger.info("Sending job finished to backend");
                restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/job/finished/" + dataService.getRobotID()
                        , Void.class);
                break;
            } catch(RestClientException e) {
                logger.error("Can't connect to the backend to finish job, retrying...");
                try {
                    Thread.sleep(500);
                } catch(InterruptedException er) {
                    er.printStackTrace();
                }
            }
        }
        logger.info("notified the backend!");
    }

    public void requestPointLock(Long robotID, Long driveTo) {
            if(driveTo != -1) {
                RestTemplate rest = new RestTemplate();
                boolean response = false;
                logger.trace("point lock Requested :" + driveTo);
                while (!response) {
                    try{
                    response = rest.getForObject("http://" + serverIP + ":" + serverPort + "/point/requestlock/" + robotID + "/" + driveTo, boolean.class);
                    if (!response) {
                        logger.warn("Point lock denied: " + driveTo);
                        Thread.sleep(1000);
                    }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (RestClientException e)  {
                        logger.error("Can't connect to the backend for point request, retrying...");
                    }
                }
            }


    }

    private void requestLinkLock(Long robotId, Long linkId) {
        if(linkId != -1) {
            RestTemplate rest = new RestTemplate();
            boolean response = false;
            logger.info("Link lock requested: " + linkId);
            while(!response) {
                try {
                    response = rest.getForObject("http://" + serverIP + ":" + serverPort + "/link/requestlock/" + robotId + "/" + linkId, boolean.class);
                    if(!response) {
                        logger.warn("Link lock denied: " + linkId);
                        Thread.sleep(1000);
                    }
                } catch(InterruptedException e) {
                    e.printStackTrace();
                } catch(RestClientException e) {
                    logger.error("Can't connect to the backend to request lock, retrying...");
                }
            }
        } else {
            logger.error("linkID = " +linkId + "when requesting linklock");
        }

    }

    private boolean releasePointLock(Long robotId, Long pointId) {
        logger.info("releasing point lock: " + pointId);
        try {
            RestTemplate rest = new RestTemplate();
            boolean response = rest.getForObject("http://" + serverIP + ":" + serverPort + "/point/unlock/" + robotId + "/" + pointId, boolean.class);
            return true;
        } catch(RestClientException e ) {
            logger.error("Can't connect to the backend");
            return false;
        }

    }

    private boolean releaseLinkLock(Long robotId, Long linkId) {
        logger.trace("releasing link lock: " + linkId);
        try {
            RestTemplate rest = new RestTemplate();
            boolean response = rest.getForObject("http://" + serverIP + ":" + serverPort + "/link/unlock/" + robotId + "/" + linkId, boolean.class);
            return true;
        } catch(RestClientException e) {
            return false;
        }

    }

    private void handleTrafficLight(Long point) {
        logger.info("Arrived at trafflight");
        RestTemplate rest = new RestTemplate();

        String response = "RED";
        while(!response.equals("GREEN")) {
            try {
                response = rest.getForObject("http://" + serverIP + ":" + serverPort + "/tlight/getState/" + point, String.class);
                if(!response.equals("GREEN")) {
                    logger.warn("Trafficlight status: "+response);
                    Thread.sleep(200);
                }
            } catch(RestClientException  e) {
                logger.error("Can't connect to the backend for trafficlight status, retrying...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

}