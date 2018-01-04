package be.uantwerpen.sc.models.map;

import be.uantwerpen.sc.models.Bot;
import be.uantwerpen.sc.models.TrafficLightEntity;
//import be.uantwerpen.sc.tools.pathplanning.AbstractMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Niels on 3/04/2016.
 */
public class Map
{
    private List<Node> nodeList;
    private List<Bot> botEntities;
    private List<TrafficLightEntity> trafficlightEntity;

    public Map(){
        nodeList = new ArrayList<>();
        botEntities = new ArrayList<>();
    }

    public  void addNode(Node node){
        nodeList.add(node);
    }

    public void setNodeList(List<Node> nodeList) {
        this.nodeList = nodeList;
    }

    public List<Node> getNodeList() {
        return nodeList;
    }

    public List<Bot> getBotEntities() {
        return botEntities;
    }

    public void setBotEntities(List<Bot> botEntities) {
        this.botEntities = botEntities;
    }

    public List<TrafficLightEntity> getTrafficlightEntity() {
        return trafficlightEntity;
    }

    public void setTrafficlightEntity(List<TrafficLightEntity> trafficlightEntity) {
        this.trafficlightEntity = trafficlightEntity;
    }

    public boolean isEndPoint(String s){
        for (int i = 0; i < getNodeList().size(); i++) {
            if(s.equals(getNodeList().get(i).getPointEntity().getRfid())){
                if(getNodeList().get(i).getNeighbours().size()<2)
                    return true;
            }
        }
        return false;

    }

    public String changeLookingDir(Long linkid, String rfidTag){
        for (int i = 0; i < getNodeList().size(); i++) {
            if (rfidTag.equals(getNodeList().get(i).getPointEntity().getRfid())) {
                for (int j = 0; j < getNodeList().get(i).getNeighbours().size(); j++) {
                    if (linkid == getNodeList().get(i).getNeighbours().get(j).getId()) {
                        switch(getNodeList().get(i).getNeighbours().get(j).getStopDirection()){
                            case "N":
                                return "Z";
                            case "E":
                                return "W";
                            case "Z":
                                return "N";
                            case "W":
                                return "E";
                        }
                    }
                }
            }
        }
        return null;
    }

    public Long getNodeByRFID(String rfid){

        Long nodeNumber = -1L;

        for(int i = 0; i < nodeList.size(); i++) {
            if(nodeList.get(i).getPointEntity().getRfid().equals(rfid)){
                nodeNumber = nodeList.get(i).getPointEntity().getId();
            }
        }

        return nodeNumber;

    }

}