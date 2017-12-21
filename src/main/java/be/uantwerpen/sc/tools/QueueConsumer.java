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
    private boolean first = true;
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
        /*
        this.serverIP = ip;
        this.serverPort = port;
        */
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                //System.out.println("Consumer wants to consume");
                Thread.sleep(100);

                //kijken of een kruispunt vrij is en een lock aanvragen
                if(dataService.getNextNode() != -1) {
                    if (!lockGranted) {
                        //Robot already has permission?
                        if (!(dataService.hasPermission() == dataService.getNextNode())) {
                            Terminal.printTerminal("Millis: " + dataService.getMillis() + " ,linkMillis: " + (dataService.getLinkMillis() - 150));
                            if (dataService.getMillis() > dataService.getLinkMillis() - 200) {
                                //Pause robot
                                sender.sendCommand("DRIVE PAUSE");
                                Terminal.printTerminal("PAUSED");
                                //Ask for permission
                                RestTemplate rest = new RestTemplate();
                                boolean response = false;
                                Terminal.printTerminal("Lock Requested");
                                while (!response) {
                                    response = rest.getForObject("http://" + serverIP + ":" + serverPort + "/point/requestlock/" + dataService.getNextNode(), boolean.class);

                                    if (!response) {
                                        Terminal.printTerminal("Lock Denied: " + dataService.getNextNode());
                                        Thread.sleep(200);
                                    }
                                }
                                //response true -> Lock granted
                                Terminal.printTerminal("Lock Granted: " + dataService.getNextNode());
                                lockGranted = true;
                                dataService.setPermission((int)(long)dataService.getNextNode());
                                Terminal.printTerminal("Permission: " + dataService.hasPermission() + " ,NextNode: " + dataService.getNextNode());
                                sender.sendCommand("DRIVE RESUME");
                                Terminal.printTerminal("RESUMED");
                            }
                        } else {
                            lockGranted = true;
                        }
                    }
                }

                //zijn er nog jobs in de queue?
                if(queueService.getContentQueue().size() == 0){

                    if((dataService.getCurrentLocation() == dataService.getDestination()) && (dataService.getDestination() != -1L) && (dataService.getCurrentLocation() != -1L)){

                        Terminal.printTerminal("Finished");
                        //Park();

                        prevQueueSize = 0;
                        RestTemplate restTemplate = new RestTemplate(); //standaard resttemplate gebruiken

                        restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/job/finished/" + dataService.getRobotID()//aan de server laten weten dat er een nieuwe bot zich aanbied
                                , Void.class); //Aan de server laten weten in welke mode de bot werkt
                        dataService.setDestination(-1L);

                    }

                }else{
                    //If robot not busy

                    prevQueueSize = queueService.getContentQueue().size();
                    if(!dataService.robotBusy) {
                        Terminal.printTerminal("Robot not busy");
                        Terminal.printTerminal(queueService.getContentQueue().toString());
                        String s = queueService.getJob();
                        Terminal.printTerminal("Sending: " + s);
                        sender.sendCommand(s);
                        //change looking coordinate when turning
                        if(dataService.getCurrentLocation()!=-1)
                            if(dataService.getMap().changeLookingDir(dataService.getCurrentLocation(), dataService.getTag())!=null)
                                dataService.setLookingCoordiante(dataService.getMap().changeLookingDir(dataService.getCurrentLocation(), dataService.getTag()));
                        System.out.println("coordinate: "+dataService.getLookingCoordiante());

                        if(!s.contains("DRIVE DISTANCE")) {

                            dataService.robotBusy = true;
                            dataService.setLocationVerified(false);
                        }
                        if(s.contains("DRIVE FOLLOWLINE")){
                            //Next Link
                            if(first) {
                                first = false;
                                Terminal.printTerminal("Setting up");
                            }else{
                                dataService.nextLink();
                            }

                            //TODO when sending manual commands calling getNextNode will crash the program
                            //When changing link reset permission
                            if(dataService.hasPermission() == dataService.getNextNode()){
                                //Leave permission
                            }else {
                                dataService.setPermission(-1);
                                Terminal.printTerminal("Permission reset");
                                lockGranted = false;
                            }

                            //Unlock point
                            RestTemplate rest = new RestTemplate();
                            rest.getForObject("http://" + serverIP + ":" + serverPort + "/point/setlock/" + dataService.getPrevNode() + "/0", Boolean.class);
                        }
                    }
                }
                //System.out.println("CrunchifyBlockingConsumer: Message - " + queueService.getJob() + " consumed.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void Park(){
        try{
            Terminal.printTerminal("Parking");
            sender.sendCommand("DRIVE TURN");
            Thread.sleep(4000);
            dataService.setTag("NONE");
            sender.sendCommand("DRIVE BACKWARDS 400");

            while (dataService.getTag().trim().equals("NONE") || dataService.getTag().equals("NO_TAG")) {
                try {
                    //Terminal.printTerminal("reading tag");
                    Thread.sleep(1000);
                    queueService.insertJob("TAG READ UID");

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            sender.sendCommand("DRIVE ABORT");
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
