package be.uantwerpen.sc.tools;

import be.uantwerpen.sc.models.map.Map;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by Niels on 27/04/2016.
 */

@Component
public interface IPathplanning {

    List<Vertex> Calculatepath(Map map, int start, int stop); //implementhed in pathplanning server


}
