package me.xploited.tornado.handlers

import com.google.gson.FieldNamingPolicy
import java.io.File
import com.google.gson.GsonBuilder
import java.io.FileReader
import java.io.FileWriter

private val gson = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .setPrettyPrinting()
    .create()

object ConfigurationHandler {
    fun <T> readConfig(fileName: String, clazz: Class<T>): T {
        val fullFileName = "$fileName.json"
        val file = File(fullFileName)
        if (file.exists())
            return FileReader(file).use { gson.fromJson(it, clazz) }

        // save a new configuration
        // let the method throw if it does not have a parameter-less constructor
        val config = clazz.getConstructor().newInstance()
        saveToFile(fileName, config)
        return config
    }

    fun <T> saveToFile(fileName: String, obj: T) = FileWriter("$fileName.json").use { it.write(gson.toJson(obj)) }
}

// every configuration data class must have a parameter-less constructor
// in this case we use a constructor with parameters that have default values
data class Configuration(
    // configuration defaults
    val prefix: String = ">",
    val discordToken: String = "Insert Your Token Here"
)