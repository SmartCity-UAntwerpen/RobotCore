package be.uantwerpen.sc.configurations;

import be.uantwerpen.sc.RobotCoreLoop;
import be.uantwerpen.sc.controllers.*;
import be.uantwerpen.sc.controllers.mqtt.MqttPublisher;
import be.uantwerpen.sc.services.DataService;
import be.uantwerpen.sc.services.QueueService;
import be.uantwerpen.sc.tools.PathplanningType;
import be.uantwerpen.sc.tools.QueueConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import be.uantwerpen.sc.services.TerminalService;

/**
 * Created by Thomas on 14/04/2016.
 */
@Configuration
public class SystemLoader implements ApplicationRunner
{

    @Autowired
    private TerminalService terminalService;

    @Autowired
    private QueueService queueService;


    @Autowired
    private DriverCommandSender cCommandSender;


    @Autowired
    private MapController mapController;


    @Autowired
    private PathController pathController;


    @Autowired
    private PathplanningType pathplanningType;


    @Autowired
    private DriverStatusEventHandler driverStatusEventHandler;


    @Autowired
    private DataService dataService;


    @Autowired
    private MqttPublisher locationPublisher;

    @Autowired
    private DriverLocationPoller driverLocationPoller;

    @Autowired KeepAliveController keepAlivePoller;


    @Value("${sc.backend.ip:localhost}")
    String serverIP;

    @Value("#{new Integer(${sc.backend.port})}")
    int serverPort;

    @Autowired
    private RobotCoreLoop robotCoreLoop;

    //Run after Spring context initialization
    public void run(ApplicationArguments args)
    {
        new Thread(new StartupProcess()).start();
    }

    private class StartupProcess implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                Thread.sleep(200);
            }
            catch(InterruptedException ex)
            {
                //Thread interrupted
            }

            QueueConsumer queueConsumer = new QueueConsumer(queueService,cCommandSender, dataService,serverIP,serverPort);

            new Thread(robotCoreLoop).start();
            new Thread(driverStatusEventHandler).start();
            new Thread(queueConsumer).start();
            new Thread(keepAlivePoller).start();
            new Thread(driverLocationPoller).start();

            terminalService.systemReady();
        }
    }
}
