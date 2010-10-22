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
        	if(quantity > this.getTotal() || this.getAllocated(CThread) + quantity > this.getMaxClaim(CThread))
        		return null;
            else{
        		if(quantity > this.getAvailable()){
        			CThread.suspend(rrb);
        		    rrb.setStatus(Suspended);
        		    ResourceCB.RRBqueue.append(rrb);
					if(!threadList.contains(CThread)) {
                        threadList.append(CThread);
                    }
					need[this.getID()].put(CThread, quantity);
        		    return rrb;
        		}
        		else{
        			rrb.grant();
        			allocated[getID()].put(CThread, quantity);
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
        GenericList deadlocked = new GenericList();
        
        /* Percorre a resource table guardando
         * a quantidade de instancias disponiveis
         * para cada recurso.  */
        for(int i=0; i < qtdRecursos; i++) {
            resource = ResourceTable.getResourceCB(i);
            available[i] = resource.getAvailable();
        }

        /* Verifica se esta em deadlock.
         * Se estiver, cria o vetor das threads em deadlock.
         * Se nao, retorna null*/
		if(deadlockDetectionAux(deadlocked)) {
			deadlockVector = new Vector<ThreadCB>(threadList.length());
			for(int i = 0; i < deadlocked.length(); i++) {
				deadlockVector.add((ThreadCB)deadlocked.removeHead());
			}
		} else {
			return null;
		}

        /* Enquanto o sistem estiver em deadlock,
         * vai matando thread a thread ate voltar ao normal.*/
		while(deadlockDetectionAux(deadlocked)) {
			thread = (ThreadCB)deadlocked.removeHead();
		    thread.kill();
		}

        return deadlockVector;

    }

    /* Funcao auxilir que verifica se o sistema esta em deadlock. */
	private static boolean deadlockDetectionAux(GenericList deadlocked) 
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
        int[] work;

		resourceQtd = table.getSize();
        work = new int[resourceQtd];

        /* Copia o vetor de recursos disponiveis. */
        for(int i=0; i < resourceQtd; i++) {
            work[i] = available[i];
        }

        /* Percorre o vetor de threads com recursos pendentes
         * enquanto existir threads podendo ser terminadas. */
        cont = true;
		while(cont) {
			cont = false;
			requestCount = threadList.forwardIterator();

			while(requestCount.hasMoreElements()) {
			    thread = (ThreadCB)requestCount.nextElement();
                para = true;

                /* Verifica para uma thread se existem recursos
                * suficientes disponíveis, ou seja, a thread
                * pode ser finalizada.  */
				for(int i = 0; i < resourceQtd && para; i++) {
                    aux = need[i].get(thread);
                    if(aux != null) {
                        needAux = (Integer)aux;
						if(needAux > work[i])	{
					        para = false;
						}
                    }
			    }

                /* A thread pode ser finalizada */
			    if(para == true) {
				    for(int i = 0; i < resourceQtd; i++) {
                        aux = allocated[i].get(thread);
                        if(aux != null) {
    					    work[i] += (Integer)aux;
    					    allocated[i].remove(thread);
    					    need[i].remove(thread);
                        }
				    }
					cont = true;
					threadList.remove(thread);
				}
			}
		}


		if(threadList.length() != 0) {
			while(threadList.length() > 0) {
				deadlocked.append(threadList.removeHead());
			}
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
    	
		while(e.hasMoreElements()) {
			rrb = (RRB)e.nextElement();
			if(rrb.getThread().getID() == thread.getID())
				RRBqueue.remove(rrb);
		}
    	
    	for(i=0;i<ResourceTable.getSize();i++){
    		
    		res = ResourceTable.getResourceCB(i);
    		need[i].remove(res);
    		allocated[i].remove(res);
    		released = res.getAllocated(thread);
    		res.setAvailable(res.getAvailable() + released);
    		res.setAllocated(thread, 0);
    		
    	}
    	
		
	    RRBqueueGrant();

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
       int available, allocated, id;
       Enumeration e = RRBqueue.forwardIterator();
       boolean flag = false;

       /* Pega a thread que esta rodando. */
       pageTable = MMU.getPTBR();
       task = pageTable.getTask();
       thread = task.getCurrentThread();
  
       
       /* Atualiza as quantidades de recursos disponiveis
        * e alocados. */
       allocated = getAllocated(thread);
       available = getAvailable();
       id = getID();
       if(((Integer)(ResourceCB.allocated[id].get(thread))) - quantity == 0) {
    	   ResourceCB.allocated[id].remove(thread);
       } else {
    	   ResourceCB.allocated[id].put(thread, ((Integer)(ResourceCB.allocated[id].get(thread))) - quantity);
       }
       this.setAllocated(thread, allocated - quantity);
       this.setAvailable(available + quantity);
       
		/*while(e.hasMoreElements() && flag == false) {
			rrb = (RRB)e.nextElement();
			if(rrb.getThread().getID() == thread.getID() && rrb.getResource() == this){
				if(this.getAllocated(thread) == 0) RRBqueue.remove(rrb);
				flag = true;
			}
		}*/
       
       RRBqueueGrant();

    }
    
    private boolean do_deadlockAvoidance()
    {
    	//ResourceTable rt = new ResourceTable();
    	ResourceCB resource;
    	Enumeration threadEnum;
    	ThreadCB thread;
    	int resourceQtd = ResourceTable.getSize();
    	int[] work = new int[resourceQtd];
    	boolean para, cont;
    	Object aux;
    	Integer needAux;
    	
    	for(int i=0; i<resourceQtd; i++){
            resource = ResourceTable.getResourceCB(i);
            available[i] = resource.getAvailable();
            work[i] = available[i];
    	}
    	
    	/* Cria as hashes contendo 
    	 * numero maximo de recursos por tipo que uma thread pode requisitar, 
    	 * quantidade de recursos alocados por thread e
    	 * quantidade de recursos que a thread ainda pode requisitar. */
    	threadEnum = threadList.forwardIterator();
    	while(threadEnum.hasMoreElements()) {
    		thread = (ThreadCB)threadEnum.nextElement();
    		for(int i = 0; i<resourceQtd; i++) {
    			resource = ResourceTable.getResourceCB(i);
    			max[i].put(thread, resource.getMaxClaim(thread));
    			allocated[i].put(thread, resource.getAllocated(thread));
    			need[i].put(thread, resource.getMaxClaim(thread) - resource.getAllocated(thread));
    		}
    	}
    	
    	do {
    		cont = false;
    		threadEnum = threadList.forwardIterator();
    		while(threadEnum.hasMoreElements()) {
    			thread = (ThreadCB)threadEnum.nextElement();
    			para = true;
    		
    			/* Verifica se a thread pode ser finalizada. */
    			for(int i = 0; i<resourceQtd && para; i++) {
    				aux = need[i].get(thread);
    				if(aux != null) {
    					needAux = (Integer)aux;
    					if(needAux > work[i]) {
    						para = false;
    					}
    				}
    			}
    		
    			/* A thread pode ser finalizada. */
    			if(para = true) {
    				for(int i = 0; i<resourceQtd; i++) {
    					aux = need[i].get(thread);
    					if(aux != null) {
    						work[i] += (Integer)aux;
    					}
    					threadList.remove(thread);
    					cont = true;
    				}
    			}
    		}
    	} while(cont);
    	
    	if(threadList.length() != 0) {
    		return false;
    	} else {
    		return true;
    	}
    	
    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
	
	@OSPProject Resources
    */
    
    private static void RRBqueueGrant(){
    	Enumeration e = RRBqueue.forwardIterator();
    	RRB rrb;    	
    	int flag = 1;
    	
    	while(flag == 1) {
    		flag = 0;
			while(e.hasMoreElements()) {
				rrb = (RRB)e.nextElement();
				if(rrb.getResource().getAvailable() > rrb.getQuantity()) {
					flag = 1;
					RRBqueue.remove(rrb);
					rrb.grant();
				}
			}
    	}
    }
    
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
