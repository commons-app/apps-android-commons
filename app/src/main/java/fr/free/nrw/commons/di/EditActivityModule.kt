package fr.free.nrw.commons.di

import dagger.Module
import dagger.Provides
import fr.free.nrw.commons.edit.TransformImage
import fr.free.nrw.commons.edit.TransformImageImpl

@Module
object EditActivityModule {

    @Provides
    fun provideTransformImage(): TransformImage {
        return TransformImageImpl()
    }
}