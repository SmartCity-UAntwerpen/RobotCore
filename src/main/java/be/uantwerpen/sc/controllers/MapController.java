package be.uantwerpen.sc.controllers;

import be.uantwerpen.rc.models.map.Map;
import be.uantwerpen.rc.tools.Vertex;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Created by Niels on 24/04/2016.
 */
/*
class voor de map te ontvangen
 */
@RestController
@RequestMapping(value = "/map/")
public class MapController
{
    @Value("${sc.core.ip:localhost}")
    private String serverIP;

    @Value("#{new Integer(${sc.core.port}) ?: 1994}")
    private int serverPort;

    @RequestMapping(method = RequestMethod.GET)
    public Map getMap(){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> responseList;
        responseList = restTemplate.getForEntity("http://" + serverIP + ":" + serverPort + "/map/", Map.class);
        Map map = responseList.getBody();
        return map;
    }

    @RequestMapping(method = RequestMethod.GET ,value = "/map2/")
    public Vertex[] getPath(){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Vertex[]> responseList;
        responseList = restTemplate.getForEntity("http://" + serverIP + ":" + serverPort + "/map/1/path/24", Vertex[].class);
        Vertex[] list = responseList.getBody();
        return list;
    }
}