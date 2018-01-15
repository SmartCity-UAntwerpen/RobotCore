package be.uantwerpen.sc.models.map;

import be.uantwerpen.sc.models.links.Link;
import be.uantwerpen.sc.models.points.Point;

import java.util.List;

/**
 * Created by Niels on 3/04/2016.
 */
public class Node
{
    private Long nodeId;
    private Point pointEntity;
    private List<Link> neighbours;

    public Node() {
    }

    public Node(Point pointEntity) {
        this.pointEntity = pointEntity;
        this.nodeId = pointEntity.getId();
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public Point getPointEntity() {
        return pointEntity;
    }

    public void setPointEntity(Point pointEntity) {
        this.pointEntity = pointEntity;
    }

    public List<Link> getNeighbours() {
        return neighbours;
    }

    public void setNeighbours(List<Link> neighbours) {
        this.neighbours = neighbours;
    }
}
