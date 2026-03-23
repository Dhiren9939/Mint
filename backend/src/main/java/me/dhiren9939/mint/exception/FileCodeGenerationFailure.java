package me.dhiren9939.mint.exception;

public class FileCodeGenerationFailure extends RuntimeException {
    public FileCodeGenerationFailure(){
        super("Could not create a unique code for upload.");
    }

    public FileCodeGenerationFailure(String message){
        super(message);
    }
}
