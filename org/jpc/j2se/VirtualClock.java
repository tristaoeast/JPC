/*
    JPC: A x86 PC Hardware Emulator for a pure Java Virtual Machine
    Release Version 2.0

    A project from the Physics Dept, The University of Oxford

    Copyright (C) 2007-2009 Isis Innovation Limited

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License version 2 as published by
    the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

    Details (including contact information) can be found at: 

    www-jpc.physics.ox.ac.uk
*/

package org.jpc.j2se;

import org.jpc.emulator.*;
import org.jpc.support.Clock;
import java.io.*;
import java.util.PriorityQueue;
import java.util.logging.*;
import org.jpc.emulator.processor.Processor;
/*imports for halt profiling*/
import java.util.List;
import java.util.ArrayList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.io.IOException;


/**
 * 
 * @author Ian Preston
 */
public class VirtualClock extends AbstractHardwareComponent implements Clock
{
    public static final long IPS = Processor.IPS;
    public static final long NSPI = 10*1000000000L/IPS; //Nano seconds per instruction
    private static final Logger LOGGING = Logger.getLogger(VirtualClock.class.getName());
    private PriorityQueue<Timer> timers;
    private volatile boolean ticksEnabled;
    private long ticksOffset;
    private long ticksStatic;
    private long currentTime;
    private long totalTicks = 0;
    private static final boolean REALTIME = false; //sync clock with real clock
    
    private static final String FILEPATH = "./haltProfiling.xls";
    
	public static int[] count = new int[6000];
	public static long[] sleep_time = new long[6000];
    public static List<String> prof_list = new ArrayList<String>(); 
    
    public static int halt_count = 0;

    public VirtualClock()
    {
        timers = new PriorityQueue<Timer>(20); // initial capacity to be revised
        ticksEnabled = false;
        ticksOffset = 0;
        ticksStatic = 0;
        currentTime = getSystemTimer();
    }
    
    public static void haltProfilingToFile(){

    	File oldFile = new File(FILEPATH);
    	oldFile.delete();
	
	prof_list.add("Count\tSleep_Time");
	for(int i  = 0; i < 6000; i++){
		prof_list.add(String.valueOf(count[i])+"\t"+String.valueOf(sleep_time[i]));
	}
	
    File file = new File(FILEPATH);
    Writer fileWriter = null;
    BufferedWriter bufferedWriter = null;

    try{
        fileWriter = new FileWriter(file, true);
        bufferedWriter = new BufferedWriter(fileWriter);

        for(String line : prof_list){
            line += System.getProperty("line.separator");
            bufferedWriter.write(line);
        }
    }	catch (IOException e){
        	System.err.println("Error writing the file... ");
        	e.printStackTrace();
    	}	finally {
		        if(bufferedWriter != null && fileWriter != null) {
		            try{
		                bufferedWriter.close();
		                fileWriter.close();
		            } catch (IOException e){
		                e.printStackTrace();
		            }
		        }
    		}
    } 

    public void saveState(DataOutput output) throws IOException
    {
        output.writeBoolean(ticksEnabled);
        output.writeLong(ticksOffset);
        output.writeLong(getTime());
    }

    public void loadState(DataInput input, PC pc) throws IOException
    {
        ticksEnabled = input.readBoolean();
        ticksOffset = input.readLong();
        ticksStatic = input.readLong();
    }

    public synchronized Timer newTimer(TimerResponsive object)
    {
        Timer tempTimer = new Timer(object, this);
        return tempTimer;
    }

    private boolean process()
    {
        Timer tempTimer;
        tempTimer = timers.peek();
        if ((tempTimer == null) || !tempTimer.check(getTime()))
            return false;
        else
            return true;
    }

    public synchronized void update(Timer object)
    {
        timers.remove(object);
        if (object.enabled())
        {
            timers.offer(object);
        }
    }

    public long getTime()
    {
        if (ticksEnabled)
        {
            return this.getRealTime() + ticksOffset;
        } else
        {
            return ticksStatic;
        }
    }

    private long getRealTime()
    {
        return currentTime;
    }

    public long getTickRate()
    {
        return IPS*10;//1000000000L;
    }

    public long getTicks() {
        return totalTicks;
    }

    public void pause()
    {
        if (ticksEnabled)
        {
            ticksStatic = getTime();
            ticksEnabled = false;
        }
    }

    public void resume()
    {
        if (!ticksEnabled)
        {
            ticksOffset = ticksStatic - getRealTime();
            ticksEnabled = true;
        }
    }

    public void reset()
    {
            this.pause();
            ticksOffset = 0;
            ticksStatic = 0;
    }

    public String toString()
    {
        return "Virtual Clock";
    }

    private long getSystemTimer()
    {
        return System.nanoTime();
    }

    public void updateNowAndProcess() {
    	halt_count++;
        if (REALTIME) {
            currentTime = getSystemTimer();
            if (process())
            {
                return;
            }

            Timer tempTimer;
            synchronized (this)
            {
                tempTimer = timers.peek();
            }
            long expiry = tempTimer.getExpiry();
            try
            {
            	long timeToSleep = Math.min((expiry - getTime()) / 1000000, 100);
                Thread.sleep(timeToSleep);
                if(halt_count<6000){
        			count[halt_count]=halt_count;
        			sleep_time[halt_count]=timeToSleep;
        		}
            } catch (InterruptedException ex)
            {
                Logger.getLogger(VirtualClock.class.getName()).log(Level.SEVERE, null, ex);
            }
            totalTicks += (expiry - ticksOffset - currentTime)/NSPI;
            currentTime = getSystemTimer();

            tempTimer.check(getTime());
        } else {
            Timer tempTimer;
            synchronized (this)
            {
                tempTimer = timers.peek();
            }
            long expiry = tempTimer.getExpiry();
            try
            {
            	long timeToSleep = Math.min((expiry - getTime()) / 1000000, 100);
                Thread.sleep(timeToSleep);
                if(halt_count<6000){
        			count[halt_count]=halt_count;
        			sleep_time[halt_count]=timeToSleep;
        		}
                
            } catch (InterruptedException ex)
            {
                Logger.getLogger(VirtualClock.class.getName()).log(Level.SEVERE, null, ex);
            }
            totalTicks += (expiry - ticksOffset - currentTime)/NSPI;
            currentTime = expiry -ticksOffset;
            //System.out.println("New time during HALT: " + (expiry - ticksOffset));
            tempTimer.check(getTime());
        }
    }

    public void updateAndProcess(int instructions)
    {
        totalTicks += instructions;
        if (REALTIME)
            currentTime = getSystemTimer();
        else
            currentTime += instructions * NSPI;
        process();
    }
}
