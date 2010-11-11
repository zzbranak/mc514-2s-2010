package osp.Memory;

import osp.Hardware.*;
import osp.Tasks.*;
import osp.Threads.*;
import osp.Devices.*;
import osp.Utilities.*;
import osp.IFLModules.*;
/**
   The PageTableEntry object contains information about a specific virtual
   page in memory, including the page frame in which it resides.
   
   @OSPProject Memory

*/

public class PageTableEntry extends IflPageTableEntry
{
    /**
       The constructor. Must call

       	   super(ownerPageTable,pageNumber);
	   
       as its first statement.

       @OSPProject Memory
    */
    public PageTableEntry(PageTable ownerPageTable, int pageNumber)
    {
       super(ownerPageTable, pageNumber);

    }

    /**
       This method increases the lock count on the page by one. 

	The method must FIRST increment lockCount, THEN  
	check if the page is valid, and if it is not and no 
	page validation event is present for the page, start page fault 
	by calling PageFaultHandler.handlePageFault().

	@return SUCCESS or FAILURE
	FAILURE happens when the pagefault due to locking fails or the 
	that created the IORB thread gets killed.

	@OSPProject Memory
     */
    public int do_lock(IORB iorb)
    {
    	ThreadCB CallingThread = iorb.getThread();
    	ThreadCB pfThread = this.getValidatingThread();
    	TaskCB task = this.getTask();
    	FrameTableEntry frame = this.getFrame();
    	
    	if(pfThread == null){
    		if(this.isValid() == false){
    	    	MyOut.print("osp.Memory.PageTableEntry", "[[[[[[[[[[[[[[[[[[[[[[[[");
            	if(PageFaultHandler.handlePageFault(CallingThread, MemoryLock, this) == SUCCESS){
                	this.getFrame().incrementLockCount();
                	return SUCCESS;
            	}
    		    return FAILURE;
    		}   
        	this.getFrame().incrementLockCount();
        	return SUCCESS;
    	}
    	
    	if(pfThread == CallingThread){
    		this.getFrame().incrementLockCount();
        	return SUCCESS;
    	}

    	CallingThread.suspend(this);
    	if(CallingThread.getStatus() != ThreadKill && this.isValid()){
    		this.getFrame().incrementLockCount();
        	return SUCCESS;
    	}
    	return FAILURE;

    }

    /** This method decreases the lock count on the page by one. 

	This method must decrement lockCount, but not below zero.

	@OSPProject Memory
    */
    public void do_unlock()
    {
        this.getFrame().decrementLockCount();

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
