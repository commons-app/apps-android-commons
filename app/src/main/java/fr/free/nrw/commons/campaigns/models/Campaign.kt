package fr.free.nrw.commons.campaigns.models

/**
 * A data class to hold a campaign
 */
data class Campaign(var title: String? = null,
                    var description: String? = null,
                    var startDate: String? = null,
                    var endDate: String? = null,
                    var link: String? = null,
                    var isWLMCampaign: Boolean = false)