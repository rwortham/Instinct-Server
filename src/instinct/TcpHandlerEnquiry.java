//  Instinct Server - TcpHandler
//  Copyright (c) 2016  Robert H. Wortham <r.h.wortham@gmail.com>
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

package instinct;

import java.io.*;
import java.util.*;
import java.net.*;
import java.util.regex.*;

// class to handle comms over a particular tcp stream
public class TcpHandlerEnquiry implements Runnable{

    protected Socket clientSocket = null;
    protected String logFileName   = null;
    protected String cmdFileName   = null;
    protected FileOutputStream ofs = null;
    protected PrintWriter oss = null;
    protected boolean logDisplay = false;
    protected  boolean isStopped    = false;
    protected  boolean robotReady = false;
    protected HashMap<Integer, String> planElements = new HashMap<Integer, String>(255);
    protected HashMap<Integer, String> robotActions = new HashMap<Integer, String>(100);
    protected HashMap<Integer, String> robotSenses = new HashMap<Integer, String>(100);
    protected RobotStreamData robotStreamData = null;

    public TcpHandlerEnquiry(Socket clientSocket, String logFileName) {
        this.clientSocket = clientSocket;
        this.logFileName   = logFileName;
    }
    public TcpHandlerEnquiry(Socket clientSocket, String logFileName, String cmdFileName, RobotStreamData robotStreamData)
    {
        this.clientSocket = clientSocket;
        this.logFileName   = logFileName;
        this.cmdFileName   = cmdFileName;
        this.robotStreamData = robotStreamData;
    }

    // process command lines and send them to the client. Handles @ includefile directives
    // only return true if we are sending the command down to the robot
    public synchronized boolean sendCmd(String cmdLine)
    {
        if (cmdLine.startsWith("@")) // include file
        {
            String includeFileName = cmdLine.substring(cmdLine.indexOf("@")+1).trim();
            sendFile(includeFileName);
            return true;
        }
        else
        {
            String cmd;
            if (cmdLine.startsWith(cmd = "PELEM"))
                addPlanElement(cmdLine.substring(cmd.length()).trim());
            else if (cmdLine.startsWith(cmd = "RACTION"))
                addRobotAction(cmdLine.substring(cmd.length()).trim());
            else if (cmdLine.startsWith(cmd = "RSENSE"))
                addRobotSense(cmdLine.substring(cmd.length()).trim());
//			else // don't send PELEM, RACTION, RSENSE to the robot
//			{
            oss.println(cmdLine);
            return true;
//			}
        }
//		return false;
    }

    public synchronized void toggleLogDisplay()
    {
        this.logDisplay = !this.logDisplay;
    }

    public void run ()
    {
        try {
            BufferedReader iss = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            oss = new PrintWriter(clientSocket.getOutputStream(), true);


            // now loop, reading lines from the socket and writing them to the log file
            ofs = new FileOutputStream(new File(logFileName), true);
            PrintWriter bw = new PrintWriter(new OutputStreamWriter(ofs));
            String line;
            // Create a Pattern object to search for certain output lines
            // after the timestamp, log lines starting with E, S, P, F, Z, R can be decoded
            // to resolve node, sense and action IDs to their respective names
            Pattern p = Pattern.compile("[ESPFZ]");
            String[] comparators = {"EQ","NE", "GT", "LT", "TR", "FL"};

            while (!isStopped && (line = iss.readLine()) != null)
            {
                List<String> request = Arrays.asList(line.split(":"));
                String robotStreamDataLine = robotStreamData.getRobotIncomingString();

                if(robotStreamDataLine == null){
                    oss.println("No Robot Connected to Server!");
                }else{
                    boolean robotStreamDataContainsRequestKeywords = request.stream().anyMatch(robotStreamDataLine::contains);

                    if(robotStreamDataContainsRequestKeywords){
                        oss.println(robotStreamDataLine);
                    }else{
                        oss.println("doNothing");
                    }
                }

                boolean bEcho = true; // default is to echo lines from the robot back to the console

                if (!robotReady) // only send the cmdFile when the robot is ready
                {
                    if (line.contains("Robot Running"))
                    {
                        robotReady = true;
                        sendFile(cmdFileName);
                    }
                }
                String[] elements = line.split("[ ]+");
                if ((elements.length > 3) && (elements[1].length() == 1)) // check its a single character
                {
                    if (elements[1].equals("X")) // this is a sensor log line
                    {
                        // not doing anything with these at present, but don't send to console
                        bEcho = logDisplay;
                    }
                    else if (elements[1].equals("Y")) // this is a log line from the head cell matrix
                    {
                        // not doing anything with these at present, but don't send to console
                        bEcho = logDisplay;
                    }
                    else if (elements[1].equals("R")) // this line is a log line from a releaser
                    {
                        bEcho = logDisplay;

                        try
                        {
                            Integer val = new Integer(elements[2]);
                            String name = robotSenses.get(val);
                            if (!(name == null) && (name.length() > 0))
                                elements[2] = name;
                            val = new Integer(elements[3]); // this is the Comparator type
                            if (val < 6)
                                elements[3] = comparators[val];
                            line = "";
                            for (String str: elements)
                            {
                                line = line + str + " ";
                            }
                        }
                        catch (NumberFormatException e)
                        {
                            System.err.println("Error processing Releaser log entry " + line);
                        }
                    }
                    else
                    {
                        Matcher m = p.matcher(elements[1]);
                        if (m.find()) // this is a log line from a plan element
                        {
                            bEcho = logDisplay;

                            try
                            {
                                Integer val = new Integer(elements[3]);
                                String name = planElements.get(val);
                                if (!(name == null) && (name.length() > 0))
                                    elements[3] = name;

                                line = "";
                                for (String str: elements)
                                {
                                    line = line + str + " ";
                                }
                            }
                            catch (NumberFormatException e)
                            {
                                System.err.println("Error processing Element log entry " + line);
                            }
                        }
                    }
                }

                line = line.trim(); // remove any trailing spaces before logging

                // write all log lines to the log file except blank ones
                if (!line.equals(""))
                {
                    bw.println(line);
                    bw.flush();
                }
                // provide a way to close gracefully
                if (line.equals("bye"))
                {
/*****					// temporary code to write out hashmap values on exit for testing
 for (Integer key: planElements.keySet())
 {
 String value = planElements.get(key);
 System.out.println(key + " " + value);
 }
 for (Integer key: robotActions.keySet())
 {
 String value = robotActions.get(key);
 System.out.println(key + " " + value);
 }
 for (Integer key: robotSenses.keySet())
 {
 String value = robotSenses.get(key);
 System.out.println(key + " " + value);
 }
 *******/

                    System.out.println("bye received. Closing socket.");
                    break;
                }
            }

            if (!isStopped)
            {
                isStopped = true;
                ofs.close(); // out of while loop so close log file
            }
        }
        catch (IOException e)
        {
            if (!isStopped)
                System.err.println("IO Exception caught: client disconnected.");
        }
        finally
        {
            try
            {
                ofs.close(); // close the file first, then the socket
                clientSocket.close();
            }
            catch (IOException e ){ ; } // this is a bit poor I know
        }
    }

