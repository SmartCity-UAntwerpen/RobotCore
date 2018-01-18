package be.uantwerpen.sc.tools;

import be.uantwerpen.sc.controllers.CCommandSender;
import be.uantwerpen.sc.services.DataService;
import be.uantwerpen.sc.services.QueueService;
import org.mockito.internal.matchers.Null;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.expression.spel.ast.NullLiteral;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.BlockingQueue;

/**
 * Created by Niels on 4/05/2016.
 */

public class QueueConsumer implements Runnable
{

    @Value("${sc.core.ip:localhost}") //values won't be loaded beceause QueueConsumer is created with "new" in systemloader
    private String serverIP;

    @Value("#{new Integer(${sc.core.port}) ?: 1994}")
    private int serverPort;

    private CCommandSender sender;
    private QueueService queueService;
    private DataService dataService;

    private boolean lockGranted = false;
    //private boolean first = true;
    private int prevQueueSize = 0;

    private BlockingQueue<String> jobQueue;

    public QueueConsumer(QueueService queueService, CCommandSender sender, DataService dataService, String serverIP, int serverPort)
    {
        this.queueService = queueService;
        this.sender = sender;
        this.dataService = dataService;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    @Deprecated
    public void setServerCoreIP(String ip, int port)
    {

    }

    @Override
    public void run() {
        int i = 1;
        while (!Thread.currentThread().isInterrupted()) {
            try {

                if((dataService.getCurrentLocation() == dataService.getDestination()) && (dataService.getDestination() != -1L) && (dataService.getCurrentLocation() != -1L)){
                    Terminal.printTerminal("Current location : " + dataService.getCurrentLocation() + " destination : " + dataService.getDestination() + " tempjob : " + dataService.tempjob);

                    if(!dataService.tempjob){ //end of total job
                        Park();
                        Terminal.printTerminal("Total job Finished");
                        dataService.robotDriving = false;

                        RestTemplate restTemplate = new RestTemplate(); //standaard resttemplate gebruiken
                        restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/job/finished/" + dataService.getRobotID()//aan de server laten weten dat er een nieuwe bot zich aanbied
                                , Void.class); //Aan de server laten weten in welke mode de bot werkt

                        dataService.setDestination(-1L);
                        dataService.firstOfQueue = true;
                        dataService.jobfinished = true;
                    }else{ //end of temp job
                        Terminal.printTerminal("Temp job finished");
                        Park();
                        dataService.setDestination(-1L);
                        dataService.robotDriving = false;
                        dataService.tempjob = false;
                        dataService.firstOfQueue = true;

                    }

                    dataService.executingJob = false;
                    i = 1;
                }

            if(queueService.getContentQueue().size() == 0){

            }else{
                if(!dataService.robotBusy && (dataService.getWorkingmodeEnum() != null)){

                    switch(dataService.getWorkingmodeEnum()){
                        case INDEPENDENT:
                            Terminal.printTerminal("case independent");
                            if(dataService.firstOfQueue){
                                RequestLock();
                            }

                            Terminal.printTerminal("Robot not busy");
                            Terminal.printTerminal(queueService.getContentQueue().toString());
                            String s = queueService.getJob();
                            Terminal.printTerminal("Sending: " + s);
                            sender.sendCommand(s);

                            dataService.robotBusy = true;
                            Terminal.printTerminal("DRIVING.........");
                            if(s.contains("DRIVE FOLLOWLINE")){
                                while(dataService.robotBusy){ //wait till drive event is finished
                                }

                                if(dataService.firstOfQueue){
                                    Terminal.printTerminal("First followlin of queue");
                                    dataService.firstOfQueue = false;
                                }else{
                                    i++;
                                    Terminal.printTerminal("Current = " + dataService.getCurrentLocation() + " next = " + dataService.getNextNode() + " prev = " + dataService.getPrevNode());

                                    if(i < dataService.navigationParser.list.size()){
                                        dataService.setPrevNode(dataService.getCurrentLocation());
                                        dataService.setCurrentLocation(dataService.getNextNode());
                                        dataService.setNextNode(dataService.navigationParser.list.get(i).getId());
                                        RequestLock();
                                        ReleaseLock();

                                    }else{
                                        dataService.setPrevNode(dataService.getCurrentLocation());
                                        ReleaseLock(dataService.getCurrentLocation());
                                        dataService.setCurrentLocation(dataService.getNextNode());
                                        dataService.firstLink();

                                    }

                                    Terminal.printTerminal("Current = " + dataService.getCurrentLocation() + " next = " + dataService.getNextNode() + " prev = " + dataService.getPrevNode());
                                    Terminal.printTerminal("Current = " + dataService.getCurrentLocation() + " destination = " + dataService.getDestination());
                                    Terminal.printTerminal("");
                                }
                            }else{

                                while(dataService.robotBusy){//wait till drive event is finished
                                }

                            }
                            break;
                     default:
                         Terminal.printTerminal("Robot not busy");
                         Terminal.printTerminal(queueService.getContentQueue().toString());
                         String c = queueService.getJob();
                         Terminal.printTerminal("Sending: " + c);
                         sender.sendCommand(c);

                         dataService.robotBusy = true;
                         Terminal.printTerminal("DRIVING.........");
                         if(c.contains("DRIVE FOLLOWLINE")){
                             while(dataService.robotBusy){//wait till drive event is finished
                             }
                             dataService.readTag();
                         }else{

                             while(dataService.robotBusy){//wait till drive event is finished
                             }

                         }
                         break;
                    }

                }
            }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void Park(){
        try{
            Terminal.printTerminal("Parking");

            sender.sendCommand("SPEAKER UNMUTE");
            sender.sendCommand("SPEAKER SAY PARKING");

            dataService.robotBusy = true;
            sender.sendCommand("DRIVE ROTATE R 180");
            while(dataService.robotBusy){
            }

            //Thread.sleep(6000);

            dataService.setTag("NONE");
            dataService.robotBusy = true;
            sender.sendCommand("DRIVE BACKWARDS 150");
            while(dataService.robotBusy){
            }
            sender.sendCommand("SPEAKER SAY TUUT TUUT TUUT TUUT TUUT TUUT TUUT TUUT");
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    public void RequestLock(){
        try{
            if(dataService.getNextNode() != -1) {
                RestTemplate rest = new RestTemplate();
                boolean response = false;
                Terminal.printTerminal("Lock Requested : " + dataService.getNextNode());

                while (!response) {

                    response = rest.getForObject("http://" + serverIP + ":" + serverPort + "/point/requestlock/" + dataService.getNextNode(), boolean.class);

                    if (!response) {
                        Terminal.printTerminal("Lock Denied: " + dataService.getNextNode());
                        Thread.sleep(200);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void ReleaseLock(){
        Terminal.printTerminal("resetting point :" + dataService.getPrevNode());
        RestTemplate restTemplate = new RestTemplate();
        boolean setlock = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/point/setlock/" + dataService.getPrevNode() + "/0", Boolean.class);
    }

    public void ReleaseLock(Long pointToRelease){
        Terminal.printTerminal("resetting point :" + dataService.getPrevNode());
        RestTemplate restTemplate = new RestTemplate();
        boolean setlock = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/point/setlock/" + pointToRelease + "/0", Boolean.class);
    }
}