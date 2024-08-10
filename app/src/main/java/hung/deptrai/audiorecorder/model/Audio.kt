package hung.deptrai.audiorecorder.model

import android.net.Uri

data class Audio(
    val name: String,
    val duration: Long,
    val recordingTime: Long,
    val audioPath: Uri
){
    fun doesMatchingSearchQuery(
        query: String
    ): Boolean{
        val  matchingCombination = listOf(
            "${name.first()}"
        )
        return matchingCombination.any{
            it.contains(query, ignoreCase = true)
        }
    }
}
