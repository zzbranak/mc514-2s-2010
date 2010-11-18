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
import java.lang.Math;
import java.util.*;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Interrupts.*;

/**
    The MMU class contains the student code that performs the work of
    handling a memory reference.  It is responsible for calling the
    interrupt handler if a page fault is required.

    @OSPProject Memory
*/
public class MMU extends IflMMU
{
	//private static FrameTableEntry[] frameTable;
	
    /** 
        This method is called once before the simulation starts. 
	Can be used to initialize the frame table and other static variables.

        @OSPProject Memory
    */
    public static void init()
    {
    	
    	/* Cria a FrameTable e associa uma FrameTableEntry a cada entrada. */
    	int frameTableSize = MMU.getFrameTableSize();
        
        for(int i=0; i<frameTableSize; i++) {
        	MMU.setFrame(i, new FrameTableEntry(i));
        }

    }

    /**
       This method handlies memory references. The method must 
       calculate, which memory page contains the memoryAddress,
       determine, whether the page is valid, start page fault 
       by making an interrupt if the page is invalid, finally, 
       if the page is still valid, i.e., not swapped out by another 
       thread while this thread was suspended, set its frame
       as referenced and then set it as dirty if necessary.
       (After pagefault, the thread will be placed on the ready queue, 
       and it is possible that some other thread will take away the frame.)
       
       @param memoryAddress A virtual memory address
       @param referenceType The type of memory reference to perform 
       @param thread that does the memory access
       (e.g., MemoryRead or MemoryWrite).
       @return The referenced page.

       @OSPProject Memory
    */
    static public PageTableEntry do_refer(int memoryAddress,
					  int referenceType, ThreadCB thread)
    {
    	int VAb = getVirtualAddressBits();
    	int PAb = getPageAddressBits();
    	int EndMax = 1;
    	int PageSize = 1;
    	int offsetBits;
    	int PageNum;
    	int PageTot;
    	PageTable PTb = getPTBR();
    	
    
    	/* Pega a pagina do endereço de memoria. */
    	
    	for(int i=0;i<VAb;i++) EndMax = EndMax*2;
    	//EndMax = Math.pow((int)2, (int)VAb);
    	for(int i=0;i<PAb;i++) PageSize = PageSize*2;
    	//PageSize = Math.pow((int)2, (int)PAb);
    	PageTot = EndMax / PageSize;
    	offsetBits = memoryAddress % PageSize;
    	PageNum = memoryAddress / PageTot;
    	
    	if(PTb.pages[PageNum].isValid() == true){
    		if(referenceType == MemoryWrite )PTb.pages[PageNum].getFrame().setDirty(true);
    		PTb.pages[PageNum].getFrame().setReferenced(true);
    		return PTb.pages[PageNum];
    	}  	                
    	else{
    		if(PTb.pages[PageNum].getValidatingThread() != null){
    			thread.suspend(PTb.pages[PageNum]);
    		}
    		else{
    			InterruptVector.setInterruptType(PageFault);
    			InterruptVector.setThread(thread);
    			InterruptVector.setPage(PTb.pages[PageNum]);
    			CPU.interrupt(PageFault);
    			
    		}
			if(thread.getStatus() != ThreadKill){
				if(referenceType == MemoryWrite )PTb.pages[PageNum].getFrame().setDirty(true);
	    		
	    		PTb.pages[PageNum].getFrame().setReferenced(true);
			}
    		return PTb.pages[PageNum];
    	}

    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
     
	@OSPProject Memory
     */
    public static void atError()
    {

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
     
      @OSPProject Memory
     */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
