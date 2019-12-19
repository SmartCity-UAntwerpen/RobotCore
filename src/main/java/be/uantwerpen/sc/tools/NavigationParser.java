package be.uantwerpen.sc.tools;

import be.uantwerpen.rc.models.map.Link;
import be.uantwerpen.rc.models.map.Map;
import be.uantwerpen.rc.models.map.Point;
import be.uantwerpen.rc.tools.DriveDir;
import be.uantwerpen.rc.tools.DriveDirEnum;
import be.uantwerpen.sc.services.DataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.List;

/**
 * Created by Arthur on 28/04/2016.
 *
 * @Author Riad on 12/12/2019
 */
public class NavigationParser {

    private Logger logger = LoggerFactory.getLogger(NavigationParser.class);

    @Autowired
    private DataService dataService;

    private List<Point> path;
    private Queue<DriveDir> commands = new LinkedList<DriveDir>();

    public NavigationParser(List<Point> path, DataService dataservice){
        this.path = path;
        this.dataService=dataservice;
    }

    public List<Point> getPath(){
        return path;
    }

    public void decideOnCrossing(Point current, Point next) {
        for(Link link: current.getNeighbours()) {
            if(link.getStartPoint().getId().equals(current.getId()) && link.getEndPoint().getId().equals(next.getId())) {
                if(link.getAngle() > -181.0 && link.getAngle() < 181.0)
                    if(link.getAngle() <= 0.0001 && link.getAngle() >= -0.0001) {
                        //if the length of the path is 0 we assume it's a crossroad
                        if(link.getCost().getLength() == 0) {
                            commands.add(new DriveDir(DriveDirEnum.FORWARD));
                        } else {
                            // execute follow line after each crossroad
                            commands.add(new DriveDir(DriveDirEnum.FOLLOW));
                        }
                    } else if(Math.abs(link.getAngle()) <= 180.0001 && Math.abs(link.getAngle()) >= 179.9999) {
                        commands.add(new DriveDir(DriveDirEnum.TURN));
                    } else {
                        if(link.getAngle() > 0)
                            commands.add(new DriveDir(DriveDirEnum.RIGHT, link.getAngle()));
                        else
                            commands.add(new DriveDir(DriveDirEnum.LEFT, Math.abs(link.getAngle())));
                    }
                break;
            }

        }
    }

    public Queue<DriveDir> parseMap(){
        if(path.isEmpty()){
            logger.warn("Cannot parse empty path");
        }else {
            Point current;
            Point driveTo;
            Point next;
            for(int i = 0;  i < path.size() - 1; i++) {
                current = path.get(i);
                driveTo = path.get(i+1);
                if(i+2  < path.size())
                    next = path.get(i+2);
                else
                    next = driveTo;
                Long linkId = (long) -1;
                for(Link link : current.getNeighbours()) {
                    if(link.getStartPoint().getId().equals(current.getId()) && link.getEndPoint().getId().equals(driveTo.getId())) {
                        linkId = link.getId();
                        break;
                    }
                }
                commands.add(new DriveDir("REQUEST LOCKS "+driveTo.getId() + " " + linkId));
                switch (dataService.getMap().getPointById(path.get(i).getId()).getTile().getType().toLowerCase()) {
                    case "crossing":
                            //Check at what angle the crossroad needs to be passed
                           decideOnCrossing(current, driveTo);
                        break;
                    case "tlight":
                        //followline is needed to continue driving, forward is needed to get over the gap
                        commands.add(new DriveDir("TRAFFICLIGHT DETECTION "+current.getId()));
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
                float temp = i+1;
                float progress = temp/path.size() * 100;
                if(dataService.getTempJob())
                    progress = 0;
                commands.add(new DriveDir("UPDATE LOCATION"+" "+driveTo.getId()+" "+next.getId()+ " " + progress));
                commands.add(new DriveDir("RELEASE LOCKS " + current.getId() + " " + linkId));
                // needed to relock the tile since points on crosspoints reffer to same tile -> same lock
                commands.add(new DriveDir("RELOCK TILE " + driveTo.getId()));
            }
            if(dataService.getMap().getPointById(path.get(path.size()-1).getId()).getTile().getType().toLowerCase().equals("end")) {
                //last point -> park
                commands.add(new DriveDir("SPEAKER UNMUTE"));
                commands.add(new DriveDir("SPEAKER SAY PARKING"));
                commands.add(new DriveDir("DRIVE ROTATE R 180"));
                commands.add(new DriveDir("SPEAKER SAY BEEP BEEP BEEP BEEP"));
                commands.add(new DriveDir("DRIVE BACKWARDS 150"));
            }
            commands.add((new DriveDir("SEND LOCATION")));
            if(!dataService.getTempJob()) {
                commands.add(new DriveDir("FINISH JOB"));
            }


        }
        return commands;
    }

    public DataService getDataService() {
        return dataService;
    }

    public void setDataService(DataService dataService) {
        this.dataService = dataService;
    }

    public void setPath(List<Point> path) {
        this.path = path;
    }

    public Queue<DriveDir> getCommands() {
        return commands;
    }

    public void setCommands(Queue<DriveDir> commands) {
        this.commands = commands;
    }
}
