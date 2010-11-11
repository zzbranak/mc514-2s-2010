package osp.Memory;

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
        //MMU.frameTable = new FrameTableEntry[frameTableSize];
        
        for(int i=0; i<frameTableSize; i++) {
        	//MMU.frameTable[i] = new FrameTableEntry(i);
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
    	for(int i=0;i<PAb;i++) PageSize = PageSize*2;
    	PageTot = EndMax / PageSize;
    	offsetBits = memoryAddress % PageSize;
    	PageNum = memoryAddress / PageTot;
    	MyOut.print("osp.Memory.MMU", "###Endereço de memória: " + memoryAddress);
    	MyOut.print("osp.Memory.MMU", "###Bits de Endereçamento: " + getVirtualAddressBits());
    	MyOut.print("osp.Memory.MMU", "###Bits de Pagina: " + getPageAddressBits());
    	MyOut.print("osp.Memory.MMU", "###Endereço Máximo: " + EndMax);
    	MyOut.print("osp.Memory.MMU", "###Tamanho da Página: " + PageSize);
    	MyOut.print("osp.Memory.MMU", "###Total de Páginas: " + PageTot);
    	MyOut.print("osp.Memory.MMU", "###offset: " + offsetBits);
    	MyOut.print("osp.Memory.MMU", "###Número da Página referenciada: " + PageNum);
    	MyOut.print("osp.Memory.MMU", "###Total de páginas da PageTable: " + PTb.PTBsize);
    	
    	if(PTb.pages[PageNum].isValid() == true){
    		if(referenceType == MemoryWrite )PTb.pages[PageNum].getFrame().setDirty(true);
    		if(PTb.pages[PageNum].getFrame().isReferenced() == false )PTb.pages[PageNum].getFrame().setReferenced(true);
    		return PTb.pages[PageNum];
    	}  	                
    	else{
    		if(PTb.pages[PageNum].getValidatingThread() != null){
    			MyOut.print("osp.Memory.MMU", "$$$$$$$ PÁGINA INVÁLIDA, MAS A CAMINHO");
    			thread.suspend(PTb.pages[PageNum]);
    		}
    		else{
    			MyOut.print("osp.Memory.MMU", "$$$$$$$ PÁGINA TOTALMENTE INVÁLIDA");
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
