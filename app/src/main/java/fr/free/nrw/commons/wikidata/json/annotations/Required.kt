package fr.free.nrw.commons.wikidata.json.annotations


/**
 * Annotate fields in Retrofit POJO classes with this to enforce their presence in order to return
 * an instantiated object.
 *
 * E.g.: @NonNull @Required private String title;
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Required 
