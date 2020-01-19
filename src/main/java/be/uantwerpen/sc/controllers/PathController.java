package be.uantwerpen.sc.controllers;

import be.uantwerpen.rc.models.map.Path;
import be.uantwerpen.sc.services.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

// TODO: this class is used by TerminalService: this should be deleted or the endpoints should be defined


/**
 * Created by Niels on 10/05/2016.
 */
@RestController
@RequestMapping(value = "/path/")
public class PathController
{
    @Autowired
    DataService dataService;

    @Value("${sc.backend.ip:localhost}")
    private String serverIP;

    @Value("#{new Integer(${sc.backend.port}) ?: 1994}")
    private int serverPort;

    @RequestMapping(method = RequestMethod.GET)
    public Path getPath(int start, int stop){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Path> responseList;
        responseList = restTemplate.getForEntity("http://" + serverIP + ":" + serverPort + "/map/"+"/path/"+Integer.toString(start)+ "/"+Integer.toString(stop), Path.class);
        return responseList.getBody();
    }

    @RequestMapping(value = "random",method = RequestMethod.GET)
    public Path getRandomPath(int start){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Path> responseList;
        responseList = restTemplate.getForEntity("http://" + serverIP + ":" + serverPort + "/map/random/"+Integer.toString(start), Path.class);
        return responseList.getBody();
    }
}