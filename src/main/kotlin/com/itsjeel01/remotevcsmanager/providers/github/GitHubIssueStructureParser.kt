package com.itsjeel01.remotevcsmanager.providers.github

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.itsjeel01.remotevcsmanager.models.IssueMilestone
import com.itsjeel01.remotevcsmanager.models.IssueRelationship

internal object GitHubIssueStructureParser {

    fun toMilestone(json: JsonObject): IssueMilestone =
        IssueMilestone(
            id = safeString(json, "id") ?: "",
            title = safeString(json, "title") ?: "",
            openIssueCount = safeInt(json, "open_issues"),
            state = safeString(json, "state") ?: ""
        )

    fun toIssueRelationships(parentIssueNumber: Int, subIssues: JsonArray): List<IssueRelationship> =
        subIssues.mapNotNull { element ->
            val child = element.asObjectOrNull() ?: return@mapNotNull null
            val childNumber = safeInt(child, "number").takeIf { it > 0 } ?: return@mapNotNull null
            IssueRelationship(
                parentIssueNumber = parentIssueNumber,
                childIssueNumber = childNumber
            )
        }

    private fun JsonElement.asObjectOrNull(): JsonObject? =
        if (this is JsonNull || !isJsonObject) null else asJsonObject

    private fun safeString(json: JsonObject, key: String): String? {
        val element = json.get(key)
        return if (element == null || element is JsonNull) null else element.asString
    }

    private fun safeInt(json: JsonObject, key: String): Int {
        val element = json.get(key)
        return if (element == null || element is JsonNull) 0 else element.asInt
    }
}
