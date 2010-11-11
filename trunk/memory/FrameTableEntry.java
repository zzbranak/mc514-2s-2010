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
    The FrameTableEntry class contains information about a specific page
    frame of memory.

    @OSPProject Memory
*/
import osp.Tasks.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.IflFrameTableEntry;

public class FrameTableEntry extends IflFrameTableEntry
{
    /**
       The frame constructor. Must have

       	   super(frameID)
	   
       as its first statement.

       @OSPProject Memory
    */
    public FrameTableEntry(int frameID)
    {
        super(frameID);

    }

    /** Método Auxiliar: Libera uma frame. Seta sua página para nula, seu dirty bit para false e
     *  seu bit de referência para falso */
    public void FreeingFrame(){
  
    	this.setDirty(false);
    	this.setReferenced(false);
    	if(this.getPage() != null){
        	this.getPage().setValid(false);
        	this.getPage().setFrame(null);
    	}   	
    	this.setPage(null);
    }

}

/*
      Feel free to add local classes to improve the readability of your code
*/
