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
       TaskCB task;
       
       MyOut.print("osp.Memory.PageTable", "------Tamanho da PageTable: " + PTBsize);
       
       for(int i=0;i<PTBsize;i++){
    	   MyOut.print("osp.Memory.PageTable", "------LOOP: " + i);
    	   if(this.pages[i].getFrame() != null){
               this.pages[i].getFrame().setPage(null);
               this.pages[i].getFrame().setDirty(false);
               this.pages[i].getFrame().setReferenced(false);
    	   }
        }

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
