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

import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Threads.*;

/**
   The studends module for dealing with resource management. The methods 
   that have to be implemented are do_grant().

   @OSPProject Resources
*/

public class RRB extends IflRRB
{
    /** 
        constructor of class RRB 
        Creates a new RRB object. This constructor must have
        super() as its first statement.

        @OSPProject Resources
    */   
    public RRB(ThreadCB thread, ResourceCB resource,int quantity)
    {

        super(thread, resource, quantity);

    }

    /**
       This method is called when we decide to grant an RRB.
       The method must update the various resource quantities
       and notify the thread waiting on the granted RRB.

        @OSPProject Resources
    */
    public void do_grant()
    {
        ResourceCB resourceType;
        int available, quantity, allocated;
        ThreadCB thread;

        quantity = this.getQuantity();
        thread = this.getThread();
        resourceType = this.getResource();
        allocated = resourceType.getAllocated(thread);
        available = resourceType.getAvailable();


        resourceType.setAvailable(available - quantity);
        resourceType.setAllocated(thread, allocated + quantity);
        this.setStatus(Granted);
        this.notifyThreads();

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
