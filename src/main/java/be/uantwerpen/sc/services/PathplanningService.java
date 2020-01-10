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
        // TODO: transitions from map-classes to pathplanning-classes


        /*List<Link> linkEntityList = new ArrayList<>();
        List<Point> vertexes = new ArrayList<>();
        for (Point node : map.getPointList()){
            System.out.print(node);
            vertexes.add(node);
            linkEntityList.addAll(node.getNeighbours());
        }

        ArrayList<Link> edges = new ArrayList<>();
        List<ArrayList<Link>> edgesListInList = new ArrayList<>();
        Link realLink = new Link();
        int i = 0;
        for (Point node : map.getPointList())
        {
            edges.clear();
            for (Link neighbour : node.getNeighbours())
            {
                for (Point v : map.getPointList())
                {
                    if(v.getId().equals(neighbour.getEndPoint()))
                    {
                        for(Link linkEntity: linkEntityList)
                        {
                            if(linkEntity.getEndPoint().equals(v.getId()) && linkEntity.getStartPoint().equals(node.getId()))
                            {
                                realLink = linkEntity;
                            }
                        }
                        edges.add(new Link(v.getId(), neighbour.getCost().getWeight()));
                    }
                }
            }
            edgesListInList.add(i, (edges));
            i++;
        }

        for (int j = 0; j < vertexes.size(); j++){
            vertexes.get(j).setNeighbours(edgesListInList.get(j));
        }*/


        dijkstra.computePaths(start, map.getPointList()); // run Dijkstra
        List<Point> path = dijkstra.getShortestPathTo(stop,map.getPointList()).getPath();
        System.out.println("Path: " + path);
        logger.info("Path: " + path);
        return path;
    }
}