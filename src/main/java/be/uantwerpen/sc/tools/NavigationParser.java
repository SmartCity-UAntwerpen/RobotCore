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
    public List<Vertex> list;
    public Queue<DriveDir> commands = new LinkedList<DriveDir>();

    public NavigationParser(List<Vertex> list, DataService dataservice){
        this.list = list;
        this.dataService=dataservice;
    }

    public List<Vertex> getList(){
        return list;
    }

    public Queue<DriveDir> parseMap(){
        if(list.isEmpty()){
            Terminal.printTerminalError("Cannot parse empty map");
        }else{
            //First part is always driving forward.
            commands.add(new DriveDir(DriveDirEnum.FOLLOW));
            //Second part is parsing the rest of the map
            Vertex current = list.get(0);
            System.out.println("current: "+current);
            Vertex previous = list.get(0);
            Vertex next = list.get(1);
            System.out.println("next: "+next+"\n");
            if(list.size()==2){
                System.out.println(dataService.getPrevNode());
                System.out.println(dataService.getNextNode());
                DriveDir relDir=null;
            }else{
                for(int i = 2; i < list.size(); i++) {
                    previous = current;
                    current = next;
                    next = list.get(i);
                    System.out.println("previous: " + previous + " current: " + current + " next: " + next);
                    //Drive followLine
                    commands.add(new DriveDir(DriveDirEnum.FOLLOW));
                }
            }
        }
        //commands.add(new DriveDir((DriveDirEnum.TURN)));
        Terminal.printTerminal("Commands added");
        return commands;
    }
}
