package me.liranai.adjutant

import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono

interface Command {
    fun execute(event: MessageCreateEvent): Mono<Unit>
}
