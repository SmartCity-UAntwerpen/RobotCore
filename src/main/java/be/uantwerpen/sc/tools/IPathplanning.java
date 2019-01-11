package be.uantwerpen.sc.tools;

import be.uantwerpen.rc.models.map.Map;
import org.springframework.stereotype.Component;
import be.uantwerpen.rc.tools.Vertex;
import java.util.List;

/**
 * Created by Niels on 27/04/2016.
 */

@Component
public interface IPathplanning {

    List<Vertex> Calculatepath(Map map, long start, long stop); //implementhed in pathplanning server


}