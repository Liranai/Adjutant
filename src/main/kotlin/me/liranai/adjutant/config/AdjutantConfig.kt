package me.liranai.adjutant.config

import kotlinx.serialization.*

@Serializable
data class AdjutantConfig(
    val security: SecurityConfig,
    val prefix: String
)

@Serializable
data class SecurityConfig(
    val tokens: TokensConfig,
    val authorizedUsers: List<String>
)

@Serializable
data class TokensConfig(
    val discord: String,
    val google: String
)