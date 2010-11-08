package osp.Memory;
import java.util.*;
import osp.Hardware.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.FileSys.FileSys;
import osp.FileSys.OpenFile;
import osp.IFLModules.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.*;

/**
    The page fault handler is responsible for handling a page
    fault.  If a swap in or swap out operation is required, the page fault
    handler must request the operation.

    @OSPProject Memory
*/
public class PageFaultHandler extends IflPageFaultHandler
{
    /**
        This method handles a page fault. 

        It must check and return if the page is valid, 

        It must check if the page is already being brought in by some other
	thread, i.e., if the page's has already pagefaulted
	(for instance, using getValidatingThread()).
        If that is the case, the thread must be suspended on that page.
        
        If none of the above is true, a new frame must be chosen 
        and reserved until the swap in of the requested 
        page into this frame is complete. 

	Note that you have to make sure that the validating thread of
	a page is set correctly. To this end, you must set the page's
	validating thread using setValidatingThread() when a pagefault
	happens and you must set it back to null when the pagefault is over.

        If a swap-out is necessary (because the chosen frame is
        dirty), the victim page must be dissasociated 
        from the frame and marked invalid. After the swap-in, the 
        frame must be marked clean. The swap-ins and swap-outs 
        must are preformed using regular calls read() and write().

        The student implementation should define additional methods, e.g, 
        a method to search for an available frame.

	Note: multiple threads might be waiting for completion of the
	page fault. The thread that initiated the pagefault would be
	waiting on the IORBs that are tasked to bring the page in (and
	to free the frame during the swapout). However, while
	pagefault is in progress, other threads might request the same
	page. Those threads won't cause another pagefault, of course,
	but they would enqueue themselves on the page (a page is also
	an Event!), waiting for the completion of the original
	pagefault. It is thus important to call notifyThreads() on the
	page at the end -- regardless of whether the pagefault
	succeeded in bringing the page in or not.

        @param thread the thread that requested a page fault
        @param referenceType whether it is memory read or write
        @param page the memory page 

	@return SUCCESS is everything is fine; FAILURE if the thread
	dies while waiting for swap in or swap out or if the page is
	already in memory and no page fault was necessary (well, this
	shouldn't happen, but...). In addition, if there is no frame
	that can be allocated to satisfy the page fault, then it
	should return NotEnoughMemory

        @OSPProject Memory
    */
    public static int do_handlePageFault(ThreadCB thread, 
					 int referenceType,
					 PageTableEntry page){
    	
    	boolean flag = false;
        int FTsize = MMU.getFrameTableSize();
        FrameTableEntry FEntry;
        SystemEvent pfEvent = new SystemEvent("PageFault");
              
        page.setValidatingThread(thread);
    	
        /* Checa se o pedido de tratamento de pagefault é válido, testando se a página mandada
         * tem ou não frame alocado. Caso tenha, o pedido é inválido e retorna false */
        if(page.getFrame() != null){
        	page.notifyThreads();
        	page.setValidatingThread(null);
        	ThreadCB.dispatch();
        	MyOut.print("osp.Memory.PFH", "RETORNA FAILURE");
        	return FAILURE;
        }
       
        
        /* Checa se há frames não locked e não reservadas para que possa ocorrer swap-out. Caso
         * não haja nenhum que obedeca as duas regras, devolve memória insuficiente */
        for(int i=0;i<FTsize;i++){
        	FEntry = MMU.getFrame(i);
        	if(FEntry.getLockCount() == 0 && FEntry.isReserved() == false) flag = true;
        }
        if(flag == false){
           	page.notifyThreads();
        	page.setValidatingThread(null);
        	ThreadCB.dispatch();
        	MyOut.print("osp.Memory.PFH", "RETORNA FAILURE");
        	return NotEnoughMemory;
        }
        
        
        thread.suspend(pfEvent);
        
        FEntry = FindFreeFrame();
        FEntry.setReserved(page.getTask());
        
        
        if(FEntry.getPage() == null){
        	MyOut.print("osp.Memory.PFH", ">>>>>>>>>>>FRAME SEM DONO: " + FEntry);
        	FEntry.FreeingFrame();
	        page.setFrame(FEntry);
        	SwapIn(page, thread);
            if(thread.getStatus() == ThreadKill){
            	FEntry.FreeingFrame();
            	page.notifyThreads();
            	page.setValidatingThread(null);
            	ThreadCB.dispatch();
            	MyOut.print("osp.Memory.PFH", "RETORNA FAILURE");
            	return FAILURE;       
            }
            PageFrameSettings(page, FEntry, referenceType);
        }
        else{
        	if(FEntry.isDirty() == true){
        		MyOut.print("osp.Memory.PFH", ">>>>>>>>>>>FRAME SUJA: " + FEntry);
        		SwapOut(FEntry.getPage(), thread);
        		FEntry.setDirty(false);
                if(thread.getStatus() == ThreadKill){
                	page.notifyThreads();
                	page.setValidatingThread(null);
                	ThreadCB.dispatch();
                	MyOut.print("osp.Memory.PFH", "RETORNA FAILURE");
                	return FAILURE;       
                }
        	
        	}
        	MyOut.print("osp.Memory.PFH", ">>>>>>>>>>>FRAME LIMPA: " + FEntry);
        	FEntry.FreeingFrame();
	        page.setFrame(FEntry);
        	SwapIn(page, thread);
            if(thread.getStatus() == ThreadKill){
            	page.setFrame(null);
            	page.notifyThreads();
            	page.setValidatingThread(null);
            	ThreadCB.dispatch();
            	MyOut.print("osp.Memory.PFH", "RETORNA FAILURE");
            	return FAILURE;       
            	
            }
            PageFrameSettings(page, FEntry, referenceType);
            
        }
        page.setValidatingThread(null);
        FEntry.setUnreserved(page.getTask());
        page.notifyThreads();
        pfEvent.notifyThreads();
        ThreadCB.dispatch();
        return SUCCESS;

    }
    
  
    /** Método Auxiliar: Faz uma busca pela tabela de frames para encontrar uma que sirva para ser usada
     * pela página, segundo o algoritmo de 'clock'. Primeiro, roda a tabela de frames atrás de uma cujo
     * bit de referência seja zero, zerando todos que não forem. Se encontrar uma frame nessa condição,
     * testa se sua quantidade de locks é zero e se ela não está reservada, pois caso uma dessas condições
     * seja falsa, a frame não poderá ser usada. Caso encontre uma frame com essas três condições, retorna
     * ela. Senão, após mudar todos os bits de referência, roda a tabela de frames novamente buscando uma
     * que satisfaça as condições de locks = 0 e referência = false, e usa a primeira que achar. Note que
     * na segunda vez que rodamos a tabela, SEMPRE haverá uma frame que obedeça a essas condições, pois
     * antes de chamar esse método, nós rastreamos a tabela de frames buscando frames cujo lock = 0 e
     * a referência = false, e não chamamos esse método caso não houvesse frames nessa situação. Além
     * disso, como a primeira vez que rodamos setamos o bit de referência para false, as frames que nao
     * puderam ser usadas no loop anterior por ter sua referência como true, poderão ser agora
     * 
     * @return objeto do tipo FrameTableEntry - Frame a ser usada
     */
    public static FrameTableEntry FindFreeFrame(){
        int FTsize = MMU.getFrameTableSize();
        FrameTableEntry FEntry;
        
        for(int i=0;i<FTsize;i++){
        	FEntry = MMU.getFrame(i);
        	
        	if(FEntry.isReferenced() == false){
        		if(FEntry.getLockCount() == 0 && FEntry.isReserved() == false){
        			return FEntry;
        		}
        		else{
        			FEntry.setReferenced(false);
        		}
        	}
        	else FEntry.setReferenced(false);
        }
        for(int i=0;i<FTsize;i++){
            FEntry = MMU.getFrame(i);
            if(FEntry.getLockCount() == 0 && FEntry.isReserved() == false && FEntry.isReferenced() == true)
            	return FEntry;
        }
        return null;
    }

    public static void SwapOut(PageTableEntry page, ThreadCB thread){
    	
    	OpenFile SwpFile = page.getTask().getSwapFile();
    	SwpFile.write(page.getID(), page, thread);
    	
    }
    
    public static void SwapIn(PageTableEntry page, ThreadCB thread){
    	
    	OpenFile SwpFile = page.getTask().getSwapFile();
    	SwpFile.read(page.getID(), page, thread);
    	
    }
    
    public static void PageFrameSettings(PageTableEntry page, FrameTableEntry frame, int Type){
    	
    	page.setValid(true);
    	frame.setReferenced(true);
        frame.setPage(page);
    	
    	if(Type == MemoryRead){
    		frame.setDirty(true);
    		return; 	
    	}
    	if(Type == MemoryWrite){ 
    		frame.setDirty(false);
    		return;
    	}
    	
    	if(Type == MemoryLock){ 
    		frame.setDirty(false);
    		return;
    	}
    }

}

/*
      Feel free to add local classes to improve the readability of your code
*/
