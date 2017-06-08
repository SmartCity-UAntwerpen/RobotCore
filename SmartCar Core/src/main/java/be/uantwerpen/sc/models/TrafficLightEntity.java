package be.uantwerpen.sc.models;

/**
 * Created by Niels on 24/03/2016.
 */
public class TrafficLightEntity
{
    private long tlid;
    private String direction;
    private String state;
    private int placeVertex;
    private LinkEntity link;

    public long getTlid() {
        return tlid;
    }

    public void setTlid(long tlid) {
        this.tlid = tlid;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrafficLightEntity that = (TrafficLightEntity) o;

        if (tlid != that.tlid) return false;
        if (direction != null ? !direction.equals(that.direction) : that.direction != null) return false;
        if (state != null ? !state.equals(that.state) : that.state != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        long result = tlid;
        result = 31 * result + (direction != null ? direction.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        return (int) result;
    }

    public LinkEntity getLink() {
        return this.link;
    }

    public void setLink(LinkEntity link) {
        this.link = link;
    }

    public int getPlaceVertex() {
        return placeVertex;
    }

    public void setPlaceVertex(int placeVertex) {
        this.placeVertex = placeVertex;
    }

}
