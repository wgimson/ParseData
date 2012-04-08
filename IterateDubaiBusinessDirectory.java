package parsedata.dubaiBusinessDirectory;

import java.io.File;
import java.util.LinkedList;
import com.princeton.database.directory.session.DirectoryDatabaseFacade;
import parsedata.AbstractIterate;
import parsedata.IDGenerator;
import parsedata.Insert;

public class IterateDubaiBusinessDirectory extends AbstractIterate {

    public static final String ICC = "AE";
    
    private String rootDir;
    private LinkedList<String> dirQueue, encounteredDirQueue;
    private LinkedList<String> filesToParse, next1000Files;
    private int fileCount;
    
    public IterateDubaiBusinessDirectory(String root) {
        this.rootDir = root;
        this.dirQueue = new LinkedList<String>();
        this.dirQueue.add(root);
        this.encounteredDirQueue = new LinkedList<String>();
        this.filesToParse = new LinkedList<String>();
        this.fileCount = 0;
    }

    public static void main(String[] args) {
        
        Insert insertObj = new Insert();
        DirectoryDatabaseFacade sessionBean = insertObj.getSessionBean();
        IterateDubaiBusinessDirectory iter = new IterateDubaiBusinessDirectory("C:\\My Web Sites\\test\\" + ""
                + "dubai-businessdirectory.com");
        iter.addFilesToParse();
        ParseDataDubaiBusinesseDirectory parser = 
                new ParseDataDubaiBusinesseDirectory();
        IDGenerator generator = new IDGenerator(sessionBean, ICC);
        while (iter.hasMoreFiles()) {
            parser.parseString(iter.getNext1000Files());
            generator.passDBOVector(parser.getDBOVector());
            generator.generateIDs();
            insertObj.insertIntoDatabase(generator.getDBOVector());
        }
    }

    @Override
    public boolean hasMoreFiles() {
        if (!this.filesToParse.isEmpty()) {
            return true;
        }  else {
            return false;
        }
    }
    
    @Override
    public LinkedList<String> getNext1000Files() {
        this.next1000Files = new LinkedList<String>();
        while ((!this.filesToParse.isEmpty()) && 
                (next1000Files.size() < 1000)) {
            this.next1000Files.add(this.filesToParse.remove());
        }
        return this.next1000Files;
    }
    
    @Override
    public int getFileCount() {
        return this.fileCount;
    }
    
    @Override
    public LinkedList<String> getDirQueue() {
        return this.dirQueue;
    }
    
    @Override
    public LinkedList<String> getEncounteredDirQueue() {
        return this.encounteredDirQueue;
    }

    @Override
    public LinkedList<String> getFilesToParse() {
        return this.filesToParse;
    }
    
    @Override
    public void incrementFileCount() {
        this.fileCount++;
    }
    
    @Override
    public void addFile(String filePath) {
        this.filesToParse.add(filePath);
    }

    @Override
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

    @Override
    protected boolean isLegitFile(String filePath) {
        if (filePath.matches(".*findadealer.*\\.html")) { 
            this.incrementFileCount();
            return true;
        } else { 
            return false;
        }
    }
}
