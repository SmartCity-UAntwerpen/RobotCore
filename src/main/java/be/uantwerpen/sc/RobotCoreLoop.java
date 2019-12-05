package be.uantwerpen.sc;

import be.uantwerpen.rc.models.map.Map;
import be.uantwerpen.sc.controllers.PathController;
import be.uantwerpen.sc.controllers.mqtt.MqttJobSubscriber;
import be.uantwerpen.sc.services.*;
import be.uantwerpen.sc.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

/**
 * Created by Arthur on 4/05/2016.
 */
@Service
public class RobotCoreLoop implements Runnable
{
    @Autowired
    private DataService dataService;

    @Autowired
    private PathplanningType pathplanningType;

    @Autowired
    private WorkingmodeType workingmodeType;

    @Autowired
    private MqttJobSubscriber jobSubscriber;

    @Autowired
    private JobService jobService;

    @Value("${sc.backend.ip:localhost}")
    private String serverIP;

    @Value("#{new Integer(${sc.backend.port}) ?: 1994}")
    private int serverPort;

    @Value("#{new Long(${robot.id}) ?: 0}")
    private Long botId;

    @Autowired
    private QueueService queueService;

    @Autowired
    private PathController pathController;

    private IPathplanning pathplanningService;

    private TerminalService terminalService;
    private Logger logger = LoggerFactory.getLogger(RobotCoreLoop.class);


    public RobotCoreLoop(){

    }

    @PostConstruct
    private void postconstruct(){ //struct die wordt opgeroepen na de initiele struct. Omdat de autowired pas wordt opgeroepen NA de initiele struct
        //Setup type
        Terminal.printTerminalInfo("Selected PathplanningType: " + pathplanningType.getType().name());
        Terminal.printTerminalInfo("Selected WorkingmodeType: " + workingmodeType.getType().name());
    }

    public void run() {
        //getRobotId
        terminalService=new TerminalService(); //terminal service starten. terminal wordt gebruikt om bepaalde dingen te printen en commandos in te geven
        RestTemplate restTemplate = new RestTemplate(); //standaard resttemplate gebruiken

        Long robotID = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/bot/initiate/" + botId + "/" //aan de server laten weten dat er een nieuwe bot zich aanbiedt
                +workingmodeType.getType().toString(), Long.class); //Aan de server laten weten in welke mode de bot werkt

        dataService.setRobotID(robotID);
        jobService.setRobotCoreLoop(this);
        jobService.setEndJob(-1);
        jobService.removeCommands();

        if(!jobSubscriber.initialisation()) //subscribe to topics to listen to jobs
        {
            System.err.println("Could not initialise MQTT Job service!");
        }

        //Setup interface for correct mode of pathplanningService
        this.setupInterface();
        logger.info("Interface is set up");
        //Wait for tag read
        //Read tag where bot is located
        synchronized (this) {
            while (dataService.getTag().trim().equals("NONE") || dataService.getTag().trim().equals("NO_TAG")) {
                try {
                    //Read tag
                    queueService.insertCommand("TAG READ UID");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        logger.info("Tag: " + dataService.getTag());

       // updateStartLocation();

        //Request map at server with rest
        this.getMap();
        logger.info("Map received " + dataService.getMap().getNodeList());

        //Set location of bot
        Long locationID = dataService.getMap().getLocationByRFID(dataService.getTag());
        dataService.setCurrentLocation(locationID);
        logger.info("Start Location: " + dataService.getCurrentLocation()+"\n\n");


        //We have the map now, update link
        if(dataService.getWorkingmodeEnum()==WorkingmodeEnum.INDEPENDENT)
            dataService.firstLink();
        logger.info("link updated");
        logger.info("next: "+dataService.getNextNode());

        restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/bot/" + botId + "/locationUpdate/" +dataService.getCurrentLocation(), boolean.class);
        boolean response = false;
        while(!response) {
            try {
                response = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/point/requestlock/" +dataService.getRobotID()+ "/" + dataService.getCurrentLocation(), boolean.class);
                logger.info("Lock Requested : " + dataService.getCurrentLocation());
                if(!response) {
                    logger.trace("First point lock denied with id : " + dataService.getCurrentLocation());
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch(RestClientException e) {
                logger.error("Can't connect to the database to lock first point, retrying...");
            }
        }

        while(!Thread.interrupted()){
            if(dataService.getJob() != null){
                jobService.performJob(dataService.getJob());
                dataService.setJob(null);
            }
        }
    }

    public IPathplanning getPathplanningService()
    {
        return this.pathplanningService;
    }

    public void setPathplanningService(IPathplanning pathplanningService) {
        this.pathplanningService = pathplanningService;
    }

    public void getMap() {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> responseList;
        Map map;
        while(true) { //TODO:: refactor this while(true)-loop (is ugly)
            try {
                responseList = restTemplate.getForEntity("http://" + serverIP + ":" + serverPort + "/map/", Map.class);
                map = responseList.getBody();
                break;
            } catch(RestClientException e) {
                logger.error("Can't connect to the backend to retrieve map, retrying...");
                try {
                    Thread.sleep(200);
                } catch(InterruptedException er) {
                    er.printStackTrace();
                }
            } catch(HttpMessageNotReadableException e) {
                logger.error("Can't get map from database, key fails");
                if(dataService.getMap() != null)
                    map = dataService.getMap();
            }
        }
        dataService.setMap(map);
    }

    private void setupInterface(){
        switch (pathplanningType.getType()){
            case DIJKSTRA:
                pathplanningService = new PathplanningService();
                dataService.setPathplanningEnum(PathplanningEnum.DIJKSTRA);
                break;
            case RANDOM:
                pathplanningService = new RandomPathPlanning(pathController);
                dataService.setPathplanningEnum(PathplanningEnum.RANDOM);
                break;
            default:
                //Dijkstra
                pathplanningService = new PathplanningService();
                dataService.setPathplanningEnum(PathplanningEnum.DIJKSTRA);
        }

        switch(workingmodeType.getType()) {
            case PARTIALSERVER:
                dataService.setworkingmodeEnum(WorkingmodeEnum.PARTIALSERVER);
                break;
            case FULLSERVER:
                dataService.setworkingmodeEnum(WorkingmodeEnum.FULLSERVER);
                break;
            case INDEPENDENT:
                dataService.setworkingmodeEnum(WorkingmodeEnum.INDEPENDENT);
                break;
            case PARTIALSERVERNG:
                dataService.setworkingmodeEnum(WorkingmodeEnum.PARTIALSERVERNG);
                break;
            default:
                dataService.setworkingmodeEnum(WorkingmodeEnum.INDEPENDENT);
        }
    }
}
