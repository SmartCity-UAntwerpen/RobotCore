package be.uantwerpen.sc;

import be.uantwerpen.sc.controllers.MapController;
import be.uantwerpen.sc.controllers.PathController;
import be.uantwerpen.sc.controllers.mqtt.MqttJobSubscriber;
import be.uantwerpen.sc.services.*;
import be.uantwerpen.sc.tools.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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

    @Value("${sc.core.ip:localhost}")
    private String serverIP;

    @Value("#{new Integer(${sc.core.port}) ?: 1994}")
    private int serverPort;

    @Autowired
    private QueueService queueService;
    @Autowired
    private MapController mapController;
    @Autowired
    private PathController pathController;

    public IPathplanning pathplanning;

    private TerminalService terminalService;

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

        Long robotID = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/bot/initiate/" //aan de server laten weten dat er een nieuwe bot zich aanbied
                +workingmodeType.getType().toString(), Long.class); //Aan de server laten weten in welke mode de bot werkt

        dataService.setRobotID(robotID);
        jobService.setRobotCoreLoop(this);
        jobService.setEndJob(-1);
        jobService.removeCommands();

        if(!jobSubscriber.initialisation()) //subscribe to topics to listen to jobs
        {
            System.err.println("Could not initialise MQTT Job service!");
        }

        //Setup interface for correct mode of pathplanning
        setupInterface();
        Terminal.printTerminal("Interface is set up");
        //Wait for tag read
        //Read tag where bot is located
        synchronized (this) {
            while (dataService.getTag().trim().equals("NONE") || dataService.getTag().equals("NO_TAG")) {
                try {
                    //Read tag
                    queueService.insertJob("TAG READ UID");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        Terminal.printTerminal("Tag: " + dataService.getTag());

       // updateStartLocation();

        //Request map at server with rest
        dataService.map = mapController.getMap();
        Terminal.printTerminal("Map received " + dataService.map.getNodeList());

        //Set location of bot
        dataService.setCurrentLocation(dataService.map.getLocationByRFID(dataService.getTag()));
        Terminal.printTerminal("Start Location: " + dataService.getCurrentLocation()+"\n\n");

        //We have the map now, update link

        if(dataService.getWorkingmodeEnum()==WorkingmodeEnum.INDEPENDENT)
            dataService.firstLink();
        Terminal.printTerminal("link updated");
        Terminal.printTerminal("next: "+dataService.getNextNode());

        Terminal.printTerminal("looking in direction " + dataService.getLookingCoordiante());

        RestTemplate rest = new RestTemplate();
        Terminal.printTerminal("Lock Requested : " + dataService.getCurrentLocation());
        rest.getForObject("http://" + serverIP + ":" + serverPort + "/point/requestlock/" + dataService.getCurrentLocation(), boolean.class);

        while(!Thread.interrupted()){

            if(dataService.job != null){
                jobService.performJob(dataService.job);
                dataService.job = null;
            }
        }
    }

    public IPathplanning getPathplanning()
    {
        return this.pathplanning;
    }


    private void setupInterface(){
        switch (pathplanningType.getType()){
            case DIJKSTRA:
                pathplanning = new PathplanningService();
                dataService.setPathplanningEnum(PathplanningEnum.DIJKSTRA);
                break;
            case RANDOM:
                pathplanning = new RandomPathPlanning(pathController);
                dataService.setPathplanningEnum(PathplanningEnum.RANDOM);
                break;
            default:
                //Dijkstra
                pathplanning = new PathplanningService();
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
