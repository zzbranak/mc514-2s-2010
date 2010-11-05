package osp.Memory;
/**
    The PageTable class represents the page table for a given task.
    A PageTable consists of an array of PageTableEntry objects.  This
    page table is of the non-inverted type.

    @OSPProject Memory
*/
import java.lang.Math;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Hardware.*;

public class PageTable extends IflPageTable
{
	double PTBsize = Math.pow(2,MMU.getPageAddressBits());
    //PageTableEntry[] PageTable;
    /** 
	The page table constructor. Must call
	
	    super(ownerTask)

	as its first statement.

	@OSPProject Memory
    */
    public PageTable(TaskCB ownerTask)
    {
        super(ownerTask);       
        pages = new PageTableEntry[(int)this.PTBsize];
        ownerTask.setPageTable(this);
        
        for(int i=0;i<this.PTBsize;i++){
        	pages[i] = new PageTableEntry(this, i);
        }


    }

    /**
       Frees up main memory occupied by the task.
       Then unreserves the freed pages, if necessary.

       @OSPProject Memory
    */
    public void do_deallocateMemory()
    {
       /* TaskCB task;
    	
        for(int i=0;i<PTBsize;i++){
        	this.PageTable[i].getFrame().setPage(null);
        	this.PageTable[i].getFrame().setDirty(false);
        	this.PageTable[i].getFrame().setReferenced(false);
        	task = this.PageTable[i].getFrame().getReserved();
        	if(task != null)this.PageTable[i].getFrame().setUnreserved(task);
        }*/

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
