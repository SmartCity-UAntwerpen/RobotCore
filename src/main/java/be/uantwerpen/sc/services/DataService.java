package be.uantwerpen.sc.services;

import be.uantwerpen.sc.models.Link;
import be.uantwerpen.sc.models.map.Map;
import be.uantwerpen.sc.models.map.Node;
import be.uantwerpen.sc.tools.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    public void setCurrentLocationAccordingTag() {
        switch(getTag()){
            case "04 70 39 32 06 27 80":
                setCurrentLocation(20L); //3
                break;
            case "04 67 88 8A C8 48 80":
                setCurrentLocation(8L);//14
                break;
            case "04 97 36 A2 7F 22 80":
                setCurrentLocation(4L);//1
                break;
            case "04 7B 88 8A C8 48 80":
                setCurrentLocation(9L);//15
                break;
            case "04 B3 88 8A C8 48 80":
                setCurrentLocation(2L);//8
                break;
            case "04 8D 88 8A C8 48 80":
                setCurrentLocation(5L);//9
                break;
            case "04 AA 88 8A C8 48 80":
                setCurrentLocation(14L);//11
                break;
            case "04 C4 FD 12 Q9 34 80":
                setCurrentLocation(19L);
                break;
            case "04 96 88 8A C8 48 80":
                setCurrentLocation(17L);//17
                break;
            case "04 A1 88 8A C8 48 80":
                setCurrentLocation(15L);//18
                break;
            case "04 86 04 22 A9 34 84":
                setCurrentLocation(20L);
                break;
            case "04 18 25 9A 7F 22 80":
                setCurrentLocation(11L);//6
                break;
            case "04 BC 88 8A C8 48 80":
                setCurrentLocation(16L);//16
                break;
            case "04 C5 88 8A C8 48 80":
                setCurrentLocation(3L);//7
                break;
            case "04 EC 88 8A C8 48 80":
                setCurrentLocation(19L);//
                break;
            case "04 E3 88 8A C8 48 80":
                setCurrentLocation(1L);//13
                break;
            case "04 26 3E 92 1E 25 80":
                setCurrentLocation(6L);//4
                break;
            case "04 DA 88 8A C8 48 80":
                setCurrentLocation(13L);//12
                break;
            case "04 41 70 92 1E 25 80":
                setCurrentLocation(18L);//2
                break;
            case "04 3C 67 9A F6 1F 80":
                setCurrentLocation(10L);//5
                break;
            case "NONE":
                break;
            default:
                setCurrentLocation(-1L);
                break;
        }
    }
}