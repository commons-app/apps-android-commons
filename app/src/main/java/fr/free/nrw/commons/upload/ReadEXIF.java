package fr.free.nrw.commons.upload;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;

@Singleton
public class ReadEXIF {
    @Inject
    public ReadEXIF(){

    }
    public Single<Integer> processMetadata(String path) throws IOException{

    }
}

