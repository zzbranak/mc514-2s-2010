package osp.Devices;
import java.util.*;
import osp.IFLModules.*;
import osp.Hardware.*;
import osp.Interrupts.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Tasks.*;
import osp.Memory.*;
import osp.FileSys.*;

/**
    The disk interrupt handler.  When a disk I/O interrupt occurs,
    this class is called upon the handle the interrupt.

    @OSPProject Devices
*/
public class DiskInterruptHandler extends IflDiskInterruptHandler
{
    /** 
        Handles disk interrupts. 
        
        This method obtains the interrupt parameters from the 
        interrupt vector. The parameters are IORB that caused the 
        interrupt: (IORB)InterruptVector.getEvent(), 
        and thread that initiated the I/O operation: 
        InterruptVector.getThread().
        The IORB object contains references to the memory page 
        and open file object that participated in the I/O.
        
        The method must unlock the page, set its IORB field to null,
        and decrement the file's IORB count.
        
        The method must set the frame as dirty if it was memory write 
        (but not, if it was a swap-in, check whether the device was 
        SwapDevice)

        As the last thing, all threads that were waiting for this 
        event to finish, must be resumed.

        @OSPProject Devices 
    */
    public void do_handleInterrupt()
    {
    
    	IORB iorb = (IORB)InterruptVector.getEvent();
    	IORB newIORB;
    	ThreadCB thread = iorb.getThread();
    	PageTableEntry page = iorb.getPage();
    	OpenFile file = iorb.getOpenFile();
    	int deviceID  = iorb.getDeviceID();
    	int ioType = iorb.getIOType();
    	TaskCB task = thread.getTask();
    	FrameTableEntry frame = page.getFrame();
    	Device device;

        file.decrementIORBCount();
        
        if(file.getIORBCount() == 0 && file.closePending) {
        	file.close();
        }
        
        page.unlock();
        
    	/* Verifica se eh uma operacao de swap-in ou swap-out */
    	if(deviceID != SwapDeviceID) {
    		if(task.getStatus() != TaskTerm) {
    			if(thread.getStatus() != ThreadKill) {
    				frame.setReferenced(true);
    				if(ioType == FileRead) {
    					frame.setDirty(true);
    				}
    			}
    		}
    	} else {
    		/* Verifica se a task que possui o IORB esta viva */
    		if(task.getStatus() != TaskTerm) {
    			frame.setDirty(false);
    		}
    	}
        
        if(task.getStatus() == TaskTerm){
        	if(frame.getReserved() == task) {
        		frame.setUnreserved(task);
        	}
        }
        
        iorb.notifyThreads();
        
        device = Device.get(deviceID);
        device.setBusy(false);
        
        newIORB = device.dequeueIORB();
        if(newIORB != null) {
        	device.startIO(newIORB);
        }
        
        ThreadCB.dispatch();

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
