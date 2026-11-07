package fr.free.nrw.commons.mwapi

import java.io.IOException

/**
 * The Commons API fallback stopped because the account has more uploads than it will count.
 */
class TooManyAchievementsException : IOException("Too many uploads to compute achievements locally")
