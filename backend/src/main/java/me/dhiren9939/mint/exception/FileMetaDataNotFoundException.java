package me.dhiren9939.mint.exception;

public class FileMetaDataNotFoundException extends RuntimeException {
    public FileMetaDataNotFoundException(){
        super("File Not Found.");
    }

    public FileMetaDataNotFoundException(String message){
        super(message);
    }
}
