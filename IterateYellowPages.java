package parsedata.yellowPages;

import java.io.File;
import java.util.LinkedList;
import com.princeton.database.directory.session.DirectoryDatabaseFacade;
import parsedata.IDGenerator;
import parsedata.Insert;

public class IterateYellowPages {
    public static final String ICC = "AE";
    private String rootDir;
    private LinkedList<String> dirQueue, encounteredDirQueue;
    private LinkedList<String> filesToParse, next1000Files;
    private int fileCount;
    
    public IterateYellowPages(String root) {
        this.rootDir = root;
        this.dirQueue = new LinkedList<String>();
        this.dirQueue.add(root);
        this.encounteredDirQueue = new LinkedList<String>();
        this.filesToParse = new LinkedList<String>();
        this.fileCount = 0;
    }

    public static void main(String[] args) {
        Insert insertObj = new Insert();
        IterateYellowPages iter = new IterateYellowPages("C:\\My Web Sites\\" +
                "Dubai Yellow Pages\\www.yellowpages.ae\\category");
        iter.addFilesToParse();
        ParseDataYellowPages parser = new ParseDataYellowPages();
        DirectoryDatabaseFacade sessionBean = insertObj.getSessionBean();
        IDGenerator generator = new IDGenerator(sessionBean, ICC);
        while (iter.hasMoreFiles()) {
           parser.parseString(iter.getNext1000Files());
           //System.out.println(parser.getDBOVector().size());
           //generator.passDBOVector(parser.getDBOVector());
           //generator.generateIDs();
           //insertObj.insertIntoDatabase(generator.getDBOVector());
        }
        
        // Debugging
        System.out.println("Number of usable files: " + iter.getFileCount());
        System.out.println(iter.getFilesToParse().size());
        System.out.println("Organization name doubles: " + generator.getDoubles());
        System.out.println("Not legitimate Organization names: " + 
                generator.getNotLegitNames());
    }
    
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
        IterateYellowPages iter = this;
        getSubDirs(iter);
        while (this.getDirQueue().size() != 0) {
            getSubDirs(this);
        }
    }    
    
    private static void getSubDirs(IterateYellowPages iter) {
        String filePath = iter.getDirQueue().removeFirst();
        File dir = new File(filePath);
        for (File fileChild: dir.listFiles()) {
            if (fileChild.isFile()) { 
                addFile(fileChild, iter);
                iter.incrementFileCount();
            } else if (fileChild.isDirectory()) {
                addDir(fileChild, iter);
            } else {
                // Not a File or Directory; do nothing
                continue;
            }
        }
    }
    
    private static void addFile(File fileChild, IterateYellowPages iter) {
        String filePath = fileChild.getPath();
        if (filePath.matches(".*category.*(index.html)$")) {
            iter.addFile(filePath);
            System.out.println("HTML FILE: " + filePath);
        } 
    }
    
    private static void addDir(File fileChild, IterateYellowPages iter) {
        String filePath = fileChild.getPath();
        if (iter.getEncounteredDirQueue().contains(fileChild.getPath())) {
            iter.getDirQueue().remove(fileChild.getPath());
        } else {
            iter.getDirQueue().add(fileChild.getPath());
            iter.getEncounteredDirQueue().add(fileChild.getPath());
        }
    }
}