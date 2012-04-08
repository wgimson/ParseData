package parsedata;

import java.io.File;
import java.util.LinkedList;

public abstract class AbstractIterate {
    
    private String rootDir;
    private LinkedList<String> dirQueue, encounteredDirQueue;
    private LinkedList<String> filesToParse, next1000Files;
    private int fileCount;

    public boolean hasMoreFiles() {
        if (!this.filesToParse.isEmpty()) {
            return true;
        }  else {
            return false;
        }
    }
    
    public LinkedList<String> getNext1000Files() {
        this.next1000Files = new LinkedList<String>();
        while ((!this.filesToParse.isEmpty()) && 
                (next1000Files.size() < 1000)) {
            this.next1000Files.add(this.filesToParse.remove());
        }
        return this.next1000Files;
    }
    
    public int getFileCount() {
        return this.fileCount;
    }
    
    public LinkedList<String> getDirQueue() {
        return this.dirQueue;
    }
    
    public LinkedList<String> getEncounteredDirQueue() {
        return this.encounteredDirQueue;
    }

    public LinkedList<String> getFilesToParse() {
        return this.filesToParse;
    }
    
    public void incrementFileCount() {
        this.fileCount++;
    }
    
    public void addFile(String filePath) {
        this.filesToParse.add(filePath);
    }

    public void addFilesToParse() {
        this.getSubDirs();
            while (!this.getDirQueue().isEmpty()) {
                this.getSubDirs();
            }
    }    
    
    private void getSubDirs() {
        String filePath = this.getDirQueue().removeFirst();
        
        // We only actually create a File object here - the rest is just file
        // path String manipulation
        File dir = new File(filePath);
        for (File fileChild: dir.listFiles()) {
            if (fileChild.isFile()) { 
                this.addFile(fileChild);
                this.incrementFileCount();
            } else if (fileChild.isDirectory()) {
                this.addDir(fileChild);
            } else {
                // Not a File or Directory; do nothing
                continue;
            }
        }
    }
    
    private void addFile(File fileChild) {
        String filePath = fileChild.getPath();
        if (this.isLegitFile(filePath)) {
            this.addFile(filePath);
            System.out.println("HTML FILE: " + filePath);
        } 
    }
    
    private void addDir(File fileChild) {
        String filePath = fileChild.getPath();
        if (this.getEncounteredDirQueue().contains(fileChild.getPath())) {
            this.getDirQueue().remove(fileChild.getPath());
        } else {
            this.getDirQueue().add(fileChild.getPath());
            this.getEncounteredDirQueue().add(fileChild.getPath());
        }
    }
    
    // Must be implemented in all non-abstract child classes
    protected abstract boolean isLegitFile(String filePath);
}


    

