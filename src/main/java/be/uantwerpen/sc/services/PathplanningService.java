package be.uantwerpen.sc.services;

import be.uantwerpen.rc.models.map.*;
import be.uantwerpen.rc.tools.pathplanning.Dijkstra;
import be.uantwerpen.rc.tools.Edge;
import be.uantwerpen.rc.tools.Vertex;
import be.uantwerpen.sc.tools.IPathplanning;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Arthur on 24/04/2016.
 */
public class PathplanningService implements IPathplanning
{
    private Dijkstra dijkstra;

    public PathplanningService()
    {
        this.dijkstra = new Dijkstra();
    }

    @Override
    public List<Vertex> Calculatepath(Map map, long start, long stop) {
        List<Link> linkEntityList = new ArrayList<>();
        List<Vertex> vertexes = new ArrayList<>();
        for (Node node : map.getNodeList()){
            System.out.print(node);
            vertexes.add(new Vertex(node));
            linkEntityList.addAll(node.getNeighbours());
        }

        ArrayList<Edge> edges = new ArrayList<>();
        List<ArrayList<Edge>> edgesListInList = new ArrayList<>();
        Link realLink = new Link();
        int i = 0;
        for (Node node : map.getNodeList())
        {
            edges.clear();
            for (Link neighbour : node.getNeighbours())
            {
                for (Vertex v : vertexes)
                {
                    if(v.getId().equals(neighbour.getEndPoint().getId()))
                    {
                        for(Link linkEntity: linkEntityList)
                        {
                            if(linkEntity.getEndPoint().getId().equals(v.getId()) && linkEntity.getStartPoint().getId() == node.getPointEntity().getId())
                            {
                                realLink = linkEntity;
                            }
                        }
                        edges.add(new Edge(v.getId(),neighbour.getWeight(),realLink));
                    }
                }
            }
            edgesListInList.add(i, (edges));
            i++;
        }

        for (int j = 0; j < vertexes.size();j++){
            vertexes.get(j).setAdjacencies(edgesListInList.get(j));
        }


        dijkstra.computePaths(start, vertexes); // run Dijkstra
        List<Vertex> path = dijkstra.getShortestPathTo(stop,vertexes).getPath();
        System.out.println("Path: " + path);
        return path;
    }
}