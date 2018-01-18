package be.uantwerpen.sc.tools;

import be.uantwerpen.sc.models.map.Map;
import be.uantwerpen.sc.models.map.MapJson;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.websocket.server.ServerEndpoint;
import java.util.List;

/**
 * Created by Niels on 27/04/2016.
 */

@Component
public interface IPathplanning {

    List<Vertex> Calculatepath(Map map, int start, int stop); //implementhed in pathplanning server


}
