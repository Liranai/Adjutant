package me.liranai.adjutant.util

import discord4j.core.`object`.entity.TextChannel
import reactor.core.Disposable
import reactor.core.publisher.Mono
import java.time.Duration


/**
 * Creates a temporary message and sends it in a non blocking way
 */
fun TextChannel.sendTempMessage(message: String, delay: Duration = Duration.ofSeconds(2)): Disposable =
    this.createTempMessage(message, delay).subscribe()

/**
 * Creates a temporary message
 */
fun TextChannel.createTempMessage(messageContent: String, delay: Duration = Duration.ofSeconds(2)): Mono<Void> =
    createMessage(messageContent)
        .flatMap { it.delete().delaySubscription(delay) }

/**
 * Creates a message and sends it
 */
fun TextChannel.sendMessage(message: String): Disposable = this.createMessage(message).subscribe()