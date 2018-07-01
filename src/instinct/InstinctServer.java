//  Instinct Server
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentLinkedDeque;

// a simple command line class to run the InstinctServer
public class InstinctServer {

	public static void main(String[] args) {
		RobotStreamData robotIncomingMessage = new RobotStreamData();

		ConcurrentLinkedDeque<String> queue = new ConcurrentLinkedDeque<>();

		ThreadedServer server = new ThreadedServer(3000, robotIncomingMessage, queue); //producer
        ThreadedEnquiryServer enquiryServer = new ThreadedEnquiryServer(3001, robotIncomingMessage, queue); //consumer

		System.out.println("Instinct Server by Rob Wortham"); 

		new Thread(server).start();
        new Thread(enquiryServer).start();

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); 
		String str;
		boolean bFinished = false;
		try
		{
				System.out.println("'exit' stops the server. '!' toggles display to console."); 
				do
				{ 
					if ((str = br.readLine()) == null)
						break; 
					System.out.println(str);
					if (str.equals("exit"))
						bFinished = true;
					else if (str.equals("!"))
						server.toggleLogDisplay();
					else
						server.sendCmd(str);
				} while(!bFinished);  
				
		} catch (Exception e) {
		    e.printStackTrace();
		}
		System.out.println("Stopping Server");
		server.stop();
	}
}

