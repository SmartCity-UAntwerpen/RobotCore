package be.uantwerpen.sc.services;

import be.uantwerpen.sc.controllers.CCommandSender;
import be.uantwerpen.sc.models.links.Link;
import be.uantwerpen.sc.models.map.Map;
import be.uantwerpen.sc.models.map.Node;
import be.uantwerpen.sc.tools.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Created by Arthur on 24/04/2016.
 */
@Service
public class DataService
{
    @Value("${sc.core.ip:localhost}")
    private String serverIP;

    @Value("#{new Integer(${sc.core.port}) ?: 1994}")
    private int serverPort;

    @Autowired
    CCommandSender commandSender;

    private Long robotID;

    private int millis;
    private Long linkMillis;

    private Long nextNode = -1L;
    boolean locationVerified = false;
    private Long prevNode = -1L;
    private int hasPermission = -1;
    public boolean robotBusy = false;
    public boolean locationUpdated = true;
    public String trafficLightStatus;
    private Long currentLocation = -1L;

    public Map map = null;
    public NavigationParser navigationParser = null;

    private String tag = "NO_TAG";

    private String LookingCoordiante;
    private PathplanningEnum pathplanningEnum;
    private WorkingmodeEnum workingmodeEnum;

    public Long destination = -1L;
    public boolean robotDriving = false;

    public boolean jobfinished = false;
    public boolean tempjob = false;
    public boolean executingJob = false;

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

    public int getMillis() {return millis;}
    public void setMillis(int millis) {this.millis = millis;}

    public Long getLinkMillis() {
        return linkMillis;
    }

    public void setLinkMillis(Long linkMillis) {
        this.linkMillis = linkMillis;
    }

    public String getTag() {return tag;}
    public void setTag(String tag) {this.tag = tag;}

    public String getLookingCoordiante() {
        return LookingCoordiante;
    }

    public void setLookingCoordiante(String lookingCoordiante) {
        LookingCoordiante = lookingCoordiante;
    }

    public void changeLookingCoordiante(String command){
        if(command.equals("DRIVE TURN L")){
            switch(getLookingCoordiante()){
                case "N":
                    setLookingCoordiante("W");
                    break;
                case "E":
                    setLookingCoordiante("N");
                    break;
                case "Z":
                    setLookingCoordiante("E");
                    break;
                case "W":
                    setLookingCoordiante("Z");
            }
        }

        if(command.equals("DRIVE TURN R")){
            switch(getLookingCoordiante()){
                case "N":
                    setLookingCoordiante("E");
                    break;
                case "E":
                    setLookingCoordiante("Z");
                    break;
                case "Z":
                    setLookingCoordiante("W");
                    break;
                case "W":
                    setLookingCoordiante("N");
            }
        }

        if(command.equals("DRIVE ROTATE R 180")){
            switch(getLookingCoordiante()){
                case "N":
                    setLookingCoordiante("Z");
                    break;
                case "E":
                    setLookingCoordiante("W");
                    break;
                case "Z":
                    setLookingCoordiante("N");
                    break;
                case "W":
                    setLookingCoordiante("E");
            }
        }

    }

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
            for(Node node : map.getNodeList()){
                if(node.getNodeId() == start){
                    Link link = node.getNeighbours().get(0);
                    lid = link.getId();
                    nextNode = link.getStopPoint().getId();
                    prevNode = link.getStartPoint().getId();
                    linkMillis = link.getLength();
                }
            }
            lid=getCurrentLocation();   //BIJGEVOEGD      =====FOUT
            Terminal.printTerminal("Current Link: " + lid);
            //RestTemplate rest = new RestTemplate();
            //rest.getForObject("http://" + serverIP + ":" + serverPort + "/bot/" + robotID + "/lid/" + lid, Integer.class);

        }
    }

    public void readTag(){

        try {
                    //Read tag
            if(map != null){
                commandSender.sendCommand("TAG READ UID");
            }

            //Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*
        if(tag != null && !tag.equals("NONE") && !tag.equals("NO_TAG")){
            currentLocation = map.getNodeByRFID(tag);
        }
        */
    }

    public void nextLink(){
        if(map != null && navigationParser != null && navigationParser.list != null && !navigationParser.list.isEmpty() && navigationParser.list.size() != 1) {
            Long start = navigationParser.list.get(0).getId();
            Long end = navigationParser.list.get(1).getId();
            if(getTag().trim().equals("NONE")){
                currentLocation = nextNode;
            }
            //setCurrentLocationAccordingTag();
            nextNode = end;
            prevNode = start;
            Long lid = -1L;
            //find link from start to end
            for (Edge e : navigationParser.list.get(0).getAdjacencies()) {
                if (e.getTarget() == end) {
                    lid = e.getLinkEntity().getId();
                    linkMillis = e.getLinkEntity().getLength();
                    Terminal.printTerminal("New Link Distance: " + linkMillis);
                }
            }

            setCurrentLocation(lid);
            Terminal.printTerminal("Current Link: " + lid);
            if(this.pathplanningEnum == PathplanningEnum.DIJKSTRA) {
                //delete entry from navigationParser
                navigationParser.list.remove(0);
            }
            //RestTemplate rest = new RestTemplate();
            //rest.getForObject("http://" + serverIP + ":" + serverPort + "/bot/" + robotID + "/lid/" + lid, Integer.class);
        }else{
            //TODO update location
            Terminal.printTerminal("Entering manual manouvering mode. Location will be inaccurate");
            prevNode = nextNode;
        }
    }

    public void setDestination(Long dest){this.destination = dest;}
    public Long getDestination(){return this.destination;}
}