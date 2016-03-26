package mynosql.sm;
import java.util.*;

//import SM.CannotDeleteException;
//import SM.IOException;
//import SM.NotFoundException;

import java.io.*;

/**
 *  Description of the Class
 *
 *@author     Paul Nguyen
 *@created    March 18, 2003
 */
public class MySM implements SM {

	private SM sm;


  public SM.OID getOID( byte[] oidbytes ) {
	return null ;
  }


	/**
	 *  Constructor for the MySM object
	 *
	 *@since
	 */
	public MySM() {
		this.sm = SMFactory.getInstance();
	}



	/**
	 *  Description of the Method
	 *
	 *@param  rec              Description of Parameter
	 *@return                  Description of the Returned Value
	 *@exception  IOException  Description of Exception
	 *@since
	 */
	public void store(String key, String value) throws IOException{
		sm.store(key, value);
	}
	public SM.OID store(Record rec) throws IOException {
		return sm.store(rec);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  oid                    Description of Parameter
	 *@return                        Description of the Returned Value
	 *@exception  NotFoundException  Description of Exception
	 *@exception  IOException        Description of Exception
	 *@since
	 */
	public String fetch(String key) throws NotFoundException, IOException{
		return sm.fetch(key);
	}
	public Record fetch(SM.OID oid) throws NotFoundException, IOException {
		return sm.fetch(oid);
	}


	/**
	 *  Description of the Method
	 *
	 *@exception  SM.IOException  Description of Exception
	 *@since
	 */
	public void close() throws SM.IOException {
		sm.close();
	}


	/**
	 *  Description of the Method
	 *
	 *@exception  SM.IOException  Description of Exception
	 *@since
	 */
	public void flush()  {
		try { sm.flush(); } catch (Exception e) {}
	}



	/**
	 *  Description of the Method
	 *
	 *@param  oid                    Description of Parameter
	 *@param  rec                    Description of Parameter
	 *@return                        Description of the Returned Value
	 *@exception  NotFoundException  Description of Exception
	 *@exception  IOException        Description of Exception
	 *@since
	 */
	public void update(String key, String value) throws NotFoundException, IOException{
		sm.update(key, value);
	}
	public SM.OID update(SM.OID oid, Record rec) throws NotFoundException, IOException {
		return sm.update(oid, rec);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  oid                        Description of Parameter
	 *@exception  NotFoundException      Description of Exception
	 *@exception  CannotDeleteException  Description of Exception
	 *@since
	 */
	public void delete(String key) throws NotFoundException, CannotDeleteException{
		sm.delete(key);
	}
	public void delete(SM.OID oid) throws NotFoundException, CannotDeleteException {
		sm.delete(oid);
	}


}

