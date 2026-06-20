package com.demo.server.epmigration.project.dto

data class CreateProjectResponse(
    val transactionHash: String,
    val externalProjectId: String,
    val from: String,
    val to: String,
    val nonce: String
)
