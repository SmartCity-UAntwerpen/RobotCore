package be.uantwerpen.sc.services;

import be.uantwerpen.rc.models.map.Point;
import be.uantwerpen.rc.tools.DriveDir;
import be.uantwerpen.sc.RobotCoreLoop;
import be.uantwerpen.sc.controllers.DriverCommandSender;
import be.uantwerpen.sc.controllers.PathController;
import be.uantwerpen.rc.models.map.Path;
import be.uantwerpen.sc.tools.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Thomas on 14/04/2016.
 */
/*
commandos ingeven, bepaalde dingen printen
 */
@Service
public class TerminalService
{
    private Terminal terminal;

    @Value("${sc.backend.ip:localhost}")
    private String serverIP;

    @Value("#{new Integer(${sc.backend.port}) ?: 1994}")
    private int serverPort;

    @Autowired
    private PathController pathController;
    @Autowired
    private DriverCommandSender sender;
    @Autowired
    private QueueService queueService;
    @Autowired
    private DataService dataService;

    private RobotCoreLoop robotCoreLoop;

    private boolean activated;

    public TerminalService()
    {
        terminal = new Terminal()
        {
            @Override
            public void executeCommand(String commandString)
            {
                parseCommand(commandString);
            }
        };
        activated=false;
    }

    public boolean getActivated(){
        return activated;
    }

    public void setActivated(boolean act){
        activated=act;
    }

    public void systemReady()
    {
        terminal.printTerminal("\nSmartCar Core [Version " + getClass().getPackage().getImplementationVersion() + "]\n(c) 2015-2017 University of Antwerp. All rights reserved.");
        terminal.printTerminal("Type 'help' to display the possible commands.");

        terminal.activateTerminal();
        activated=true;
    }

    @Deprecated
    public void setRobotCoreLoop(RobotCoreLoop robotCoreLoop)
    {
        this.robotCoreLoop = robotCoreLoop;
    }

    private void parseCommand(String commandString)
    {
        String command = commandString.split(" ", 2)[0].toLowerCase();

        switch(command)
        {
            case "navigate":
                try {
                    String end = commandString.split(" ", 2)[1].toLowerCase();
                    switch(dataService.getWorkingmodeEnum()) {
                        case INDEPENDENT:
                            try {
                                int endInt = Integer.parseInt(end);
                                startPathPlanning(endInt);
                            } catch (NumberFormatException e) {
                                Terminal.printTerminalError(e.getMessage());
                                Terminal.printTerminalInfo("Usage: navigate end");
                            }
                            break;
                        case PARTIALSERVER:
                            try {
                                int endInt = Integer.parseInt(end);
                                startPathRobotcore(endInt);
                            } catch (NumberFormatException e) {
                                Terminal.printTerminalError(e.getMessage());
                                Terminal.printTerminalInfo("Usage: navigate end");
                            }
                            break;
                        case FULLSERVER:
                            try {
                                int endInt = Integer.parseInt(end);
                                while(dataService.getCurrentLocation()!=endInt)
                                    if(queueService.getContentQueue().size() == 0){
                                        startPathFullRobotcore(endInt);
                                    }
                            } catch (NumberFormatException e) {
                                Terminal.printTerminalError(e.getMessage());
                                Terminal.printTerminalInfo("Usage: navigate end");
                            }
                            break;
                    }
                    /*try {
                        int endInt = Integer.parseInt(end);
                        startPathPlanning(endInt);
                    } catch (NumberFormatException e) {
                        terminal.printTerminalError(e.getMessage());
                        terminal.printTerminalInfo("Usage: navigate end");
                    }*/
                }catch(ArrayIndexOutOfBoundsException e){
                    Terminal.printTerminalInfo("Usage: navigate end");
                }
                break;
            case "path":
                try {
                    String command2 = commandString.split(" ", 2)[1].toLowerCase();

                    String start = command2.split(" ", 2)[0].toLowerCase();
                    String end = command2.split(" ", 2)[1].toLowerCase();
                    if (start.equals(end)) {
                        Terminal.printTerminalInfo("Start cannot equal end.");
                    } else if (start.equals("") || end.equals("")) {
                        Terminal.printTerminalInfo("Usage: navigate start end");
                    } else {
                        try {
                            int startInt = Integer.parseInt(start);
                            int endInt = Integer.parseInt(end);
                            getPath(startInt, endInt);
                        } catch (NumberFormatException e) {
                            Terminal.printTerminalError(e.getMessage());
                            Terminal.printTerminalInfo("Usage: navigate start end");
                        }
                    }
                }catch(ArrayIndexOutOfBoundsException e){
                    Terminal.printTerminalError("Usage: navigate start end");
                }
                break;
            case "random":
                try {
                    getRandomPath();
                }catch(ArrayIndexOutOfBoundsException e){
                    Terminal.printTerminalError("Usage: navigate start end");
                }
                break;
            case "sendcommand":
                try {
                    String command2 = commandString.split(" ", 2)[1].toUpperCase();
                    //No override
                    //queueService.insertCommand(command2);
                    System.out.println("sending command :" + command2);
                    sender.sendCommand(command2);
                    //Override
                    //sender.sendCommand(command2);
                }catch(ArrayIndexOutOfBoundsException e){
                    Terminal.printTerminalInfo("Usage: navigate start end");
                }
                break;
            case "domusic":
                sender.sendCommand("SPEAKER UNMUTE");
                sender.sendCommand("SPEAKER PLAY QMusic");
                try{
                    Thread.sleep(1000);
                }catch (Exception e){
                    e.printStackTrace();
                }
                sender.sendCommand("SPEAKER PLAY cantina");
                break;
            case "stopmusic":
                sender.sendCommand("SPEAKER STOP");
                break;
            case "checkqueue":
                try {
                    System.out.println(queueService.getContentQueue().toString());
                }catch(ArrayIndexOutOfBoundsException e){
                    Terminal.printTerminalError("error");
                }
                break;
            case "exit":
                exitSystem();
                break;
            case "help":
            case "?":
                printHelp("");
                break;
            default:
                Terminal.printTerminalInfo("Command: '" + command + "' is not recognized.");
                break;
        }
    }

