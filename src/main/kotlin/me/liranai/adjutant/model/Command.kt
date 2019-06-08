package me.liranai.adjutant.model

import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono

interface Command {
    fun execute(event: MessageCreateEvent): Mono<Unit>
}
