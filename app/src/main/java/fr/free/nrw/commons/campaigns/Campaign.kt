package fr.free.nrw.commons.campaigns

/**
 * A data class to hold a campaign
 */
data class Campaign(var title: String? = null,
                    var description: String? = null,
                    var startDate: String? = null,
                    var endDate: String? = null,
                    var link: String? = null)