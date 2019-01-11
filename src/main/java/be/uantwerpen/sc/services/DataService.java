package be.uantwerpen.sc.services;

import be.uantwerpen.sc.controllers.DriverCommandSender;
import be.uantwerpen.rc.models.Job;
import be.uantwerpen.rc.models.map.Link;
import be.uantwerpen.rc.models.map.Map;
import be.uantwerpen.rc.models.map.Node;
import be.uantwerpen.rc.tools.Edge;
import be.uantwerpen.sc.tools.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Created by Arthur on 24/04/2016.
 */
//Class waarin bijna alles wordt bijgehouden
@Service
public class DataService
{
    @Value("${sc.backend.ip:localhost}")
    private String serverIP;

    @Value("#{new Integer(${sc.backend.port}) ?: 1994}")
    private int serverPort;

    @Autowired
    DriverCommandSender commandSender;

    private Long robotID;
    private Long linkMillis;

    private Long nextNode = -1L;
    private Long prevNode = -1L;

    volatile boolean locationVerified = false;
    private int hasPermission = -1;

    volatile public boolean robotBusy = false;
    volatile public boolean locationUpdated = true;
    public String trafficLightStatus;
    private Long currentLocation = -1L;

    public Map map = null;
    public NavigationParser navigationParser = null;

    private String tag = "NO_TAG";

    private PathplanningEnum pathplanningEnum;
    private WorkingmodeEnum workingmodeEnum;

    public Long destination = -1L;
    volatile public boolean robotDriving = false;

    volatile public boolean jobfinished = false;
    volatile public boolean tempjob = false;     //used to go to start location of job
    volatile public boolean executingJob = false;
    public Job job = null;

    public Long getNextNode() {
        return nextNode;
    }

    public void setNextNode(Long nextNode) {
        this.nextNode = nextNode;
    }

    public boolean isLocationVerified() {
        return locationVerified;
    }

    public void setLocationVerified(boolean locationVerifier) {
        this.locationVerified = locationVerifier;
    }

    public Long getPrevNode() {
        return prevNode;
    }

    public void setPrevNode(Long prevNode) {
        this.prevNode = prevNode;
    }

    public int hasPermission() {
        return hasPermission;
    }

    public void setPermission(int hasPermission) {
        this.hasPermission = hasPermission;
    }

    public Long getRobotID() {
        return robotID;
    }

    public void setRobotID(Long robotID) {
        this.robotID = robotID;
    }

    public Long getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Long currentLocation) {
        this.currentLocation = currentLocation;
    }
    public Long getLinkMillis() {
        return linkMillis;
    }

    public void setLinkMillis(Long linkMillis) {
        this.linkMillis = linkMillis;
    }

    public String getTag() {return tag;}
    public void setTag(String tag) {this.tag = tag;}

    public PathplanningEnum getPathplanningEnum() {
        return pathplanningEnum;
    }

    public void setPathplanningEnum(PathplanningEnum pathplanningEnum) {
        this.pathplanningEnum = pathplanningEnum;
    }

    public WorkingmodeEnum getWorkingmodeEnum() {
        return workingmodeEnum;
    }

    public void setworkingmodeEnum(WorkingmodeEnum workingmodeEnum) {
        this.workingmodeEnum = workingmodeEnum;
    }

    public NavigationParser getNavigationParser(){ return navigationParser;}

    public void setNavigationParser(NavigationParser parser){ this.navigationParser=parser;}

    public Map getMap(){
        return map;
    }

    public void setMap(Map map){
        this.map=map;
    }

    public void firstLink(){
        if(map != null) {
            Long start = getCurrentLocation();
            Long lid = -1L;
            for(Node node : map.getNodeList()) {
                if(node.getNodeId() == start) {
                    Link link = node.getNeighbours().get(0);
                    lid = link.getId();
                    nextNode = link.getEndPoint().getId();
                    prevNode = link.getStartPoint().getId();
                }
            }
            lid=getCurrentLocation();   //BIJGEVOEGD      =====FOUT
            Terminal.printTerminal("Current Link: " + lid);
            //RestTemplate rest = new RestTemplate();
            //rest.getForObject("http://" + serverIP + ":" + serverPort + "/bot/" + robotID + "/lid/" + lid, Integer.class);

        }
    }
    public void setDestination(Long dest){this.destination = dest;}
    public Long getDestination(){return this.destination;}
}