package be.uantwerpen.sc.services;

import org.springframework.stereotype.Service;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Niels on 4/05/2016.
 *
 * @Author Riad on 12/12/2019
 */
@Service
public class QueueService
{
    private BlockingQueue<String> commandQueue;

    public QueueService()
    {
        this.commandQueue = new ArrayBlockingQueue<>(10000);
    }

    public synchronized String getCommand(){
        try {
            return this.commandQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized void insertCommand(String command){
        try {
            this.commandQueue.put(command);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public BlockingQueue<String> getContentQueue(){

        return this.commandQueue;
    }

    public void setContentQueue(BlockingQueue<String> queue){
        this.commandQueue =queue;
    }

}
