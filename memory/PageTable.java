/*
* Grupo 06
* RA: 093311
* RA: 090995
*
* Status:Completado
*
* 11/11/2010
* 1. Método Do_lock MUITO mal explicado no manual e inclusive há erros no comentário do método
* 2.
* 3.
*
* */

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
       TaskCB task = this.getTask();
       int FTBsize = MMU.getFrameTableSize();
       FrameTableEntry frame;
             
       for(int i=0;i<FTBsize;i++){
    	   frame = MMU.getFrame(i);
    	   if(frame.getReserved() == task) frame.setUnreserved(task);
       }
       
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
