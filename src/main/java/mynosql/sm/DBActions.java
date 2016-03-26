//import java.io.File;
//import java.io.IOException;
//
//import org.tmatesoft.sqljet.core.SqlJetException;
//import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
//import org.tmatesoft.sqljet.core.schema.ISqlJetIndexDef;
//import org.tmatesoft.sqljet.core.schema.ISqlJetTableDef;
//import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
//import org.tmatesoft.sqljet.core.table.ISqlJetTable;
//import org.tmatesoft.sqljet.core.table.ISqlJetTransaction;
//import org.tmatesoft.sqljet.core.table.SqlJetDb;
//
//public abstract class DBActions{
//	
//	private static final String DB_NAME = "keyOID.db";
//	private static final String TABLE_NAME = "keys";
//	private static final String KEY_FIELD = "key";
//	private static final String OID_FIELD = "oid";
//	
//	public SM storage = null;
//	private File fdb = null;
//	public SqlJetDb db = null;
//	
//	public DBActions() throws Exception
//	{
//		fdb = new File(DB_NAME);
//		try{
//			fdb.createNewFile();
//			if(fdb.exists())
//			{			
//				db = new SqlJetDb(fdb, true);
//				 // set DB option that have to be set before running any transactions: 
//		        db.getOptions().setAutovacuum(true);
//		        // set DB option that have to be set in a transaction: 
//		        db.runTransaction(new ISqlJetTransaction() {
//		            public Object run(SqlJetDb db) throws SqlJetException {
//		                db.getOptions().setUserVersion(1);
//		                return true;
//		            }
//		        }, SqlJetTransactionMode.WRITE);
//			}
//		}
//		catch(IOException|SqlJetException exp)
//		{			
//			throw new Exception("Cannot create or write into " + DB_NAME);
//		}
//	}
//	
//	abstract String read(String key);
//	abstract String insert(String key, String value);
//	abstract String delete(String key);
//	abstract String update(String key, String newValue);	
//};
