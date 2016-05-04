package be.uantwerpen.sc.controllers;

import be.uantwerpen.sc.services.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpHeaders;
import sun.invoke.empty.Empty;

import java.util.concurrent.BlockingQueue;

/**
 * Created by Niels on 4/05/2016.
 */
@RestController
public class JobListenerController {

    @Autowired
    private QueueService queueService;
    private BlockingQueue<String> jobQueue;

    @RequestMapping(value = "/job/", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<String> jobListener(@RequestBody String job, UriComponentsBuilder ucBuilder){

        if(job == null){
            return new ResponseEntity("No Job",HttpStatus.NO_CONTENT);
        }else{
            System.out.println("Job = " + job);
            queueService.insertJob(job);
            return new ResponseEntity("ok",HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/checkjobs/", method = RequestMethod.GET)
    public void chechQueue(){
        jobQueue = queueService.getContentQueue();
        System.out.println(jobQueue.toString());
    }

}


