package be.uantwerpen.sc.tools;

import be.uantwerpen.sc.services.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import be.uantwerpen.rc.tools.Vertex;
import be.uantwerpen.rc.tools.Edge;

import java.util.*;

/**
 * Created by Arthur on 28/04/2016.
 */
public class NavigationParser {

    @Autowired
    private DataService dataService;
    public List<Vertex> path;
    public Queue<DriveDir> commands = new LinkedList<DriveDir>();

    public NavigationParser(List<Vertex> path, DataService dataservice){
        this.path = path;
        this.dataService=dataservice;
    }

    public List<Vertex> getPath(){
        return path;
    }

    public Queue<DriveDir> parseMap(){
        if(path.isEmpty()){
            Terminal.printTerminalError("Cannot parse empty map");
        }else{
            //First part is always driving forward.
            commands.add(new DriveDir(DriveDirEnum.FOLLOW));
            //Second part is parsing the rest of the map
            Vertex current = path.get(0);
            System.out.println("current: "+current);
            Vertex previous;
            Vertex next = path.get(1);
            System.out.println("next: "+next+"\n");
            if(path.size()==2){
                System.out.println(dataService.getPrevNode());
                System.out.println(dataService.getNextNode());
            }else{
                for(int i = 2; i < path.size()-1; i++) {
                    previous = current;
                    current = next;
                    next = path.get(i);
                    System.out.println("previous: " + previous + " current: " + current + " next: " + next);

                    //Check at what angle the crossroad needs to be passed
                    for(Edge edge: current.getAdjacencies()) {
                        if(edge.getLinkEntity().getStartPoint().getId() == current.getId() && edge.getLinkEntity().getEndPoint().getId() == next.getId()) {
                            if(edge.getLinkEntity().getAngle() > -181 && edge.getLinkEntity().getAngle() < 181)
                                if(edge.getLinkEntity().getAngle() == 0) {
                                    commands.add(new DriveDir(DriveDirEnum.FORWARD));
                                } else if(Math.abs(edge.getLinkEntity().getAngle()) == 180) {
                                    commands.add(new DriveDir(DriveDirEnum.TURN));
                                } else {
                                    //rotate R -90 == rotate L 90
                                    commands.add(new DriveDir(DriveDirEnum.RIGHT, edge.getLinkEntity().getAngle()));
                                }
                            // execute follow line after each crossroad
                            commands.add(new DriveDir(DriveDirEnum.FOLLOW));
                        }
                        break;
                    }
                }
            }
        }
        //commands.add(new DriveDir((DriveDirEnum.TURN)));
        Terminal.printTerminal("Commands added");
        return commands;
    }
}
