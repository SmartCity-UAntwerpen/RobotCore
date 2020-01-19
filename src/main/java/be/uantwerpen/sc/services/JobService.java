package be.uantwerpen.sc.services;

import be.uantwerpen.rc.models.map.Point;
import be.uantwerpen.rc.tools.DriveDir;
import be.uantwerpen.rc.tools.DriveDirEncapsulator;
import be.uantwerpen.rc.tools.helpers.JobAdapter;
import be.uantwerpen.sc.RobotCoreLoop;
import be.uantwerpen.sc.controllers.DriverCommandSender;
import be.uantwerpen.rc.models.Job;
import be.uantwerpen.sc.tools.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Thomas on 01/06/2016.
 */
@Service
public class JobService
{
    @Value("${sc.backend.ip:localhost}")
    private String serverIP;

    @Value("#{new Integer(${sc.backend.port}) ?: 1994}")
    private int serverPort;

    @Autowired
    private DataService dataService;

    @Autowired
    private QueueService queueService;

    @Autowired
    private DriverCommandSender sender;

    @Autowired
    private RobotCoreLoop robotCoreLoop;

    private Logger logger = LoggerFactory.getLogger(JobService.class);
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

    public void parseJob(String jsonJob) throws ParseException
    {
        logger.info("Parsing job...");
        System.out.println("Job String: \n " + jsonJob);

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Job.class, new JobAdapter());
        Gson gson = gsonBuilder.create();
        Job job = gson.fromJson(jsonJob, Job.class);

        logger.info("Parsed: jobid = " + job.getJobId() + " startid = " + job.getIdStart() + " endid = " + job.getIdEnd());

