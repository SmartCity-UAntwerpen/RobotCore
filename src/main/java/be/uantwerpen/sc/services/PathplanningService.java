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
        for (Node nj : map.getNodeList()){
            System.out.print(nj);
            vertexes.add(new Vertex(nj));
            for(Link linkEntity : nj.getNeighbours()){
                linkEntityList.add(linkEntity);
            }
        }

        ArrayList<Edge> edges;
        List<ArrayList<Edge>> edgeslistinlist = new ArrayList<>();
        Link realLink = new Link();
        int i = 0;
        for (Node nj : map.getNodeList()){
            edges = new ArrayList<>();
            for (Link neighbour : nj.getNeighbours()){
                for (Vertex v : vertexes){
                    if(v.getId() == neighbour.getEndPoint().getId()){
                        for(Link linkEntity: linkEntityList){
                            if(linkEntity.getEndPoint().getId() == v.getId() && linkEntity.getStartPoint().getId() == nj.getPointEntity().getId()){
                                realLink = linkEntity;
                            }
                        }
                        edges.add(new Edge(v.getId(),neighbour.getWeight(),realLink));
                    }
                }
            }
            edgeslistinlist.add(i, (edges));
            i++;
        }

        for (int j = 0; j < vertexes.size();j++){
            vertexes.get(j).setAdjacencies(edgeslistinlist.get(j));
        }

        //Vertex v = vertexes.get((int)start-1);

        dijkstra.computePaths(start, vertexes); // run Dijkstra
        System.out.println("Distance to " + vertexes.get((int)stop-1) + ": " + vertexes.get((int)stop-1).getMinDistance());
        List<Vertex> path = dijkstra.getShortestPathTo(stop,vertexes).getPath();
        System.out.println("Path: " + path);
        return path;
    }
}