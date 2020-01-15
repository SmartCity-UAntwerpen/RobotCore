package be.uantwerpen.sc.services;

import be.uantwerpen.rc.models.map.*;
import be.uantwerpen.rc.tools.pathplanning.Dijkstra;
import be.uantwerpen.sc.tools.IPathplanning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Arthur on 24/04/2016.
 */
public class PathplanningService implements IPathplanning
{
    private Dijkstra dijkstra;

    private Logger logger = LoggerFactory.getLogger(PathplanningService.class);

    public PathplanningService()
    {
        this.dijkstra = new Dijkstra();
    }

    @Override
    public List<Point> Calculatepath(Map map, long start, long stop) {


        // TODO: should be completely independent of Dijkstra
        // TODO: transitions from map-classes to pathplanning-classes/
        dijkstra.computePaths(start, map.getPointList()); // run Dijkstra
        List<Point> path = dijkstra.getShortestPathTo(stop,map.getPointList()).getPath();
        System.out.println("Path: " + path);
        logger.info("Path: " + path);
        return path;
    }
}