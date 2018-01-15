package be.uantwerpen.sc.tools;

/**
 * Created by Arthur on 24/04/2016.
 */
import org.springframework.boot.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.*;

import java.util.List;

@Component("test")
public class PathplanningType {

    private PathplanningEnum type;

    @Autowired
    public PathplanningType(ApplicationArguments args) {
        boolean debug = args.containsOption("debug");
        List<String> files = args.getNonOptionArgs();
        // if run with "--debug logfile.txt" debug=true, files=["logfile.txt"]
        if(files.isEmpty()){
            type = PathplanningEnum.TERMINAL;
        }else{
            switch (files.get(0).toLowerCase()){
                case "dijkstra":
                    type = PathplanningEnum.DIJKSTRA;
                    break;
                case "random":
                    type = PathplanningEnum.RANDOM;
                    break;
                default:
                    //run default
                    type = PathplanningEnum.TERMINAL;
            }
        }

    }

    public PathplanningEnum getType() {
        return type;
    }

    public void setType(PathplanningEnum type) {
        this.type = type;
    }

}