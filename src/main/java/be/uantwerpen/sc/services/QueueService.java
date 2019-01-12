package be.uantwerpen.sc.services;

import org.springframework.stereotype.Service;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Niels on 4/05/2016.
 */
@Service
public class QueueService
{
    BlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(100);

    public QueueService()
    {

    }

    public synchronized String getCommand(){
        try {
            return commandQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized void insertCommand(String command){
        try {
            commandQueue.put(command);
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
