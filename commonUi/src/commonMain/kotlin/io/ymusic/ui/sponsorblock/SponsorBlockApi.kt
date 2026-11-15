package io.ymusic.ui.sponsorblock

import io.ymusic.remoteconfig.ApiService

interface SponsorBlockApi {
    suspend fun getSkipSegments(videoId: String): GetSkipSegmentsResponse
}

data class GetSkipSegmentsResponse(
    val segments: List<SponsorBlockSegment>,
)

data class SponsorBlockSegment(
    val startTime: Double,
    val endTime: Double,
    val category: String,
    val videoDuration: Double,
)

class ServiceSponsorBlockApi(private val api: ApiService) : SponsorBlockApi {
    override suspend fun getSkipSegments(videoId: String): GetSkipSegmentsResponse =
        GetSkipSegmentsResponse(
            api.getSkipSegments(videoId).map {
                SponsorBlockSegment(it.startTime, it.endTime, it.category, it.videoDuration)
            },
        )
}
