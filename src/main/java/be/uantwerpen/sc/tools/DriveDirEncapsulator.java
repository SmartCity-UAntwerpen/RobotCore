package be.uantwerpen.sc.tools;

import java.util.ArrayList;
import java.util.List;

public class DriveDirEncapsulator {
    private List<DriveDir> driveDirs;
    public DriveDirEncapsulator() {
        driveDirs=new ArrayList<>();
    }
    public DriveDirEncapsulator(List<DriveDir>driveDirs) {
        this.driveDirs=driveDirs;
    }
    public void addDriveDir(DriveDir driveDir){
        driveDirs.add(driveDir);
    }

    public List<DriveDir> getDriveDirs() {
        return driveDirs;
    }
}