package com.example.Simtas.data.ml

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Extracts and decodes compressed audio files (.m4a, .aac, .mp3, .wav) into
 * 16kHz, single-channel (mono), 32-bit float samples normalized to [-1.0f, 1.0f]
 * as required by the offline Whisper speech recognition engine.
 */
object AudioPcmExtractor {

    private const val TARGET_SAMPLE_RATE = 16000
    private const val TIMEOUT_US = 5000L

    /**
     * Decodes an audio file to 16kHz mono FloatArray.
     */
    fun extractPcmSamples(audioFile: File): FloatArray {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            throw IllegalArgumentException("Audio file does not exist or is empty: ${audioFile.absolutePath}")
        }

        // Fast-path: If the file is a standard 16-bit PCM WAV file
        if (audioFile.name.lowercase().endsWith(".wav")) {
            val wavSamples = tryReadWavPcm(audioFile)
            if (wavSamples != null && wavSamples.isNotEmpty()) {
                return wavSamples
            }
        }

        // Standard path: Use Android MediaExtractor + MediaCodec
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(audioFile.absolutePath)
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = trackFormat
                    break
                }
            }

            if (audioTrackIndex == -1 || format == null) {
                throw IllegalStateException("No audio track found in file: ${audioFile.name}")
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)!!
            val inputSampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else {
                44100
            }
            val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else {
                1
            }

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val rawPcmList = ArrayList<Short>()
            val bufferInfo = MediaCodec.BufferInfo()
            var sawInputEOS = false
            var sawOutputEOS = false

            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    val inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(
                                    inputBufferIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                sawInputEOS = true
                            } else {
                                codec.queueInputBuffer(
                                    inputBufferIndex,
                                    0,
                                    sampleSize,
                                    extractor.sampleTime,
                                    0
                                )
                                extractor.advance()
                            }
                        }
                    }
                }

                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        val shortBuffer = outputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        while (shortBuffer.hasRemaining()) {
                            rawPcmList.add(shortBuffer.get())
                        }
                    }
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true
                    }
                }
            }

            // Downmix to mono if multi-channel
            val monoShorts: ShortArray
            if (channelCount > 1) {
                val numFrames = rawPcmList.size / channelCount
                monoShorts = ShortArray(numFrames)
                for (i in 0 until numFrames) {
                    var sum = 0
                    for (ch in 0 until channelCount) {
                        sum += rawPcmList[i * channelCount + ch]
                    }
                    monoShorts[i] = (sum / channelCount).toShort()
                }
            } else {
                monoShorts = ShortArray(rawPcmList.size)
                for (i in rawPcmList.indices) {
                    monoShorts[i] = rawPcmList[i]
                }
            }

            // Resample to 16,000 Hz if needed
            val targetShorts: ShortArray
            if (inputSampleRate != TARGET_SAMPLE_RATE && inputSampleRate > 0) {
                val targetLength = ((monoShorts.size.toLong() * TARGET_SAMPLE_RATE) / inputSampleRate).toInt()
                targetShorts = ShortArray(targetLength)
                val ratio = monoShorts.size.toDouble() / targetLength.toDouble()
                for (i in 0 until targetLength) {
                    val srcIndex = (i * ratio).toInt().coerceIn(0, monoShorts.size - 1)
                    targetShorts[i] = monoShorts[srcIndex]
                }
            } else {
                targetShorts = monoShorts
            }

            // Convert to normalized FloatArray [-1.0f, 1.0f]
            val floatSamples = FloatArray(targetShorts.size)
            for (i in targetShorts.indices) {
                floatSamples[i] = targetShorts[i] / 32768.0f
            }

            return floatSamples

        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (_: Exception) {}
            try {
                extractor.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Reads a standard 16-bit PCM WAV file without requiring MediaCodec.
     */
    fun tryReadWavPcm(wavFile: File): FloatArray? {
        try {
            FileInputStream(wavFile).use { fis ->
                val header = ByteArray(44)
                if (fis.read(header) < 44) return null

                // Check "RIFF" and "WAVE"
                if (header[0] != 'R'.code.toByte() || header[1] != 'I'.code.toByte() ||
                    header[2] != 'F'.code.toByte() || header[3] != 'F'.code.toByte() ||
                    header[8] != 'W'.code.toByte() || header[9] != 'A'.code.toByte() ||
                    header[10] != 'V'.code.toByte() || header[11] != 'E'.code.toByte()
                ) {
                    return null
                }

                val channels = (header[22].toInt() and 0xFF) or ((header[23].toInt() and 0xFF) shl 8)
                val sampleRate = (header[24].toInt() and 0xFF) or
                        ((header[25].toInt() and 0xFF) shl 8) or
                        ((header[26].toInt() and 0xFF) shl 16) or
                        ((header[27].toInt() and 0xFF) shl 24)
                val bitsPerSample = (header[34].toInt() and 0xFF) or ((header[35].toInt() and 0xFF) shl 8)

                if (bitsPerSample != 16) return null

                val pcmBytes = fis.readBytes()
                val numShorts = pcmBytes.size / 2
                val shorts = ShortArray(numShorts)
                ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

                val monoShorts: ShortArray
                if (channels > 1) {
                    val numFrames = shorts.size / channels
                    monoShorts = ShortArray(numFrames)
                    for (i in 0 until numFrames) {
                        var sum = 0
                        for (ch in 0 until channels) {
                            sum += shorts[i * channels + ch]
                        }
                        monoShorts[i] = (sum / channels).toShort()
                    }
                } else {
                    monoShorts = shorts
                }

                val targetShorts: ShortArray
                if (sampleRate != TARGET_SAMPLE_RATE && sampleRate > 0) {
                    val targetLen = ((monoShorts.size.toLong() * TARGET_SAMPLE_RATE) / sampleRate).toInt()
                    targetShorts = ShortArray(targetLen)
                    val ratio = monoShorts.size.toDouble() / targetLen.toDouble()
                    for (i in 0 until targetLen) {
                        val srcIdx = (i * ratio).toInt().coerceIn(0, monoShorts.size - 1)
                        targetShorts[i] = monoShorts[srcIdx]
                    }
                } else {
                    targetShorts = monoShorts
                }

                val floats = FloatArray(targetShorts.size)
                for (i in targetShorts.indices) {
                    floats[i] = targetShorts[i] / 32768.0f
                }
                return floats
            }
        } catch (_: Exception) {
            return null
        }
    }
}
