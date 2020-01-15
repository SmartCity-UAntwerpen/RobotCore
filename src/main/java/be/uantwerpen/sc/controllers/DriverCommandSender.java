package be.uantwerpen.sc.controllers;

import be.uantwerpen.sc.tools.Terminal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by Arthur on 2/05/2016.
 */
/*
Class waarmee robot core gaat communiceren met robot driver
*/

@Service
public class DriverCommandSender
{
    private Socket socket;
    private DataOutputStream dOut;

    @Value("${car.driver.ip}")
    private String driverIp;

    @Value("#{new Integer(${car.driver.taskport}) ?: 1313}")
    private int driverCommandPort;

    public DriverCommandSender()
    {

    }

    @PostConstruct
    private void postConstruct()
    {
        //IP and port-values are initialised at the end of the constructor
        try
        {

            //socket openen met de robot driver
            socket = new Socket(driverIp, driverCommandPort);
            socket.setSoTimeout(500);
            dOut = new DataOutputStream(socket.getOutputStream());
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void closeConnection(){
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean sendCommand(String str){ //commandos sturen naar robot driver
        try {
            str = str.concat("\n");

            byte[] bytes = str.getBytes();

            dOut.flush();
            dOut.write(bytes);
            dOut.flush();

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            Terminal.printTerminalInfo("IOException");
            return false;
        }
    }

    public boolean close()
    {
        try{
            socket.close();
            return true;
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }
}