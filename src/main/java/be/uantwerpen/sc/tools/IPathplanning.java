package be.uantwerpen.sc.tools;

import be.uantwerpen.rc.models.map.Map;
import be.uantwerpen.rc.models.map.Point;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Created by Niels on 27/04/2016.
 */

@Component
public interface IPathplanning {

    List<Point> Calculatepath(Map map, long start, long stop); //implemented in pathplanning server


}