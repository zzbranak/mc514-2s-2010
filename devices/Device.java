package osp.Devices;

/**
    This class stores all pertinent information about a device in
    the device table.  This class should be sub-classed by all
    device classes, such as the Disk class.

    @OSPProject Devices
*/

import osp.IFLModules.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Memory.*;
import osp.FileSys.*;
import osp.Tasks.*;
import java.util.*;

public class Device extends IflDevice
{
    /**
        This constructor initializes a device with the provided parameters.
	As a first statement it must have the following:

	    super(id,numberOfBlocks);

	@param numberOfBlocks -- number of blocks on device

        @OSPProject Devices
    */
    public Device(int id, int numberOfBlocks)
    {
    	super(id,numberOfBlocks);
    	iorbQueue = new GenericList();

    }

    /**
       This method is called once at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Devices
    */
    public static void init()
    {
        // your code goes here

    }

    /**
       Enqueues the IORB to the IORB queue for this device
       according to some kind of scheduling algorithm.
       
       This method must lock the page (which may trigger a page fault),
       check the device's state and call startIO() if the 
       device is idle, otherwise append the IORB to the IORB queue.

       @return SUCCESS or FAILURE.
       FAILURE is returned if the IORB wasn't enqueued 
       (for instance, locking the page fails or thread is killed).
       SUCCESS is returned if the IORB is fine and either the page was 
       valid and device started on the IORB immediately or the IORB
       was successfully enqueued (possibly after causing pagefault pagefault)
       
       @OSPProject Devices
    */
    public int do_enqueueIORB(IORB iorb)
    {
        PageTableEntry IORBpage = iorb.getPage();
        OpenFile IORBfile = iorb.getOpenFile();
        int DeviceId = iorb.getDeviceID();
        
        int CylinderN;
        int TrackN;
        int TracksPerCylinder = ((Disk)this).getPlatters();
        int BlocksPerTrack;
        int BlockN = iorb.getBlockNumber();
        int BlockSize = 1;
        int SectorsPerBlock;
        int SectorSize = ((Disk)this).getBytesPerSector();
        int PAb = MMU.getVirtualAddressBits() - MMU.getPageAddressBits();
        
        IORBpage.lock(iorb);
        IORBfile.incrementIORBCount();
        
        
        for(int i=0;i<PAb;i++) BlockSize = BlockSize*2; //Tamanho do bloco é o mesmo que o da página. Cálculo igual.
        MyOut.print("OSP.Devices.Device", ">>>>>>>> Tamanho do Bloco: " + BlockSize);
        SectorsPerBlock = BlockSize / SectorSize;
        BlocksPerTrack = ((Disk)this).getSectorsPerTrack() / SectorsPerBlock;
        TrackN = BlockN / BlocksPerTrack;
        CylinderN = (TrackN / TracksPerCylinder);    
        MyOut.print("OSP.Devices.Device", ">>>>>>>> Page Address Bits " + PAb);
        MyOut.print("OSP.Devices.Device", ">>>>>>>> Tamanho do Bloco: " + BlockSize);
        MyOut.print("OSP.Devices.Device", ">>>>>>>> Numero do Bloco: " + BlockN);
        MyOut.print("OSP.Devices.Device", "-------------------------------------------");
        MyOut.print("OSP.Devices.Device", ">>>>>>>> Setores por Bloco " + SectorsPerBlock);
        MyOut.print("OSP.Devices.Device", ">>>>>>>> Bytes por Setor: " + ((Disk)this).getBytesPerSector());
        MyOut.print("OSP.Devices.Device", ">>>>>>>> Tamanho do Setor: " + SectorSize);
        MyOut.print("OSP.Devices.Device", ">>>>>>>> Setores por trilha: " + ((Disk)this).getSectorsPerTrack());
        MyOut.print("OSP.Devices.Device", "-------------------------------------------");
        MyOut.print("OSP.Devices.Device", ">>>>>>>> Trilhas por Cilindro: " + TracksPerCylinder);
        MyOut.print("OSP.Devices.Device", ">>>>>>>> Numero da Trilha: " + TrackN);
        MyOut.print("OSP.Devices.Device", "-------------------------------------------");      
        MyOut.print("OSP.Devices.Device", ">>>>>>>> Numero do Cilindro: " + CylinderN);
        
        
        iorb.setCylinder(CylinderN);
        
        if(iorb.getThread().getStatus() == ThreadKill){
        	IORBfile.decrementIORBCount();
        	return FAILURE;
        
        }
        
        if(this.isBusy() == true){
        	((GenericList)iorbQueue).append(iorb);
        	return SUCCESS;
        	
        }
        
        this.startIO(iorb);
        return SUCCESS;
        

    }

    /**
       Selects an IORB (according to some scheduling strategy)
       and dequeues it from the IORB queue.

       @OSPProject Devices
    */
    public IORB do_dequeueIORB()
    {
        IORB retIorb = (IORB)((GenericList)iorbQueue).removeHead();
        
        return retIorb;

    }

    /**
        Remove all IORBs that belong to the given ThreadCB from 
	this device's IORB queue

        The method is called when the thread dies and the I/O 
        operations it requested are no longer necessary. The memory 
        page used by the IORB must be unlocked and the IORB count for 
	the IORB's file must be decremented.

	@param thread thread whose I/O is being canceled

        @OSPProject Devices
    */
    public void do_cancelPendingIO(ThreadCB thread)
    {
        Enumeration enumIorb = ((GenericList)iorbQueue).forwardIterator();
        IORB iorb;
        
        
        while(enumIorb.hasMoreElements()){
        	iorb = (IORB)enumIorb.nextElement();
        	
        	if(iorb.getThread().getID() == thread.getID()){
        		((GenericList)iorbQueue).remove(iorb);
        		iorb.getPage().unlock();
        		iorb.getOpenFile().decrementIORBCount();
        		
        		if(iorb.getOpenFile().getIORBCount() == 0 && iorb.getOpenFile().closePending == true)
        			iorb.getOpenFile().close();
        		
        	}
        	
        }
        
        

    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
	
	@OSPProject Devices
     */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
	
	@OSPProject Devices
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
