package com.metacontent.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.metacontent.LetMeSpawn
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class PermitConfig {
    companion object {
        const val PATH = "config/${LetMeSpawn.ID}.json"
        private val GSON: Gson = GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .registerTypeAdapter(ExcessiveSpawnPermit::class.java, SpawnPermitAdapter)
            .create()

        fun load(): PermitConfig {
            val default = PermitConfig()

            val file = File(PATH)
            file.parentFile.mkdirs()

            val config = runCatching {
                if (!file.exists()) {
                    file.createNewFile()
                }
                FileReader(file).use {
                    GSON.fromJson(it, PermitConfig::class.java) ?: default
                }
            }.onFailure {
                LetMeSpawn.LOGGER.error(it.message, it)
            }.getOrDefault(default)

            config.save()

            return config
        }
    }

    val enableSpawnMessages = true

    val permits = listOf<ExcessiveSpawnPermit?>(
        BucketSpawnPermit("ultra-rare")
    )

    fun save() {
        val file = File(PATH)
        try {
            val fileWriter = FileWriter(file)
            GSON.toJson(this, fileWriter)
            fileWriter.flush()
            fileWriter.close()
        } catch (e: Exception) {
            LetMeSpawn.LOGGER.error(e.message, e)
        }
    }
}