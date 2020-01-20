package be.uantwerpen.sc.tools;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by SarahDS on 2/06/2017.
 */

@Component
public class WorkingmodeType {

    private WorkingmodeEnum type;

    @Autowired
    public WorkingmodeType(ApplicationArguments args) {
        boolean debug = args.containsOption("debug");
        List<String> files = args.getNonOptionArgs();
        // if run with "--debug logfile.txt" debug=true, files=["logfile.txt"]
        if(files.isEmpty()){
            //type = WorkingmodeEnum.PARTIALSERVERNG;
            //type = WorkingmodeEnum.FULLSERVER;
            type = WorkingmodeEnum.INDEPENDENT;

        }else{
            switch (files.get(0).toLowerCase()){
                case "partialserver":
                    //type = WorkingmodeEnum.PARTIALSERVER;
                    type = WorkingmodeEnum.INDEPENDENT;
                    break;
                case "fullserver":
                    type = WorkingmodeEnum.FULLSERVER;
                    break;
                default:
                    //run default
                    type = WorkingmodeEnum.INDEPENDENT;
            }
        }
    }

    public WorkingmodeEnum getType() {
        return type;
    }

    public void setType(WorkingmodeEnum type) {
        this.type = type;
    }

    public String toString(WorkingmodeEnum type) {return type.toString();}

}
