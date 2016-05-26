package be.uantwerpen.sc.services;

import be.uantwerpen.sc.controllers.MapController;
import be.uantwerpen.sc.controllers.PathController;
import be.uantwerpen.sc.models.map.Map;
import be.uantwerpen.sc.tools.IPathplanning;
import be.uantwerpen.sc.tools.Vertex;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Created by Arthur on 18/05/2016.
 */

public class RandomPathPlanning implements IPathplanning {

    @Autowired
    PathController pathController;

    @Override
    public List<Vertex> Calculatepath(Map map, int start, int stop) {
        return pathController.getRandomPath(start).getPath();
    }
}
