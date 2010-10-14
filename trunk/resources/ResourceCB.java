/*
* Grupo 06
* RA: 093311
* RA: 090995
*
* Status: Incompleto
* 
* 1. Dificuldades nos métodos do_acquire e deadlock detection. Poucas especificações no livro.
*
* 07/10/2010
*
* */

package osp.Resources;

import java.util.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Memory.*;

/**
    Class ResourceCB is the core of the resource management module.
    Students implement all the do_* methods.
    @OSPProject Resources
*/
public class ResourceCB extends IflResourceCB
{
	private static GenericList RRBqueue;
    /**
       Creates a new ResourceCB instance with the given number of 
       available instances. This constructor must have super(qty) 
       as its first statement.

       @OSPProject Resources
    */
    public ResourceCB(int qty)
    {
        
        super(qty);

    }

    /**
       This method is called once, at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Resources
    */
    public static void init()
    {
        // your code goes here

    }

    /**
       Tries to acquire the given quantity of this resource.
       Uses deadlock avoidance or detection depending on the
       strategy in use, as determined by ResourceCB.getDeadlockMethod().

       @param quantity
       @return The RRB corresponding to the request.
       If the request is invalid (quantity+allocated>total) then return null.

       @OSPProject Resources
    */
    public RRB  do_acquire(int quantity) 
    {
        PageTable PTble = MMU.getPTBR();
        TaskCB CTask = PTble.getTask();
        ThreadCB CThread = CTask.getCurrentThread();
        RRB rrb = new RRB(CThread, this, quantity);        
        
        if(ResourceCB.getDeadlockMethod() == Detection){
        	if(quantity + this.getAllocated(CThread) > this.getTotal())
        		return null;
            else{
        		if(quantity > this.getAvailable()){
        			CThread.suspend(rrb);
        		    rrb.setStatus(Suspended);
        		    return null;
        		}
        		else{
        			rrb.grant();
        		    return rrb;
        		}
            }
        }
        return null;

    }

    /**
       Performs deadlock detection.
       @return A vector of ThreadCB objects found to be in a deadlock.

       @OSPProject Resources
    */
    public static Vector do_deadlockDetection()
    {
        // your code goes here
    	return null;

    }

    /**
       When a thread was killed, this is called to release all
       the resources owned by that thread.

       @param thread -- the thread in question

       @OSPProject Resources
    */
    public static void do_giveupResources(ThreadCB thread)
    {
    	boolean flag=false;
    	int i, released;
    	ResourceCB res;
    	GenericList wQueue;
    	
    	for(i=0;i<ResourceTable.getSize();i++){
    		
    		res = ResourceTable.getResourceCB(i);
    		released = res.getAllocated(thread);
    		res.setAvailable(res.getAvailable() + released);
    		res.setAllocated(thread, 0);
    	}
        

    }

    /**
        Release a previously acquired resource.

	@param quantity

        @OSPProject Resources
    */
    public void do_release(int quantity)
    {
        
        
        PageTable pageTable;
        TaskCB task;
        ThreadCB thread;
        ResourceCB resource;
        int available, allocated;

        /* Pega a thread que esta rodando. */
        pageTable = MMU.getPTBR();
        task = pageTable.getTask();
        thread = task.getCurrentThread();

        //resource = getResource();

        /* Atualiza as quantidades de recursos disponiveis
         * e alocados. */
        allocated = getAllocated(thread);
        available = getAvailable();
        setAllocated(thread, allocated - quantity);
        setAvailable(available + quantity);
        

    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
	
	@OSPProject Resources
    */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
     
	@OSPProject Resources
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
