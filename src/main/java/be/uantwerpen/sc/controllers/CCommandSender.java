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
public class CCommandSender
{
    private Socket socket;
    private DataOutputStream dOut;
    private DataInputStream dIn;
    private boolean serverActive;

    //@Value("${car.ccore.ip:localhost}")
    @Value("${car.ccore.ip:146.175.140.187}")
    private String coreIP;

    @Value("#{new Integer(${car.ccore.taskport}) ?: 1313}")
    private int coreCommandPort;

    public CCommandSender()
    {

    }

    @PostConstruct
    private void postConstruct()
    {
        //IP / port-values are initialised at the end of the constructor
        try
        {

            //socket openen met de robot driver

            socket = new Socket(coreIP, coreCommandPort);
            socket.setSoTimeout(500);
            dOut = new DataOutputStream(socket.getOutputStream());
            dIn = new DataInputStream(socket.getInputStream());
            serverActive = true;

        }


        catch(Exception e)
        {
            e.printStackTrace();
            serverActive = false;
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
            //byte[] message = str.getBytes();
            //System.out.println(message.toString());

            int attempts = 0;

            str = str.concat("\n");

            byte[] bytes = str.getBytes();

            //while(attempts <5) {
            //Send message
            //dOut.writeInt(message.length); // write length of the message
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