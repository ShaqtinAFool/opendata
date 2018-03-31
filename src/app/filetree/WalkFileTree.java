package app.filetree;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

/**
 * 走訪目錄專用
 * @author tony
 */
public class WalkFileTree {
    
    private final ArrayList<String> al_FindFile = new ArrayList<>();
    
    /**
     * 走訪目錄，只選出檔案
     * @return FileVisitor<Path>
     */
    public FileVisitor<Path> findFile(){
        // 只選出檔案
        FileVisitor<Path> fv = new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                al_FindFile.add(file.toString());
                return FileVisitResult.CONTINUE;
            }            
        };
        return fv;
    }
    
    /**
     * 回傳走訪結果 FileVisitor
     * @return ArrayList<String>
     */
    public ArrayList<String> returnFV() {
        return al_FindFile;
    }          
}
