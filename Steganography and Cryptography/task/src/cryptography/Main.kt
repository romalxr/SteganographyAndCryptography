package cryptography

import java.io.File
import javax.imageio.ImageIO
import kotlin.experimental.xor
import kotlin.text.Charsets.UTF_8

fun main() {
    while (true) {
        println("Task (hide, show, exit): ")
        when (val task = readln()) {
            "exit" -> {
                println("Bye!")
                break
            }
            "hide" -> {
                println("Input image file: ")
                val inputFile = File(readln())
                println("Output image file: ")
                val outputFile = File(readln())
                println("Message to hide: ")
                val message = readln()
                println("Password: ")
                val password = readln()
                try {
                    hide(inputFile, outputFile, message, password)
                    println("Message saved in $outputFile image.")
                } catch (e: Exception) {
                    println(e.message)
                }
            }
            "show" -> {
                println("Input image file: ")
                val inputFile = File(readln())
                println("Password: ")
                val password = readln()
                val message = show(inputFile, password)
                println("Message:")
                println(message)
            }
            else -> {
                println("Wrong task: [$task]")
            }
        }
    }
}

fun hide(inputFile: File, outputFile: File, message: String, password: String) {
    val encoded = encryptMessage(message, password) + "\u0000\u0000\u0003"
    val bytes = encoded.encodeToByteArray()
    val bits = bytes
        .map { byte -> (0..7).map { byte.toInt() shl it and 0xFF shr 7 } }
        .flatten()
        .toIntArray()
    val image = ImageIO.read(inputFile)
    if (image.width * image.height < bits.size) {
        throw IllegalArgumentException("The input image is not large enough to hold this message.")
    }

    bits.withIndex().forEach {
        val x = it.index % image.width
        val y = it.index / image.width
        val modified = image.getRGB(x, y).toUInt() and 0xFFFFFFFEu or it.value.toUInt()
        image.setRGB(x, y, modified.toInt())
    }

    ImageIO.write(image, "png", outputFile)
}

fun show(inputFile: File, password: String): String {
    val image = ImageIO.read(inputFile)
    val bytes = mutableListOf<Int>()
    for (byte in generateSequence(0) { it + 1 }
        .map {
            val x = it % image.width
            val y = it / image.width
            image.getRGB(x, y) and 1
        }
        .chunked(8)
        .map { it.reduce { byte, i -> byte shl 1 or i } }) {
        bytes.add(byte)
        val lastIndex = bytes.lastIndex
        if (bytes.size >= 3 && bytes[lastIndex] == 3 && bytes[lastIndex - 1] == 0 && bytes[lastIndex - 2] == 0) {
            break
        }
    }

    val message = bytes.dropLast(3).map { it.toByte() }.toByteArray().toString(UTF_8)
    return encryptMessage(message, password)
}

fun encryptMessage(message: String, password: String): String {
    val encoder = generateSequence { password.encodeToByteArray().asSequence() }.flatten()
    return message.encodeToByteArray().asSequence().zip(encoder)
        .map { it.first xor it.second }
        .toList().toByteArray().toString(UTF_8)
}