    public synchronized void stop()
    {
        this.isStopped = true;
        try
        {
            ofs.close(); // close the file first, then the socket
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("IO Exception caught: client disconnect error.");
        }
    }


    // read a file and send it, minus comment lines, to the remote client
    protected void sendFile(String fileName)
    {
        try
        {
            if ((fileName != null) && (new File(fileName)).exists())
            {
                // read the cmd file and write it to the robot
                BufferedReader ifs = new BufferedReader(new InputStreamReader(
                        new FileInputStream(new File(fileName))));
                String cmdLine;
                while((cmdLine = ifs.readLine()) != null )
                {
                    if ((cmdLine.length() > 0) && !cmdLine.startsWith("//")) // lines starting with // are comments in the command file
                    {
                        // new server only versions of PELEM, RACTION, RSENSE prefixed by !
                        String cmd;
                        if (cmdLine.startsWith(cmd = "!PELEM"))
                            addPlanElement(cmdLine.substring(cmd.length()).trim());
                        else if (cmdLine.startsWith(cmd = "!RACTION"))
                            addRobotAction(cmdLine.substring(cmd.length()).trim());
                        else if (cmdLine.startsWith(cmd = "!RSENSE"))
                            addRobotSense(cmdLine.substring(cmd.length()).trim());
                        else if ( sendCmd(cmdLine) ) // if we sent the command to the robot then delay
                        {
                            // add a short delay to allow the robot to process each command, otherwise the robot's serial buffers overrun
                            try {
                                Thread.sleep(100); // limit to 10 commands per second
                            } catch(InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                }
                ifs.close();
            }
            else
            {
                System.err.println("Command file not found");
            }
        }
        catch (IOException e)
        {
            System.err.println("Exception caught: command file not processed");
        }
    }

    void addPlanElement(String str)
    {
        try
        {
            int idx = str.indexOf("=");
            String name = str.substring(0, idx);
            Integer value = new Integer(str.substring(idx+1));
            // if plan is reloaded with different values, remove the old ones
            if (planElements.containsValue(value))
                planElements.remove(value);
            planElements.put(value, name);
        }
        catch (NumberFormatException | IndexOutOfBoundsException e)
        {
            System.err.print("AddPlanElement, Invalid entry: ");
            System.err.println(str);
        }
    }

    void addRobotAction(String str)
    {
        try
        {
            int idx = str.indexOf("=");
            String name = str.substring(0, idx);
            Integer value = new Integer(str.substring(idx+1));
            // if plan is reloaded with different values, remove the old ones
            if (robotActions.containsValue(value))
                robotActions.remove(value);
            robotActions.put(value, name);
        }
        catch (NumberFormatException | IndexOutOfBoundsException e)
        {
            System.err.print("AddRobotAction, Invalid entry: ");
            System.err.println(str);
        }
    }

    void addRobotSense(String str)
    {
        try
        {
            int idx = str.indexOf("=");
            String name = str.substring(0, idx);
            Integer value = new Integer(str.substring(idx+1));
            if (robotSenses.containsValue(value))
                robotSenses.remove(value);
            robotSenses.put(value, name);
        }
        catch (NumberFormatException | IndexOutOfBoundsException e)
        {
            System.err.print("AddRobotSense, Invalid entry: ");
            System.err.println(str);
        }
    }
}