    private void exitSystem()
    {
        //aan RobotBackend late weten dat bot shutdownt
        RestTemplate resttemplate = new RestTemplate();
        resttemplate.getForObject("http://" + serverIP + ":" + serverPort + "/bot/delete/"
                + dataService.getRobotID(), void.class);


        System.exit(0);
    }

    private void printHelp(String command)
    {
        switch(command)
        {
            default:
                terminal.printTerminal("Available commands:");
                terminal.printTerminal("-------------------");
                terminal.printTerminal("'navigate {end}': navigates the robot from point {start} to {end}");
                terminal.printTerminal("'path {start} {end}': get the path from the server");
                terminal.printTerminal("'random': get random path from the server from current location");
                terminal.printTerminal("'simulate {true/false}': activate he simulator");
                terminal.printTerminal("'checkQueue': check content of the queue");
                terminal.printTerminal("'exit' : shutdown the core.");
                terminal.printTerminal("'help' / '?' : show all available commands.\n");
                break;
        }
    }

    private void startPathPlanning(int end2){
        dataService.setLocationUpdated(false);
        while(!dataService.getLocationUpdated()){
            //Wait
            try {
                //Read tag
                queueService.insertCommand("TAG READ UID");
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Terminal.printTerminal("Starting pathplanning from point " + dataService.getCurrentLocation() + " to " + end2);
        dataService.setNavigationParser(new NavigationParser(robotCoreLoop.getPathplanningService().Calculatepath(dataService.getMap(), (int)(long)dataService.getCurrentLocation(), end2), dataService));
        //Parse Map
        dataService.getNavigationParser().parseMap();
        //dataService.navigationParser.parseRandomMap(dataService);

        //Setup for driving
        int start = (int)(long)dataService.getNavigationParser().getPath().get(0).getId();
        int end = (int)(long)dataService.getNavigationParser().getPath().get(1).getId();
        dataService.setNextNode((long)end);
        dataService.setPrevNode((long)start);
        queueService.insertCommand("DRIVE FOLLOWLINE");
        queueService.insertCommand("DRIVE FORWARD 110");

        //Process map
        for (DriveDir command : dataService.getNavigationParser().getCommands()) {
            queueService.insertCommand(command.toString());
        }
    }

    private void getPath(int start, int end){
        Path path = pathController.getPath(start, end);
        System.out.println(path.toString());
    }

    private void getRandomPath(){
        int currentLocation = (int)(long)dataService.getCurrentLocation();
        if(currentLocation < 0) {
            currentLocation = 4;
        }
        List<Point> path = pathController.getRandomPath(currentLocation).getPath();
        NavigationParser navigationParser = new NavigationParser(path, dataService);

    }

    public void startPathRobotcore(int end){

        //ask robotcore for instructions
        RestTemplate restTemplate = new RestTemplate();
        DriveDir[] nextPath = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/map/"
                +dataService.getCurrentLocation()+"/path/"+end, DriveDir[].class);

        removeDriveCommands();

        //Process map
        for (DriveDir command : nextPath) {
            queueService.insertCommand(command.toString());
        }
    }

    public void startPathFullRobotcore(int end){
        //ask robotcore for instructions
        RestTemplate restTemplate = new RestTemplate();
        DriveDir[] nextPath = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/map/"
                +dataService.getCurrentLocation()+"/path/"+end, DriveDir[].class);

        //Process map but only 2 first commands
        for (int i=0;i<2;i++) {
            queueService.insertCommand(nextPath[i].toString());
        }
    }

    public void removeDriveCommands() {
        //remove drive jobs from queue
        BlockingQueue<String> content = queueService.getContentQueue();
        ArrayList<String> contentcopy = new ArrayList<String>();
        content.drainTo(contentcopy);
        String comm;
        while (contentcopy.size() > 0) {
            comm = contentcopy.get(0);
            Terminal.printTerminal(comm);
            if (!comm.matches("DRIVE (.*)")) {
                content.add(comm);
            }
        }
        queueService.setContentQueue(content);
    }
}