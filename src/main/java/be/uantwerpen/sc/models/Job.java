package be.uantwerpen.sc.models;

/**
 * Created by Niels on 4/05/2016.
 */
public class Job
{
    private Long jobId,botid,startid,endid;
    private String jobDescription;

    public Job(String jobDescription)
    {
        this.jobId = 0L;
        this.jobDescription = jobDescription;
    }

    public Job(Long jobId, String jobDescription)
    {
        this.jobId = jobId;
        this.jobDescription = jobDescription;
    }

    public Job(Long jobId, Long botid, Long startid, Long endid)
    {
        this.jobId = jobId;
        this.botid = botid;
        this.startid = startid;
        this.endid = endid;
    }

    public Long getJobId()
    {
        return jobId;
    }

    public void setJobId(Long jobId)
    {
        this.jobId = jobId;
    }

    public String getJobDescription()
    {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription)
    {
        this.jobDescription = jobDescription;
    }

    @Override
    public String toString()
    {
        return "Job:{" +
                "jobId:" + jobId +
                "/ " + jobDescription +
                '}';
    }

    public Long getEndid(){
        return endid;
    }
}
