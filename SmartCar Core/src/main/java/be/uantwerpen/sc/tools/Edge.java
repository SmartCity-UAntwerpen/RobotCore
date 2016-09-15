package be.uantwerpen.sc.tools;

import be.uantwerpen.sc.models.Link;

/**
 * Created by Niels on 17/04/2016.
 */
public class Edge {

    public Edge(){}

    private Long target;
    private int weight;
    private Link linkEntity;
    public Edge(Long argTarget, int argWeight, Link linkEntity)
    { target = argTarget; weight = argWeight; this.linkEntity = linkEntity;}

    public Edge(Long argTarget, int argWeight)
    { target = argTarget; weight = argWeight;}


    public Long getTarget() {
        return target;
    }

    public void setTarget(Long target) {
        this.target = target;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public Link getLinkEntity() {
        return linkEntity;
    }

    public void setLinkEntity(Link linkEntity) {
        this.linkEntity = linkEntity;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if(Vertex.class.isAssignableFrom(obj.getClass())) {
            final Vertex other = (Vertex) obj;
            if ((this.target == -1) ? (other.getId() != -1) : !(this.target == other.getId())) {
                return false;
            }
            return true;
        }else if(Edge.class.isAssignableFrom(obj.getClass())){
            final Edge other = (Edge) obj;
            if ((this.target == -1) ? (other.getTarget() != -1) : !(this.target == other.getTarget())) {
                return false;
            }
            return true;
        }
        return false;
    }
}
