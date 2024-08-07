package server;

import java.io.*;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Memory implements MemoryInterface {
    Node node;
    ReadWriteLock lock;
    int corePoolSize = 10;
    String storageFolder;
    String storagePath;
    String configFolder;
    String configPath;
    String tombstoneFolder;
    String tombstonePath;
    String commonPath;
    String counterPath;
    String logPath;
    String nodesPath;

    public Memory(Node node) {
        this.node = node;
        this.lock = new ReentrantReadWriteLock();
        this.initializeDirs();
    }

    private void initializeDirs() {
        // Make sure that the folders exist and are unique for each node (using the node
        // id)
        // We need to have a prefixPath that is just the nodeID
        // common path is ./<node_id>
        // and every other path uses common path
        String prefix = node.getNodeId();
        // create mem directory if does not exist
        createIfNotExistDir("./mem");
        commonPath = "./mem/" + prefix + "/";
        // create prefix directory if does not exist
        createIfNotExistDir(commonPath);
        storageFolder = "data";
        storagePath = commonPath + storageFolder + "/";
        // create storage directory if does not exist
        createIfNotExistDir(storagePath);
        tombstoneFolder = "tombstone";
        tombstonePath = commonPath + tombstoneFolder + "/";
        // create tombstone directory if does not exist
        createIfNotExistDir(tombstonePath);
        configFolder = "config";
        configPath = commonPath + configFolder + "/";
        createIfNotExistDir(configPath);
        // create config directory if does not exist
        counterPath = configPath + "counter";
        logPath = configPath + "log";
        nodesPath = configPath + "nodes";
        // create counter file (it's not a directory) if does not exist (will just have
        // one file butt that's alright)
    }

    private void createIfNotExistDir(String path) {
        File f = new File(path);

        // Check if the directory can be created using the specified path name
        if (!f.exists()) {
            if (f.mkdir() == true) {
                f.mkdirs();
                System.out.println("Directory has been created successfully");
            } else {
                System.out.println("Directory cannot be created");
            }
        }
    }

    private String hash(String fileName) {
        return Hash.getHash(fileName);
    }

    public void storeCounter(Integer counter) {
        String fullPath = this.counterPath;
        // Integer to bytes
        String counterString = Integer.toString(counter);
        byte[] counterBytes = counterString.getBytes(StandardCharsets.UTF_8);
        this.putFile(fullPath, counterBytes);
    }

    public Integer getCounter() {
        String fullPath = this.counterPath;
        byte[] counterBytes = this.getFileFromMemory(fullPath);
        if (counterBytes.length == 0) {
            return 0;
        }
        String counter = new String(counterBytes, StandardCharsets.UTF_8);
        return Integer.valueOf(counter);
    }

    public void storeLog(String logString) {
        String fullPath = this.logPath;
        byte[] logBytes = logString.getBytes();
        this.putFile(fullPath, logBytes);
    }

    public void storeNodes(String nodeString) {
        String fullPath = this.nodesPath;
        byte[] nodesBytes = nodeString.getBytes();
        this.putFile(fullPath, nodesBytes);
    }

    public String getLog() {
        String fullPath = this.logPath;
        byte[] logBytes = this.getFileFromMemory(fullPath);
        String logString = new String(logBytes);
        return logString;
    }

    public String getNodes() {
        String fullPath = this.nodesPath;
        byte[] logBytes = this.getFile(fullPath);
        String nodesString = new String(logBytes);
        return nodesString;
    }

    public void putReplication(String hashName, byte[] data) {
        String fullPath = this.storagePath + hashName;
        this.remFile(this.getTombPath(fullPath));
        this.putFile(fullPath, data);
    }

    public String put(String fileName, byte[] data) {
        String hashName = this.hash(fileName);
        String fullPath = this.storagePath + hashName;
        String tombPath = this.getTombPath(fullPath);
        if (hasFile(tombPath)) {
            this.remFile(tombPath);
        }
        this.putFile(fullPath, data);
        return hashName;
    }

    public String put(String fileName) {
        Future<String> putFut = this.node.threadPool.submit(() -> {
            return this.memPut(fileName);
        });

        String hash = "";
        if (putFut.isDone()) {
            try {
                hash = putFut.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        return hash;
    }

    private String memPut(String fileName) {
        String hashName = this.hash(fileName);
        String originalFilePath = this.commonPath + fileName;
        String fullPath = this.storagePath + hashName;
        byte[] data = this.getFileFromMemory(originalFilePath);
        this.remFile(this.getTombPath(fullPath));
        this.putFile(fullPath, data);
        return hashName;
    }

    private void putFile(String fullPath, byte[] data) {
        // Put into the memory
        lock.writeLock().lock();
        File outputFile = new File(fullPath);
        if (!outputFile.exists()) {
            try {
                outputFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            outputStream.write(data);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        lock.writeLock().unlock();
    }

    private byte[] getFileFromMemory(String fullPath) {
        try {
            // https://stackoverflow.com/a/14169760
            lock.readLock().lock();
            File file = new File(fullPath);
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            return data;
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.readLock().unlock();
        }
        return new byte[0];

    }

    private void remFile(String fullPath) {
        lock.writeLock().lock();
        File file = new File(fullPath);
        try {
            file.delete();
        } catch (Exception e) {

        } finally {
            lock.writeLock().unlock();
        }
    }

    public void del(String hashName) {
        String fullPath = this.storagePath + hashName;
        String tombPath = this.getTombPath(fullPath);
        this.putFile(tombPath, new byte[0]);
    }

    public byte[] get(String hashName) {
        return this.memGet(hashName);
    }

    public byte[] getFromHashed(String hashName) {
        String fullPath = storagePath + hashName;
        return this.getFileFromMemory(fullPath);
    }

    public byte[] memGet(String hashName) {

        String fullPath = this.storagePath + hashName;
        Future<Boolean> existsFut = this.node.threadPool.submit(() -> {
            return this.hasFile(fullPath);
        });

        Future<Boolean> deletedFut = this.node.threadPool.submit(() -> {
            return this.hasTombstoneFile(fullPath);
        });

        Boolean exists = false, deleted = false;
        try {
            exists = existsFut.get();
            deleted = deletedFut.get();
        } catch (Exception e) {
            return null;
        }

        if (deleted) {
            return null;
        } else if (!exists) {
            return null;
        } else {
            return this.getFileFromMemory(fullPath);
        }

    }

    private byte[] getFile(String fullPath) {
        Boolean deletePair = hasTombstoneFile(fullPath);
        if (!deletePair) {
            return this.getFileFromMemory(fullPath);
        } else {
            return new byte[0];
        }

    }

    public void makeOwnDir(boolean isFromLeft) {

    }

    private Boolean hasFile(String fullPath) {
        lock.readLock().lock();
        File file = new File(fullPath);
        Boolean fileExists = file.exists();
        lock.readLock().unlock();
        return fileExists;
    }

    public Boolean hasTombstoneFileFromHashName(String hashName) {
        String fullPath = this.tombstonePath + hashName;
        return this.hasFile(fullPath);
    }

    private Boolean hasTombstoneFile(String fullPath) {
        String tombPath = getTombPath(fullPath);
        return this.hasFile(tombPath);
    }

    private String getTombPath(String fullPath) {
        List<String> folders = Arrays.asList(fullPath.split("/"));
        for (int i = 0; i < folders.size(); i++) {
            if (folders.get(i).equals(storageFolder)) {
                folders.set(i, this.tombstoneFolder);
                break;
            }
        }
        return String.join("/", folders);
    }

    public List<List<List<String>>> getReplicationIntervals(InetAddress add1, InetAddress add2, InetAddress add3,
            InetAddress add4) {
        File keysDir, tombstoneKeysDir;
        String[] keysList, tombstoneKeysList;
        Integer keysListLen, tombstoneKeysListLen;
        // Mem lock here
        lock.readLock().lock();
        keysDir = new File(this.storagePath);
        tombstoneKeysDir = new File(this.tombstonePath);

        keysList = keysDir.list();
        tombstoneKeysList = tombstoneKeysDir.list();
        lock.readLock().unlock();

        // Mem unlock here
        String hash1 = Hash.getHash(add1.toString());
        String hash2 = Hash.getHash(add2.toString());
        String hash3 = Hash.getHash(add3.toString());
        String hash4 = Hash.getHash(add4.toString());

        List<List<String>> keysByInterval = this.getIntervals(keysList, hash1, hash2, hash3, hash4);
        List<List<String>> tombstoneKeysByInterval = this.getIntervals(tombstoneKeysList, hash1, hash2, hash3, hash4);

        return new ArrayList<>(Arrays.asList(keysByInterval, tombstoneKeysByInterval));
    }

    public List<List<String>> getIntervals(String[] interval, String h1, String h2, String h3, String h4) {
        Arrays.sort(interval, (str1, str2) -> str1.compareTo(str2));
        Integer len = interval.length;
        Integer intervalState = 1;
        String hash;
        List<String> interval1 = new ArrayList<>(), interval2 = new ArrayList<>(), interval3 = new ArrayList<>();

        for (int i = 0; i < len; i++) {
            hash = interval[i];
            switch (intervalState) {
                case 1:
                    if (Hash.hle(hash, h1)) {
                        continue;
                    } else {
                        if (Hash.hle(hash, h2)) {
                            interval1.add(hash);
                        } else {
                            intervalState++;
                            interval2.add(hash);
                        }
                    }
                    break;
                case 2:
                    if (Hash.hle(hash, h3)) {
                        interval2.add(hash);
                    } else {
                        intervalState++;
                        interval3.add(hash);
                    }
                    break;
                case 3:
                    if (Hash.hle(hash, h4)) {
                        interval3.add(hash);
                    } else {
                        i = len;
                    }
                    break;
            }
        }
        return new ArrayList<>(Arrays.asList(interval1, interval2, interval3));
    }

    public List<List<String>> getKeysRange(String hash1, String hash2) {
        File keysDir, tombstoneKeysDir;
        String[] keysList, tombstoneKeysList;
        Integer keysListLen, tombstoneKeysListLen;
        // mem lock here
        keysDir = new File(this.storagePath);
        tombstoneKeysDir = new File(this.tombstonePath);

        keysList = keysDir.list();
        tombstoneKeysList = tombstoneKeysDir.list();
        // mem unlock here

        List<String> keysRange = getInterval(keysList, hash1, hash2);
        List<String> tombstoneKeysRange = getInterval(tombstoneKeysList, hash1, hash2);
        return new ArrayList<>(Arrays.asList(keysRange, tombstoneKeysRange));
    }

    public List<String> getInterval(String[] allKeys, String hash1, String hash2) {
        Arrays.sort(allKeys, (str1, str2) -> str1.compareTo(str2));
        List<String> interval = new ArrayList<>();
        String curHash = "";
        Boolean startedFinding = false;

        if (hash1.equals(hash2)) {
            if (new ArrayList<>(Arrays.asList(allKeys)).contains(hash1)) {
                interval.add(hash1);
                return interval;
            }
        }
        if (Hash.hle(hash1, hash2)) {
            for (int i = 0; i < allKeys.length; i++) {
                curHash = allKeys[i];
                if (Hash.hle(curHash, hash1)) {
                    continue;
                } else {
                    if (Hash.hle(curHash, hash2)) {
                        interval.add(curHash);
                    } else {
                        return interval;
                    }
                }
            }
        } else {
            for (int i = 0; i < allKeys.length; i++) {
                curHash = allKeys[i];
                if (Hash.hle(curHash, hash2)) {
                    interval.add(curHash);
                } else {
                    break;
                }
            }
            for (int i = allKeys.length - 1; i >= 0; i--) {
                curHash = allKeys[i];
                if (!Hash.hle(curHash, hash1)) {
                    interval.add(curHash);
                } else {
                    break;
                }
            }
            return interval;
        }
        return interval;
    }