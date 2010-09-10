/*
* Grupo 06
* RA: 093311
* RA: 090995
*
* Status: Completo
*
* 02/09/2010
* 1. Projeto Completado
* 2. Observação: Existe um erro em certas execuções esporádicas do programa, em que ele encerra a execução
* mostrando um erro que não faz sentido no Log. O professor pediu para deixar especificado no comentário,pois
* é possível bug do simulador.
* 3. 
*
* 09/09/2010
* 1.
* 2.
* 3.
*
* */ 

package osp.Tasks;

import java.util.Enumeration;
import java.util.Vector;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Ports.*;
import osp.Memory.*;
import osp.FileSys.*;
import osp.Utilities.*;
import osp.Hardware.*;

/**
    The student module dealing with the creation and killing of
    tasks.  A task acts primarily as a container for threads and as
    a holder of resources.  Execution is associated entirely with
    threads.  The primary methods that the student will implement
    are do_create(TaskCB) and do_kill(TaskCB).  The student can choose
    how to keep track of which threads are part of a task.  In this
    implementation, an array is used.

    @OSPProject Tasks
*/
public class TaskCB extends IflTaskCB
{
    // Criação de Generic Lists que contêm as Threads, Ports e Files da task
	private GenericList lsThreads;
	private GenericList lsPorts;
	private GenericList lsFiles;
	
    /**
       The task constructor. Must have

       	   super();

       as its first statement.

       @OSPProject Tasks
    */
    public TaskCB()
    {
    	super();
        lsThreads = new GenericList();
    	lsPorts = new GenericList();
    	lsFiles = new GenericList();
        // your code goes here

    }

    /**
       This method is called once at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Tasks
    */
    public static void init()
    {
        // your code goes here

    }

    /** 
        Sets the properties of a new task, passed as an argument. 
        
        Creates a new thread list, sets TaskLive status and creation time,
        creates and opens the task's swap file of the size equal to the size
	(in bytes) of the addressable virtual memory.

	@return task or null

        @OSPProject Tasks
    */
    static public TaskCB do_create()
    {
    	long cTime;
    	double swpSize;
    	int swpName;
    	
    	TaskCB nTask = new TaskCB();   // Cria a referência para a nova task e aloca
    	PageTable tTable = new PageTable(nTask);
    	nTask.setPageTable(tTable);
 
    	// Seta informações básicas da Task. Hora de Criação, Status e Prioridade
    	cTime = HClock.get();
    	nTask.setCreationTime((double)cTime);
    	nTask.setStatus(TaskLive);
    	nTask.setPriority(1);
    	
    	// Seta arquivo de Swap com o tamanho em blocos certo
    	swpSize = Math.pow(2,MMU.getVirtualAddressBits());
    	swpName = nTask.getID();
    	FileSys.create(SwapDeviceMountPoint+swpName, (int)swpSize);
    	OpenFile swpFile = OpenFile.open(SwapDeviceMountPoint+swpName,nTask);
    	if(swpFile == null){ //Se swap falhar em criar, dispatch e retorn null
    		ThreadCB.dispatch();
    		return null;
    	}
    	nTask.setSwapFile(swpFile);
    	
    	// Criação da primeira Thread da Task
    	ThreadCB frstThread = ThreadCB.create(nTask);
    	   	
    	return nTask;

    }

    /**
       Kills the specified task and all of it threads. 

       Sets the status TaskTerm, frees all memory frames 
       (reserved frames may not be unreserved, but must be marked 
       free), deletes the task's swap file.
	
       @OSPProject Tasks
    */
    public void do_kill()
    {
    
    	PageTable page;
    	/*OpenFile swpFile;*/
    	ThreadCB thread;
    	PortCB port;
    	OpenFile file;
    	int swpName;
    	int countFiles;
    	Enumeration enumFiles;
    	
    	/* Mata as threads da task */
    	while(do_getThreadCount() > 0) {
    		thread = (ThreadCB)this.lsThreads.getHead();
    		thread.kill();
    	}
    	
    	/* Destroi as portas utilizadas pela task */
    	while(do_getPortCount() > 0) {
    		port = (PortCB)this.lsPorts.getHead();
    		port.destroy();
    	}
    	
    	/* Seta TaskTerm para o status da thread */
    	this.setStatus(TaskTerm);
    	
    	/* Libera a memoria alocada pela task */
    	page = this.getPageTable();
    	page.deallocateMemory();
    	
    	/* Fecha os arquivos utilizados pela task */
    	enumFiles = lsFiles.forwardIterator();
    	while(enumFiles.hasMoreElements()){
    		file = (OpenFile)enumFiles.nextElement();
    		file.close();
    	}
    	
    	/* Deleta o arquivo de swap utilizado pela task */
    	swpName = this.getID();
    	FileSys.delete(SwapDeviceMountPoint+swpName);
    	//ThreadCB.dispatch();
    
    }

    /** 
	Returns a count of the number of threads in this task. 
	
	@OSPProject Tasks
    */
    public int do_getThreadCount()
    {
    	return lsThreads.length();

    }

    /**
       Adds the specified thread to this task. 
       @return FAILURE, if the number of threads exceeds MaxThreadsPerTask;
       SUCCESS otherwise.
       
       @OSPProject Tasks
    */
    public int do_addThread(ThreadCB thread)
    {
    	
    	if(lsThreads.length() >= ThreadCB.MaxThreadsPerTask) return FAILURE;
    	lsThreads.append(thread);	
    	return SUCCESS;

    }

    /**
       Removes the specified thread from this task. 		

       @OSPProject Tasks
    */
    public int do_removeThread(ThreadCB thread)
    {
    	if(lsThreads.contains(thread)){
    		lsThreads.remove(thread);
    		return SUCCESS;
    	}
    	else return FAILURE;

    }

    /**
       Return number of ports currently owned by this task. 

       @OSPProject Tasks
    */
    public int do_getPortCount()
    {
    	return lsPorts.length();

    }

    /**
       Add the port to the list of ports owned by this task.
	
       @OSPProject Tasks 
    */ 
    public int do_addPort(PortCB newPort)
    {
    	if(lsPorts.length() >= PortCB.MaxPortsPerTask) return FAILURE;
    	lsPorts.append(newPort);	
    	return SUCCESS;

    }

    /**
       Remove the port from the list of ports owned by this task.

       @OSPProject Tasks 
    */ 
    public int do_removePort(PortCB oldPort)
    {
    	if(lsPorts.contains(oldPort)){
    		lsPorts.remove(oldPort);
    		return SUCCESS;
    	}
    	else return FAILURE;

    }
    
    /**
    Return number of files currently owned by this task. 

    @OSPProject Tasks
     */
    public int do_getFileCount()
    {
    	return lsFiles.length();
    }
    
    /**
       Insert file into the open files table of the task.

       @OSPProject Tasks
    */
    public void do_addFile(OpenFile file)
    {
    	lsFiles.append(file);

    }

    /** 
	Remove file from the task's open files table.

	@OSPProject Tasks
    */
    public int do_removeFile(OpenFile file)
    {
    	if(lsFiles.contains(file)){
    		lsFiles.remove(file);
    		return SUCCESS;
    	}
    	else return FAILURE;

    }

    /**
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures
       in their state just after the error happened.  The body can be
       left empty, if this feature is not used.
       
       @OSPProject Tasks
    */
    public static void atError()
    {

    }

    /**
       Called by OSP after printing a warning message. The student
       can insert code here to print various tables and data
       structures in their state just after the warning happened.
       The body can be left empty, if this feature is not used.
       
       @OSPProject Tasks
    */
    public static void atWarning()
    {

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
