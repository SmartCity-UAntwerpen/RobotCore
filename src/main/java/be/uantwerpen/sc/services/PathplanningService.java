package be.uantwerpen.sc.services;

import be.uantwerpen.sc.models.links.Link;
import be.uantwerpen.sc.models.map.*;
import be.uantwerpen.sc.tools.Dijkstra;
import be.uantwerpen.sc.tools.Edge;
import be.uantwerpen.sc.tools.IPathplanning;
import be.uantwerpen.sc.tools.Vertex;

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
    public List<Vertex> Calculatepath(Map map, int start, int stop) {
        //MapJson mapJsonServer = mapControlService.buildMapJson();
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
                    if(v.getId() == neighbour.getStopPoint().getId()){
                        for(Link linkEntity: linkEntityList){
                            if(linkEntity.getStopPoint().getId() == v.getId() && linkEntity.getStartPoint().getId() == nj.getPointEntity().getId()){
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

        Vertex v = new Vertex();
        for(int k = 0; i < vertexes.size();k++){

            if(vertexes.get(k).getId() == start){
                v = vertexes.get(i);
            }

        }


        dijkstra.computePaths(v,vertexes); // run Dijkstra
        //System.out.println("Distance to " + vertexes.get(stop-1) + ": " + vertexes.get(stop-1).getMinDistance());
        List<Vertex> path = dijkstra.getShortestPathTo((stop),vertexes);
        System.out.println("Path: " + path);
        //return ("Distance to " + vertexes.get(stop-1) + ": " + vertexes.get(stop-1).minDistance) + ( "Path: " + path);
        return path;
    }
}