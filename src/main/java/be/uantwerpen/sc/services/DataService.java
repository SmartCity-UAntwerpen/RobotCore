package be.uantwerpen.sc.services;

import be.uantwerpen.rc.models.map.Point;
import be.uantwerpen.sc.controllers.DriverCommandSender;
import be.uantwerpen.rc.models.Job;
import be.uantwerpen.rc.models.map.Link;
import be.uantwerpen.rc.models.map.Map;
import be.uantwerpen.sc.tools.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Created by Arthur on 24/04/2016.
 *
 * @Author Riad on 12/12/2019
 */
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

    private Long nextNode = -1L;
    private Long prevNode = -1L;

    private volatile boolean robotBusy = false;
    private volatile boolean locationUpdated = true;
    public String trafficLightStatus;
    private volatile Long currentLocation = -1L;

    private Map map = null;
    private NavigationParser navigationParser = null;

    private String tag = "NO_TAG";

    private PathplanningEnum pathplanningEnum;
    private WorkingmodeEnum workingmodeEnum;

    private volatile Long destination = -1L;
    private volatile boolean robotDriving = false;
    private volatile boolean tempjob = false;     //used to go to start location of job
    private volatile Job job = null;

    public synchronized  void setRobotBusy(boolean robotBusy) {
        this.robotBusy = robotBusy;
    }
    public synchronized  boolean getRobotBusy() {
        return robotBusy;
    }

    public synchronized boolean getLocationUpdated() {
        return locationUpdated;
    }

    public synchronized void setLocationUpdated(boolean locationUpdated) {
        this.locationUpdated = locationUpdated;
    }

    public synchronized boolean getTempJob() {
        return tempjob;
    }

    public synchronized  void setTempJob(boolean tempjob) {
        this.tempjob = tempjob;
    }
    public synchronized boolean getRobotDriving() {
        return robotDriving;
    }

    public synchronized void setRobotDriving(boolean robotDriving) {
        this.robotDriving = robotDriving;
    }

    public synchronized Job getJob() {
        return job;
    }

    public synchronized  void setJob(Job job) {
        this.job = job;
    }

    public synchronized Long getNextNode() {
        return nextNode;
    }

    public synchronized void setNextNode(Long nextNode) {
        this.nextNode = nextNode;
    }

    public synchronized Long getPrevNode() {
        return prevNode;
    }

    public synchronized void setPrevNode(Long prevNode) {
        this.prevNode = prevNode;
    }

    public Long getRobotID() {
        return robotID;
    }

    public void setRobotID(Long robotID) {
        this.robotID = robotID;
    }

    public synchronized Long getCurrentLocation() {
        return currentLocation;
    }

    public synchronized void setCurrentLocation(Long currentLocation) {
        this.currentLocation = currentLocation;
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
    public synchronized Map getMap(){
        return map;
    }

    public synchronized void setMap(Map map){
        this.map=map;
    }

    public void firstLink(){
        if(this.map != null) {
            Long start = this.getCurrentLocation();
            Long id;
            for(Point node : map.getPointList()) {
                if(node.getId().equals(start)) {
                    Link link = node.getNeighbours().get(0);
                    //lid = link.getId();
                    nextNode = link.getEndPoint();
                    prevNode = link.getStartPoint();
                }
            }
            id=this.getCurrentLocation();
            Terminal.printTerminal("Current Link: " + id);
        }
    }
    public synchronized void setDestination(Long dest){this.destination = dest;}
    public synchronized Long getDestination(){return this.destination;}

    public NavigationParser getNavigationParser() {
        return navigationParser;
    }

    public void setNavigationParser(NavigationParser navigationParser) {
        this.navigationParser = navigationParser;
    }
}