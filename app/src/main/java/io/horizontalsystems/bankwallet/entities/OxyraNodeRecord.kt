package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class OxyraNodeRecord(
    @PrimaryKey
    val url: String,
    val username: String?,
    val password: String?,
    val trusted: Boolean
)
