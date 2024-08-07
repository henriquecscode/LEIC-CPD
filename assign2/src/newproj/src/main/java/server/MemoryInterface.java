package server;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public interface MemoryInterface {

    /**
     * Deletes file.
     *
     * @param fileName, file to delete.
     */
    void del(String fileName);

    /**
     * Returns an object (file).
     *
     * @param fileName, name of the file to return.
     */
    byte[] get(String fileName);

    /**
     * @param fileName
     */
    String put(String fileName);

    /**
     * Makes all the files in one of the directory be considered as if they were
     * their own.
     *
     * @param isFromLeft, boolean that shows if it's from left.
     */
    void makeOwnDir(boolean isFromLeft);
}
