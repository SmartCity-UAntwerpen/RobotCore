package be.uantwerpen.sc.services;

import be.uantwerpen.sc.RobotCoreLoop;
import be.uantwerpen.sc.controllers.CCommandSender;
import be.uantwerpen.sc.models.Job;
import be.uantwerpen.sc.tools.DriveDir;
import be.uantwerpen.sc.tools.NavigationParser;
import be.uantwerpen.sc.tools.Terminal;
import be.uantwerpen.sc.tools.WorkingmodeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Thomas on 01/06/2016.
 */
@Service
public class JobService
{
    @Value("${sc.core.ip:localhost}")
    private String serverIP;

    @Value("#{new Integer(${sc.core.port}) ?: 1994}")
    private int serverPort;

    @Autowired
    private DataService dataService;

    @Autowired
    private QueueService queueService;

    @Autowired
    private CCommandSender sender;

    private RobotCoreLoop robotCoreLoop;

    private int endJob;

    private Long jobid;

    public int getEndJob(){
        return endJob;
    }

    public void setEndJob(int endOfJob){
        endJob=endOfJob;
    }

    public Long jobid(){
        return jobid;
    }

    public void getJobid(Long jobid){
        this.jobid=jobid;
    }

    public void setRobotCoreLoop(RobotCoreLoop robotCoreLoop)
    {
        this.robotCoreLoop = robotCoreLoop;
    }

    public void parseJob(String job) throws ParseException
    {
        if(!job.startsWith("Job:{jobId:") || job.split("/ ", 2).length <= 1)
        {
            //Not a valid job string
            throw new ParseException("Can not parse job from: " + job + "\nInvalid type!", 0);
        }
        Long oldjobid=jobid;
        try
        {
            String partialstring=job.split(":",3 )[0];
            partialstring=partialstring.split("/",2)[0];
            jobid = Long.parseLong(partialstring);
            String jobDescription = job.split("/ ", 3)[2];

            if(!jobDescription.startsWith("idstart:"))
            {
                //Not a valid job string
                throw new ParseException("Can not parse job from: " + job + "\nInvalid field!", 0);
            }

            Job parsedJob = new Job(jobid, jobDescription);

            performJob(parsedJob);
        }
        catch(Exception e)
        {
            jobid=oldjobid;
            //Could not parse job from string
            throw new ParseException("Can not parse job from: " + job + "\nInvalid format!", 0);
        }
    }

    private void performJob(Job job)
    {
        String jobDescription = job.getJobDescription();

        System.out.println("JOB DESCRIPTION: " + jobDescription);

        switch(jobDescription.split(" ", 2)[0].toLowerCase())
        {
            case "navigate":
                try {
                    String end = jobDescription.split(" ", 2)[1].toLowerCase();
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
                            break;
                    }
                }catch(ArrayIndexOutOfBoundsException e){
                    Terminal.printTerminalInfo("Usage: navigate end");
                }
                break;
            case "playaudio":
                sender.sendCommand("SPEAKER UNMUTE");
                sender.sendCommand("SPEAKER PLAY QMusic");
                try{
                    Thread.sleep(1000);
                }catch (Exception e){
                    e.printStackTrace();
                }
                sender.sendCommand("SPEAKER PLAY cantina");
                break;
            default:
                System.out.println("Unknown job description: " + jobDescription);
        }
    }

    private void startPathPlanning(int end2){
        dataService.locationUpdated = false;
        while(!dataService.locationUpdated){
            //Wait
            try {
                //Read tag
                queueService.insertJob("TAG READ UID");
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Terminal.printTerminal("Starting pathplanning from point " + dataService.getCurrentLocation() + " to " + end2);
        dataService.navigationParser = new NavigationParser(robotCoreLoop.pathplanning.Calculatepath(dataService.map, (int)(long)dataService.getCurrentLocation(), end2), dataService);
        //Parse Map
        dataService.navigationParser.parseMap();
        //dataService.navigationParser.parseRandomMap(dataService);

        removeDriveCommands();

        //Setup for driving
        int start = (int)(long)dataService.navigationParser.list.get(0).getId();
        int end = (int)(long)dataService.navigationParser.list.get(1).getId();
        dataService.setNextNode((long)end);
        dataService.setPrevNode((long)start);
        queueService.insertJob("DRIVE FOLLOWLINE");
        queueService.insertJob("DRIVE FORWARD 110");

        //Process map
        for (DriveDir command : dataService.navigationParser.commands) {
            queueService.insertJob(command.toString());
        }
    }

    public void startPathRobotcore(int end){

        //ask robotcore for instructions
        RestTemplate restTemplate = new RestTemplate();
        DriveDir[] nextPath = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/map/"
                +dataService.getCurrentLocation()+"/path/"+end, DriveDir[].class);

        removeDriveCommands();

        //Process map
        for (DriveDir command : nextPath) {
            queueService.insertJob(command.toString());
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

    public void removeCommands(){
        BlockingQueue<String> content = queueService.getContentQueue();
        ArrayList<String> contentcopy = new ArrayList<String>();
        content.drainTo(contentcopy);
        Terminal.printTerminal(contentcopy.toString());
        contentcopy.clear();
        queueService.setContentQueue(content);
    }


}
