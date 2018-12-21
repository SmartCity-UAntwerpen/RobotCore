package be.uantwerpen.sc.tools;

/**
 * Created by Arthur on 28/04/2016.
 */
public class DriveDir {

    DriveDirEnum dir;
    double angle = 90;
    String command;
    DriveDir(){

    }

    public DriveDir(DriveDirEnum dir, double angle){
        this.dir = dir;
        this.angle = angle;
        command = "MISSING";
    }

    public DriveDir(String command){
        this.dir = DriveDirEnum.NONE;
        this.command = command;
    }


    public DriveDir(DriveDirEnum dir) {
        this.dir = dir;
    }

    @Override
    public String toString(){
        switch(dir){
            case FORWARD:
                return "DRIVE FORWARD 120";
            case LEFT:
                return "DRIVE TURN L " + angle;
            case RIGHT:
                return "DRIVE TURN R " + angle;
            case FOLLOW:
                return "DRIVE FOLLOWLINE";
            case TURN:
                return "DRIVE ROTATE R 180";
            case LONGDRIVE:
                return "DRIVE FORWARD 150";
            default:
                return command;
        }
    }

    public DriveDirEnum getDir() {
        return dir;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }
}
