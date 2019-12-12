package be.uantwerpen.sc.services;

import be.uantwerpen.rc.models.map.Point;
import be.uantwerpen.sc.controllers.PathController;
import be.uantwerpen.rc.models.map.Map;
import be.uantwerpen.sc.tools.IPathplanning;

import java.util.List;

/**
 * Created by Arthur on 18/05/2016.
 */

//TODO: refactor so this service doesn't call a controller (not good convention)

public class RandomPathPlanning implements IPathplanning
{
    PathController pathController;

    public RandomPathPlanning(PathController pathController) {
        this.pathController = pathController;
    }

    @Override
    public List<Point> Calculatepath(Map map, long start, long stop) {
        return pathController.getRandomPath((int)start).getPath();
    }
}