        if(!(dataService.getCurrentLocation().equals(job.getIdEnd()) && dataService.getCurrentLocation().equals(job.getIdStart()))) {
            dataService.setJob(job);
        } else {
            logger.info("Already on destination");
            //let the backend know that the job is finished
            sendFinished();
        }
        logger.info("job parsed");
    }

    public void performJob(Job job)
    {
        int startInt = job.getIdStart().intValue();
        int endInt = job.getIdEnd().intValue();
        logger.info("Performing job with destination: "+endInt);
        switch(dataService.getWorkingmodeEnum()) {
            case INDEPENDENT:
                try {
                    dataService.setRobotDriving(true);
                    dataService.setTempJob(false);
                    if(!dataService.getCurrentLocation().equals(job.getIdStart())) {
                        logger.info("start location not current Location. Going to " + job.getIdStart());
                        dataService.setDestination(job.getIdStart());
                        dataService.setTempJob(true);
                        this.startIndependentPathPlanning(startInt);
                        logger.info("Wait till tempjob is finished");
                        while(!dataService.getCurrentLocation().equals(job.getIdStart())) {
                            Thread.sleep(1000);
                        }
                        dataService.setTempJob(false);
                        dataService.setDestination(job.getIdEnd());
                        //this.startIndependentPathPlanning(endInt);
                    } else {
                        dataService.setTempJob(false);
                        dataService.setDestination(job.getIdEnd());
                        this.startIndependentPathPlanning(endInt);
                    }
                } catch (NumberFormatException e) {
                    logger.info(e.getMessage());
                    logger.info("Usage: navigate end");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case FULLSERVER:
                try{
                    dataService.setRobotDriving(true);
                    dataService.setTempJob(false);
                    if(!dataService.getCurrentLocation().equals(job.getIdStart()))
                    {
                        logger.info("Start location not equal to current location. Going to " + job.getIdStart());
                        dataService.setDestination(job.getIdStart());
                        dataService.setTempJob(true);
                        this.startFullServerPathPlanning(startInt, job);
                        logger.info("Wait till tempjob is finished.");
                        while(!dataService.getCurrentLocation().equals(job.getIdStart()))
                        {
                            Thread.sleep(1000);
                        }
                        dataService.setTempJob(false);
                        dataService.setDestination(job.getIdEnd());
                        //this.startFullServerPathPlanning(endInt, job);
                    }
                    else
                    {
                        logger.info("Start location equal to current location. Going to " + job.getIdStart());
                        dataService.setTempJob(false);
                        dataService.setDestination(job.getIdEnd());
                        this.startFullServerPathPlanning(endInt, job);
                    }

                }catch (NumberFormatException e) {
                    logger.info(e.getMessage());
                    logger.info("Usage: navigate end");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //TODO add full server and partial server
        }
    }

    private void startIndependentPathPlanning(int end){
        logger.info("Starting pathplanning from point " + dataService.getCurrentLocation() + " to " + end);
        //first retrieve the most updated version of the map (weights are dynamic)
        getUpdatedMap();
        List<Point> temp = robotCoreLoop.getPathplanningService().Calculatepath(dataService.getMap(), (int)(long)dataService.getCurrentLocation(), end);
        for(Point point: temp) logger.info("Point in path to be followed: " + point.getId());
        dataService.setNavigationParser(new NavigationParser(temp, dataService));
        //Parse Map
        dataService.getNavigationParser().parseMap();
        //Setup for driving
        dataService.setRobotDriving(true);
        //Process map
        for (DriveDir command : dataService.getNavigationParser().getCommands()) {
            logger.info("insert job " + command.toString());
            queueService.insertCommand(command.toString());
        }
    }

    private void startFullServerPathPlanning(int end, Job job)
    {
        logger.info("Get pathplanning from backend starting from point " + dataService.getCurrentLocation() + " to " + end);
        for(DriveDir command: job.getDriveDirections())
        {
            logger.info("insert job " + command.toString());
            queueService.insertCommand(command.toString());
        }


    }


    private void getUpdatedMap() {
        logger.info("Receiving updated map...");
        robotCoreLoop.getMap();
    }

    private void sendFinished() {
        RestTemplate restTemplate = new RestTemplate();
        while(true) {
            try {
                restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/job/finished/" + dataService.getRobotID()
                        , Void.class);
                break;
            } catch(RestClientException e ) {
                logger.error("Can't connect to the database to send job finished, retrying...");
                try {
                    Thread.sleep(500);
                } catch(InterruptedException er) {
                    er.printStackTrace();
                }
            }
        }
    }

    private void pathNotFound() {
        RestTemplate restTemplate = new RestTemplate();
        while(true) {
            try {
                logger.info("Sending path not found");
                restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/job/pathError/" + dataService.getRobotID()
                        , Void.class);
                break;
            } catch(RestClientException e ) {
                logger.error("Can't connect to the backend to send path not found, retrying...");
                try {
                    Thread.sleep(500);
                } catch(InterruptedException er) {
                    er.printStackTrace();
                }
            }
        }
    }

    public void startPathRobotcore(int start, int end){


        //ask robotcore for instructions
        RestTemplate restTemplate = new RestTemplate();
        DriveDirEncapsulator nextPath = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/map/getdirections/"
                +"/" + start + "/" + end, DriveDirEncapsulator.class);

        //new job so remove drive commands from possible earlier job
        removeDriveCommands();

        //Process map
        for (int i = 0; i < nextPath.getDriveDirs().size();i++) {
            Terminal.printTerminal("Partial server command: " + nextPath.getDriveDirs().get(i).toString());
            queueService.insertCommand(nextPath.getDriveDirs().get(i).toString());
        }

    }

    public void startPathRobotcoreNg(int start, int end){

        /*
        //ask robotcore for instructions
        RestTemplate restTemplate = new RestTemplate();
        DriveDirEncapsulator nextPath = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/map/getdirections/"
                +"/" + start + "/" + end, DriveDirEncapsulator.class);

        //new job so remove drive commands from possible earlier job
        removeDriveCommands();

        //Process map
        for (int i = 0; i < nextPath.getDriveDirs().size();i++) {
            Terminal.printTerminal("Partial server command: " + nextPath.getDriveDirs().get(i).toString());
            queueService.insertCommand(nextPath.getDriveDirs().get(i).toString());
        }
        */

        RestTemplate restTemplate = new RestTemplate();
        DriveDirEncapsulator nextPath = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/map/getdirectionsng/"
                +"/" + start + "/" + end, DriveDirEncapsulator.class);

        //new job so remove drive commands from possible earlier job
        removeDriveCommands();

        //Process map
        for (int i = 0; i < nextPath.getDriveDirs().size();i++) {
            Terminal.printTerminal("Partial server command: " + nextPath.getDriveDirs().get(i).toString());
            queueService.insertCommand(nextPath.getDriveDirs().get(i).toString());
        }
    }

    public void startPathFullRobotcore(int start, int end){
        //ask robotcore for instructions
        RestTemplate restTemplate = new RestTemplate();
        DriveDirEncapsulator nextPath = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/map/getnexthop/"
                + start + "/" + dataService.getCurrentLocation() + "/" + end, DriveDirEncapsulator.class);

        for (int i = 0; i < nextPath.getDriveDirs().size();i++) {
            queueService.insertCommand(nextPath.getDriveDirs().get(i).toString());
        }
    }

    public void removeDriveCommands() {
        //remove drive jobs from queue
        Terminal.printTerminal("remove commands");
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