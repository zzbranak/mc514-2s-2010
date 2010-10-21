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
  private static GenericList threadList;
	private static Hashtable[] max;
	private static Hashtable[] allocated;
	private static Hashtable[] need;
	private static Hashtable[] finish;
	private static int[] available;

    /**
       Creates a new ResourceCB instance with the given number of 
       available instances. This constructor must have super(qty) 
       as its first statement.

       @OSPProject Resources
    */
    public ResourceCB(int qty)
    {
        
        super(qty);
        threadList = new GenericList();

    }

    /**
       This method is called once, at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Resources
    */
    public static void init()
    {
			
			ResourceTable rt = new ResourceTable();
			int tam = rt.getSize();

    	    RRBqueue = new GenericList();
			max = new Hashtable[tam];
            for(int i = 0; i < tam; i++) {
                max[i] = new Hashtable();
            }

			allocated = new Hashtable[tam];
            for(int i = 0; i < tam; i++) {
                allocated[i] = new Hashtable();
            }

			need = new Hashtable[tam];
            for(int i = 0; i < tam; i++) {
                need[i] = new Hashtable();
            }
			available = new int[tam];

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
        		    ResourceCB.RRBqueue.append(rrb);
					if(!threadList.contains(CThread)) threadList.append(CThread);
					need[this.getID()].put(CThread, new Integer(quantity));
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
       
        ResourceTable resourceTable = new ResourceTable();
        ResourceCB resource;
        int qtdRecursos = resourceTable.getSize();
        Vector  deadlockVector;
        ThreadCB thread;
        
        /* Percorre a resource table guardando
         * a quantidade de instancias disponiveis
         * para cada recurso.  */
        for(int i=0; i < qtdRecursos; i++) {
            resource = ResourceTable.getResourceCB(i);
            available[i] = resource.getAvailable();
        }

		if(deadlockDetection()) {
			deadlockVector = new Vector<ThreadCB>(threadList.length());
			for(int i = 0; i < threadList.length(); i++) {
				deadlockVector.add((ThreadCB)threadList.removeHead());
			}
		} else {
			return null;
		}

		while(deadlockDetection()) {
			thread = (ThreadCB)threadList.removeHead();
		    thread.kill();
		}

        return deadlockVector;

    }

	private static boolean deadlockDetection() 
	{

		Enumeration requestCount;
		int requestNum, resourceID, resourceQtd;
		ThreadCB thread;
		RRB rrb;
		ResourceCB resource;
		boolean cont = false, para;
		ResourceTable table = new ResourceTable();
        Integer needAux;
        Object aux;

		resourceQtd = table.getSize();

		do {
			cont = false;
			requestCount = threadList.forwardIterator();
			while(requestCount.hasMoreElements()) {
			    thread = (ThreadCB)requestCount.nextElement();

                para = true;
				for(int i = 0; i < resourceQtd && para; i++) {
                    aux = (need[i].get(thread));
                    if(aux != null) {
                        needAux = (Integer)aux;
						if(needAux > available[i])	{
					        para = false;
						}
                    }
			    }

			    if(para == true) {
				    for(int i = 0; i < resourceQtd; i++) {
                        aux = allocated[i].get(thread);
                        if(aux != null) {
    					    available[i] += (Integer)aux;
                        }
				    }
					//finish.put(thread, true);
					cont = true;
					//threadList.remove(thread);
				}
			}
		} while(cont);


		if(threadList.length() != 0) {
			return true;
		} else {
			return false;
		}

	}

    /**
       When a thread was killed, this is called to release all
       the resources owned by that thread.

       @param thread -- the thread in question

       @OSPProject Resources
    */
    public static void do_giveupResources(ThreadCB thread)
    {
    	Enumeration e = RRBqueue.forwardIterator();
    	boolean flag=false;
    	int i, released, qtd;
    	ResourceCB res;
    	RRB rrb;
    	
    	for(i=0;i<ResourceTable.getSize();i++){
    		
    		res = ResourceTable.getResourceCB(i);
    		released = res.getAllocated(thread);
    		res.setAvailable(res.getAvailable() + released);
    		res.setAllocated(thread, 0);
    		
    		/*while(e.hasMoreElements() && flag==false) {
    			rrb = (RRB)e.nextElement();
    			if(res == rrb.getResource() && res.getAvailable() >= rrb.getQuantity()) {
    				RRBqueue.remove(rrb);
    				rrb.grant();
    				flag = true;
    			}
    		}*/
    		
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
       RRB rrb;
       int available, allocated;
       //ResourceCB resource;
       //int available, allocated;
       Enumeration e;
       boolean flag = false;

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
       e = RRBqueue.forwardIterator();
	   /*while(e.hasMoreElements() && flag==false) {
	        rrb = (RRB)e.nextElement();
			if(this == rrb.getResource() && this.getAvailable() >= rrb.getQuantity()) {
				RRBqueue.remove(rrb);
				rrb.grant();
				flag = true;
			}
	   }*/

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
