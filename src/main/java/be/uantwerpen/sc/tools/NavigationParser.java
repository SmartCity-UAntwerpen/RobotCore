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

    public void decideOnCrossing(Vertex current, Vertex next) {
        for(Edge edge: current.getAdjacencies()) {
            if(edge.getLinkEntity().getStartPoint().getId() == current.getId() && edge.getLinkEntity().getEndPoint().getId() == next.getId()) {
                if(edge.getLinkEntity().getAngle() > -181 && edge.getLinkEntity().getAngle() < 181)
                    if(edge.getLinkEntity().getAngle() == 0) {
                        //if the length of the path is 0 we assume it's a crossroad
                        if(edge.getLinkEntity().getLength() == 0) {
                            commands.add(new DriveDir(DriveDirEnum.FORWARD));
                        } else {
                            // execute follow line after each crossroad
                            commands.add(new DriveDir(DriveDirEnum.FOLLOW));
                        }
                    } else if(Math.abs(edge.getLinkEntity().getAngle()) == 180) {
                        commands.add(new DriveDir(DriveDirEnum.TURN));
                    } else {
                        if(edge.getLinkEntity().getAngle() > 0)
                            commands.add(new DriveDir(DriveDirEnum.RIGHT, edge.getLinkEntity().getAngle()));
                        else
                            commands.add(new DriveDir(DriveDirEnum.LEFT, Math.abs(edge.getLinkEntity().getAngle())));
                    }
                break;
            }

        }
    }

    public Queue<DriveDir> parseMap(){
        if(path.isEmpty()){
            Terminal.printTerminalError("Cannot parse empty map");
        }else {
            Vertex current;
            Vertex driveTo;
            Vertex next;
            for(int i = 0;  i < path.size() - 1; i++) {
                current = path.get(i);
                driveTo = path.get(i+1);
                if(i+2  < path.size())
                    next = path.get(i+2);
                else
                    next = driveTo;
                Long linkId = new Long(-1);
                for(Edge edge : current.getAdjacencies()) {
                    if(edge.getLinkEntity().getStartPoint().getId() == current.getId() && edge.getLinkEntity().getEndPoint().getId() == driveTo.getId()) {
                        linkId = edge.getLinkEntity().getId();
                        break;
                    }
                }
                commands.add(new DriveDir("REQUEST LOCKS "+driveTo.getId() + " " + linkId));
                switch (dataService.map.getPointById(path.get(i).getId()).getTile().getType().toLowerCase()) {
                    case "crossing":
                            //Check at what angle the crossroad needs to be passed
                           decideOnCrossing(current, driveTo);
                        break;
                    case "tlight":
                        //followline is needed to continue driving, forward is needed to get over the gap
                        commands.add(new DriveDir(DriveDirEnum.FORWARD));
                        commands.add(new DriveDir(DriveDirEnum.FOLLOW));

                        break;
                    case "end":
                        if(i == 0) {
                            commands.add(new DriveDir(DriveDirEnum.FOLLOW));
                            commands.add(new DriveDir(DriveDirEnum.FORWARD));
                            commands.add(new DriveDir(DriveDirEnum.FOLLOW));
                        } else {
                            commands.add(new DriveDir(DriveDirEnum.LONGDRIVE));
                            commands.add(new DriveDir(DriveDirEnum.TURN));
                            commands.add(new DriveDir(DriveDirEnum.FOLLOW));
                            commands.add(new DriveDir(DriveDirEnum.FORWARD));
                        }
                        break;
                    default:
                }
                commands.add(new DriveDir("UPDATE LOCATION"+" "+driveTo.getId()+" "+next.getId()));
                commands.add(new DriveDir("RELEASE LOCKS " + current.getId() + " " + linkId));


            }
            if(dataService.map.getPointById(path.get(path.size()-1).getId()).getTile().getType().toLowerCase().equals("end")) {
                //last point -> park
                commands.add(new DriveDir("SPEAKER UNMUTE"));
                commands.add(new DriveDir("SPEAKER SAY PARKING"));
                commands.add(new DriveDir("DRIVE ROTATE R 180"));
                commands.add(new DriveDir("SPEAKER SAY BEEP BEEP BEEP BEEP"));
                commands.add(new DriveDir("DRIVE BACKWARDS 150"));
            }
            commands.add((new DriveDir("SEND LOCATION")));


        }
        return commands;
    }
}
