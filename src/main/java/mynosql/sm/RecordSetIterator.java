package mynosql.sm;
public interface RecordSetIterator {
    void   reset() ;
    byte[] getNextRecord();
    boolean hasMoreRecords() ;
}
