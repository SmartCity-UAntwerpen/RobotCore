package be.uantwerpen.sc.models;

import java.util.List;

/**
 * Created by Niels on 4/05/2016.
 */
public class Job
{
    private Long jobId;
    private long idStart;
    private long idEnd;
    private long idVehicle;

    public Job(){

    }

    public Job(String jobDescription)
    {
        this.jobId = 0L;
    }

    public Job(Long jobId, String jobDescription)
    {
        this.jobId = jobId;
    }

    public Long getJobId()
    {
        return jobId;
    }

    public void setJobId(Long jobId)
    {
        this.jobId = jobId;
    }

    public Long getIdStart()
    {
        return idStart;
    }

    public void setIdStart(Long idStart)
    {
        this.idStart = idStart;
    }

    public Long getIdEnd()
    {
        return idEnd;
    }

    public void setIdEnd(Long idEnd)
    {
        this.idEnd = idEnd;
    }

    public Long getIdVehicle()
    {
        return idVehicle;
    }

    public void setIdVehicle(Long idVehicle)
    {
        this.idVehicle = idVehicle;
    }


    @Override
    public String toString()
    {
        return "Job{" +
                "jobId=" + jobId +
                '}';
    }
}
