package fr.free.nrw.commons

/**
 * Production variant related constants which is used in beta variant for some specific GET calls on
 * production server where beta server does not work
 */
object BetaConstants {
    /**
     * Commons production URL which is used in beta for some specific GET calls on
     * production server where beta server does not work
     */
    const val COMMONS_URL = "https://commons.wikimedia.org/"
    /**
     * Commons production's depicts property which is used in beta for some specific GET calls on
     * production server where beta server does not work
     */
    const val DEPICTS_PROPERTY = "P180"
}